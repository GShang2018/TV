package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.bean.SiteItem;
import com.fongmi.android.tv.databinding.ActivitySiteListBinding;
import com.fongmi.android.tv.databinding.DialogLinkBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.SiteListAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 站点列表页面：展示点播/直播订阅中的所有站点。
 * 支持分享（导出JSON）、导入（从剪切板/URL/文件导入为自定义站点）、检测可用性。
 */
public class SiteListActivity extends BaseActivity implements SiteListAdapter.OnClickListener {

    private static final int REQUEST_SAVE_FILE = 10001;
    private static final int REQUEST_PICK_FILE = 10002;

    private ActivitySiteListBinding mBinding;
    private SiteListAdapter mAdapter;
    private String mUrl;
    private int mType; // 0=VOD, 1=Live
    private String mTitle;
    private JsonArray mJsonArray; // 原始站点数组，用于分享
    private String mArrayKey; // "sites" 或 "lives"
    private String mSpider; // 配置中的 spider 字段，用于添加站点时解析 jar

    public static void start(Activity activity, String url, int type, String title) {
        Intent intent = new Intent(activity, SiteListActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("type", type);
        intent.putExtra("title", title);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySiteListBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mUrl = getIntent().getStringExtra("url");
        mType = getIntent().getIntExtra("type", 0);
        mTitle = getIntent().getStringExtra("title");
        mArrayKey = mType == 0 ? "sites" : "lives";

        mBinding.title.setText(TextUtils.isEmpty(mTitle) ? ResUtil.getString(R.string.site_list_title) : mTitle);
        mBinding.empty.setText(R.string.site_list_empty);

        mAdapter = new SiteListAdapter(this);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        mBinding.recycler.setAdapter(mAdapter);
        loadSites();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPress());
        mBinding.share.setOnClickListener(v -> onShare());
        mBinding.importBtn.setOnClickListener(v -> onImport());
        mBinding.check.setOnClickListener(v -> startCheck(true));
    }

