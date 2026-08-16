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
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.SiteItem;
import com.fongmi.android.tv.databinding.ActivitySiteListBinding;
import com.fongmi.android.tv.databinding.DialogLinkBinding;
import com.fongmi.android.tv.ui.adapter.SiteListAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 仓库线路列表页面：展示仓库订阅中的所有线路。
 * 点击线路打开站点列表页面，支持分享/导入/检测。
 */
public class LineListActivity extends BaseActivity implements SiteListAdapter.OnClickListener {

    private static final int REQUEST_SAVE_FILE = 10001;
    private static final int REQUEST_PICK_FILE = 10002;

    private ActivitySiteListBinding mBinding;
    private SiteListAdapter mAdapter;
    private Config mConfig;
    private String mConfigUrl;
    private int mType;

    public static void start(Activity activity, String url, int type, String title) {
        Intent intent = new Intent(activity, LineListActivity.class);
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
        mConfigUrl = getIntent().getStringExtra("url");
        mType = getIntent().getIntExtra("type", 0);
        String title = getIntent().getStringExtra("title");
        mConfig = Config.find(mConfigUrl, mType);

        mBinding.title.setText(TextUtils.isEmpty(title) ? ResUtil.getString(R.string.line_list_title) : title);
        mBinding.empty.setText(R.string.line_list_empty);

        mAdapter = new SiteListAdapter(this);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        mBinding.recycler.setAdapter(mAdapter);
        loadLines();
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

    // ==================== 加载线路 ====================

    private void loadLines() {
        if (mConfig == null) {
            mBinding.empty.setVisibility(View.VISIBLE);
            return;
        }
        List<Depot> lines = mConfig.getLineList();
        List<SiteItem> items = new ArrayList<>();
        for (Depot depot : lines) {
            JsonObject obj = new JsonObject();
            obj.addProperty("url", depot.getUrl());
            obj.addProperty("name", depot.getName());
            items.add(new SiteItem(depot.getName(), depot.getUrl(), depot.getUrl(), obj));
        }
        mBinding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter.addAll(items);
        // 标记当前选中的线路，没有则默认选第一条
        String activeLine = mConfig.getLine();
        if (TextUtils.isEmpty(activeLine) && !items.isEmpty()) {
            activeLine = items.get(0).getUrl();
            String finalLine = activeLine;
            App.execute(() -> {
                mConfig.setLine(finalLine);
                mConfig.save();
            });
        }
        if (!TextUtils.isEmpty(activeLine)) {
            mAdapter.setSelected(activeLine, true);
        }
        if (!items.isEmpty()) startCheck(false);
    }

    // ==================== 点击线路 → 打开站点列表 ====================

    @Override
    public void onItemClick(SiteItem item) {
        SiteListActivity.start(this, item.getUrl(), mType, item.getName());
    }

    @Override
    public void onToggle(SiteItem item, boolean enabled) {
        if (!enabled) {
            // 不允许取消选中线路，恢复开关状态
            mAdapter.setSelected(item.getUrl(), true);
            return;
        }
        // 选中该线路
        mAdapter.clearSelected();
        mAdapter.setSelected(item.getUrl(), true);
        App.execute(() -> {
            mConfig.setLine(item.getUrl());
            mConfig.save();
        });
    }

    // ==================== 分享 ====================

    private void onShare() {
        List<SiteItem> items = mAdapter.getItems();
        if (items.isEmpty()) {
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
        JsonArray array = new JsonArray();
        for (SiteItem item : mAdapter.getItems()) {
            JsonObject line = new JsonObject();
            line.addProperty("url", item.getUrl());
            line.addProperty("name", item.getName());
            array.add(line);
        }
        object.add("urls", array);
        return object.toString();
    }

    private void saveShareFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "depot_lines.json");
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
                java.io.File file = new java.io.File(dir, "depot_lines.json");
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

    // ==================== 导入 ====================

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
     * 将导入的 JSON 解析为线路，合并到仓库线路列表。
     * 支持 {"urls":[...]} 和 [{"url":"...","name":"..."}] 格式。
     */
    private void doImport(String json) {
        if (mConfig == null) {
            Notify.show(R.string.custom_site_import_fail);
            return;
        }
        App.execute(() -> {
            try {
                JsonElement root = Json.parse(json);
                JsonArray array;
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("urls")) array = obj.getAsJsonArray("urls");
                    else if (obj.has("sites")) array = obj.getAsJsonArray("sites");
                    else if (obj.has("lives")) array = obj.getAsJsonArray("lives");
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

                List<Depot> existing = new ArrayList<>(mConfig.getLineList());
                int added = 0;
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();
                    String url = obj.has("url") ? obj.get("url").getAsString() : "";
                    String name = obj.has("name") ? obj.get("name").getAsString() : "";
                    if (TextUtils.isEmpty(url)) continue;
                    // 去重
                    boolean found = false;
                    for (Depot d : existing) {
                        if (d.getUrl().equals(url)) { found = true; break; }
                    }
                    if (found) continue;
                    existing.add(new Depot(url, name));
                    added++;
                }
                if (added == 0) {
                    App.post(() -> Notify.show(R.string.custom_site_import_fail));
                    return;
                }
                mConfig.lines(existing).save();
                App.post(() -> {
                    Notify.show(R.string.custom_site_import_success);
                    loadLines();
                });
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
                    App.post(() -> mAdapter.setStatus(item.getUrl(), SiteListAdapter.STATUS_AVAILABLE));
                } else {
                    unavailable.incrementAndGet();
                    App.post(() -> mAdapter.setStatus(item.getUrl(), SiteListAdapter.STATUS_UNAVAILABLE));
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
}