    @Override
    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        finish();
    }

    // ==================== 加载站点 ====================

    private void loadSites() {
        mBinding.loading.setVisibility(View.VISIBLE);
        mBinding.empty.setVisibility(View.GONE);
        App.execute(() -> {
            try {
                String json = Decoder.getJson(mUrl);
                JsonElement root = Json.parse(json);
                if (!root.isJsonObject()) {
                    showLoadError();
                    return;
                }
                JsonObject obj = root.getAsJsonObject();
                if (!obj.has(mArrayKey)) {
                    showLoadError();
                    return;
                }
                mSpider = Json.safeString(obj, "spider");
                mJsonArray = obj.getAsJsonArray(mArrayKey);
                Set<String> disabled = getDisabledSet();
                List<SiteItem> items = new ArrayList<>();
                for (JsonElement element : mJsonArray) {
                    if (!element.isJsonObject()) continue;
                    JsonObject siteObj = element.getAsJsonObject();
                    String name = siteObj.has("name") ? siteObj.get("name").getAsString() : "";
                    String url = "";
                    String key = "";
                    if (mType == 0) {
                        // VOD: 从 api 字段获取URL，key 字段获取标识
                        url = siteObj.has("api") ? siteObj.get("api").getAsString() : "";
                        key = siteObj.has("key") ? siteObj.get("key").getAsString() : "";
                    } else {
                        // Live: 优先 url，其次 api；key 为 name
                        url = siteObj.has("url") ? siteObj.get("url").getAsString() : "";
                        if (TextUtils.isEmpty(url) && siteObj.has("api")) {
                            url = siteObj.get("api").getAsString();
                        }
                        key = siteObj.has("name") ? siteObj.get("name").getAsString() : "";
                    }
                    items.add(new SiteItem(name, url, key, element));
                }
                App.post(() -> {
                    mBinding.loading.setVisibility(View.GONE);
                    mBinding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    mAdapter.addAll(items);
                    // 根据持久化的禁用列表设置开关状态
                    for (SiteItem site : items) {
                        mAdapter.setSelected(site.getKey(), !disabled.contains(site.getKey()));
                    }
                    // 自动检测可用性
                    if (!items.isEmpty()) startCheck(false);
                });
            } catch (Exception e) {
                App.post(this::showLoadError);
            }
        });
    }

    private void showLoadError() {
        mBinding.loading.setVisibility(View.GONE);
        mBinding.empty.setText(R.string.site_list_load_fail);
        mBinding.empty.setVisibility(View.VISIBLE);
    }

    // ==================== 分享 ====================

    private void onShare() {
        if (mJsonArray == null || mJsonArray.size() == 0) {
            Notify.show(R.string.custom_site_empty);
            return;
        }
        new MaterialAlertDialogBuilder(this)
            .setItems(new CharSequence[]{
                ResUtil.getString(R.string.custom_site_share_file),
                ResUtil.getString(R.string.custom_site_share_clipboard)
            }, (dialog, which) -> {
                if (which == 0) saveShareFile();
                else shareToClipboard();
            }).show();
    }

    private String buildShareJson() {
        JsonObject object = new JsonObject();
        object.add(mArrayKey, mJsonArray);
        return object.toString();
    }

    private void saveShareFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "sites.json");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SAVE_FILE);
        } else {
            writeShareToLocal();
        }
    }

    private void writeShareToLocal() {
        App.execute(() -> {
            try {
                java.io.File dir = new java.io.File(Path.root(), "Download");
                if (!dir.exists()) dir.mkdirs();
                java.io.File file = new java.io.File(dir, "sites.json");
                Path.write(file, buildShareJson().getBytes());
                App.post(() -> Notify.show(getString(R.string.custom_site_share_file) + ": " + file.getAbsolutePath()));
            } catch (Exception e) {
                App.post(() -> Notify.show(R.string.custom_site_share_fail));
            }
        });
    }

    private void shareToClipboard() {
        try {
            String json = buildShareJson();
            ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("", json));
            }
            Notify.show(R.string.custom_site_share_success);
        } catch (Exception e) {
            Notify.show(R.string.custom_site_share_fail);
        }
    }

    // ==================== 导入（导入为自定义站点）====================

    private void onImport() {
        new MaterialAlertDialogBuilder(this)
            .setItems(new CharSequence[]{
                ResUtil.getString(R.string.custom_site_import_clipboard),
                ResUtil.getString(R.string.custom_site_import_url),
                ResUtil.getString(R.string.custom_site_import_file)
            }, (dialog, which) -> {
                if (which == 0) importFromClipboard();
                else if (which == 1) importFromUrl();
                else importFromFile();
            }).show();
    }

    private void importFromClipboard() {
        CharSequence clipText = Util.getClipText();
        if (TextUtils.isEmpty(clipText)) {
            Notify.show(R.string.custom_site_import_empty);
            return;
        }
        doImport(clipText.toString());
    }

    private void importFromUrl() {
        DialogLinkBinding binding = DialogLinkBinding.inflate(LayoutInflater.from(this));
        binding.input.setHint(R.string.custom_site_import_url_hint);
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.custom_site_import_url)
            .setView(binding.getRoot())
            .setNegativeButton(R.string.dialog_negative, null)
            .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                String url = binding.text.getText().toString().trim();
                if (TextUtils.isEmpty(url)) return;
                App.execute(() -> {
                    String content = OkHttp.string(url);
                    App.post(() -> {
                        if (TextUtils.isEmpty(content)) Notify.show(R.string.custom_site_import_fail);
                        else doImport(content);
                    });
                });
            }).show();
    }

    private void importFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, ""), REQUEST_PICK_FILE);
        }
    }

    /**
     * 将导入的 JSON 解析为站点，合并到自定义站点列表。
     * 支持 {"sites":[...]} 和 {"lives":[...]} 和 纯 [...] 格式。
     */
    private void doImport(String json) {
        App.execute(() -> {
            try {
                JsonElement root = Json.parse(json);
                JsonArray array;
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("sites")) array = obj.getAsJsonArray("sites");
                    else if (obj.has("lives")) array = obj.getAsJsonArray("lives");
                    else if (obj.has("urls")) array = obj.getAsJsonArray("urls");
                    else {
                        App.post(() -> Notify.show(R.string.custom_site_import_fail));
                        return;
                    }
                } else if (root.isJsonArray()) {
                    array = root.getAsJsonArray();
                } else {
                    App.post(() -> Notify.show(R.string.custom_site_import_fail));
                    return;
                }

                List<CustomSite> existing = new ArrayList<>(CustomSite.getAll());
                int added = 0;
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();
                    String name = obj.has("name") ? obj.get("name").getAsString() : "";
                    String url = "";
                    if (obj.has("api")) url = obj.get("api").getAsString();
                    else if (obj.has("url")) url = obj.get("url").getAsString();
                    if (TextUtils.isEmpty(url)) continue;
                    CustomSite item = new CustomSite();
                    item.setName(name);
                    item.setApi(url);
                    item.setKey(url);
                    existing.remove(item);
                    existing.add(item);
                    added++;
                }
                if (added == 0) {
                    App.post(() -> Notify.show(R.string.custom_site_import_fail));
                    return;
                }
                CustomSite.saveAll(existing);
                App.post(() -> Notify.show(R.string.custom_site_import_success));
            } catch (Exception e) {
                App.post(() -> Notify.show(R.string.custom_site_import_fail));
            }
        });
    }

    // ==================== 检测可用性 ====================

    private void startCheck(boolean manual) {
        List<SiteItem> items = mAdapter.getItems();
        if (items.isEmpty()) return;
        if (manual) {
            Notify.progress(this);
            Notify.show(R.string.custom_site_checking);
        }
        mAdapter.clearStatus();

        int total = items.size();
        int poolSize = Math.min(total, 32);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        AtomicInteger available = new AtomicInteger(0);
        AtomicInteger unavailable = new AtomicInteger(0);
        AtomicInteger done = new AtomicInteger(0);

        for (SiteItem item : items) {
            executor.execute(() -> {
                boolean ok = checkSite(item.getUrl());
                if (ok) {
                    available.incrementAndGet();
                    App.post(() -> mAdapter.setStatus(item.getKey(), SiteListAdapter.STATUS_AVAILABLE));
                } else {
                    unavailable.incrementAndGet();
                    App.post(() -> mAdapter.setStatus(item.getKey(), SiteListAdapter.STATUS_UNAVAILABLE));
                }
                if (done.incrementAndGet() == total) {
                    App.post(() -> {
                        if (manual) Notify.dismiss();
                        Notify.show(getString(R.string.custom_site_check_done, available.get(), unavailable.get()));
                    });
                    executor.shutdown();
                }
            });
        }
    }

    private boolean checkSite(String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            okhttp3.OkHttpClient client = OkHttp.client(5000);
            okhttp3.Call call = OkHttp.newCall(client, url);
            okhttp3.Response response = call.execute();
            try {
                return response.isSuccessful();
            } finally {
                response.close();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 文件回调 ====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQUEST_SAVE_FILE && data != null && data.getData() != null) {
            writeShareToUri(data.getData());
        } else if (requestCode == REQUEST_PICK_FILE && data != null && data.getData() != null) {
            readImportFromUri(data.getData());
        }
    }

    private void writeShareToUri(Uri uri) {
        App.execute(() -> {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                if (pfd == null) {
                    App.post(() -> Notify.show(R.string.custom_site_share_fail));
                    return;
                }
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                fos.write(buildShareJson().getBytes());
                fos.flush();
                fos.close();
                pfd.close();
                App.post(() -> Notify.show(R.string.custom_site_share_success));
            } catch (Exception e) {
                App.post(() -> Notify.show(R.string.custom_site_share_fail));
            }
        });
    }

    private void readImportFromUri(Uri uri) {
        App.execute(() -> {
            try {
                ContentResolver cr = getContentResolver();
                InputStream is = cr.openInputStream(uri);
                if (is == null) {
                    App.post(() -> Notify.show(R.string.custom_site_import_fail));
                    return;
                }
                String content = Path.read(is);
                App.post(() -> doImport(content));
            } catch (Exception e) {
                App.post(() -> Notify.show(R.string.custom_site_import_fail));
            }
        });
    }

    @Override
    public void onItemClick(SiteItem item) {
        // 点击站点项：复制URL
        Util.copy(item.getUrl());
    }

    @Override
    public void onToggle(SiteItem item, boolean enabled) {
        mAdapter.setSelected(item.getKey(), enabled);
        App.execute(() -> {
            // 更新持久化的禁用列表
            Set<String> disabled = getDisabledSet();
            if (enabled) disabled.remove(item.getKey());
            else disabled.add(item.getKey());
            saveDisabledSet(disabled);
            // 同步首页配置
            syncConfig(item.getKey(), item.getJson(), enabled);
        });
    }

    // ==================== 站点开关状态持久化 ====================

    private String getDisabledKey() {
        return "site_disabled_" + mUrl;
    }

    private Set<String> getDisabledSet() {
        String json = Prefers.getString(getDisabledKey());
        Set<String> set = new HashSet<>();
        if (TextUtils.isEmpty(json)) return set;
        try {
            JsonArray array = Json.parse(json).getAsJsonArray();
            for (JsonElement element : array) {
                set.add(element.getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return set;
    }

    private void saveDisabledSet(Set<String> disabled) {
        if (disabled.isEmpty()) {
            Prefers.remove(getDisabledKey());
        } else {
            JsonArray array = new JsonArray();
            for (String key : disabled) array.add(key);
            Prefers.put(getDisabledKey(), array.toString());
        }
    }

    private void syncConfig(String key, JsonElement json, boolean enabled) {
        if (mType == 0) {
            Config config = VodConfig.get().getConfig();
            if (config == null || !TextUtils.equals(config.getLoadUrl(), mUrl)) return;
            if (!enabled) {
                // 关闭站点：直接从内存列表移除
                VodConfig.get().removeSiteByKey(key);
            } else {
                // 打开站点：从 JSON 重新创建并添加
                VodConfig.get().addSiteFromJson(json, mSpider);
            }
            App.post(() -> { RefreshEvent.video(); RefreshEvent.config(); });
        } else {
            Config config = LiveConfig.get().getConfig();
            if (config == null || !TextUtils.equals(config.getLoadUrl(), mUrl)) return;
            if (!enabled) {
                LiveConfig.get().removeLiveByName(key);
            } else {
                LiveConfig.get().addLiveFromJson(json, mSpider);
            }
            App.post(RefreshEvent::live);
        }
    }
}
