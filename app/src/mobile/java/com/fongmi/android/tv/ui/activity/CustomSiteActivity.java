package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
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
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.CustomLine;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.databinding.ActivityCustomSiteBinding;
import com.fongmi.android.tv.databinding.DialogLinkBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.CustomSiteListAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.CustomSiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.permissionx.guolindev.PermissionX;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomSiteActivity extends BaseActivity implements CustomSiteListAdapter.OnClickListener {

    private static final int REQUEST_SAVE_FILE = 10001;
    private static final int REQUEST_PICK_FILE = 10002;

    private ActivityCustomSiteBinding mBinding;
    private CustomSiteListAdapter mAdapter;
    // 为空表示管理全局自定义站点(custom.json)；非空表示管理某条自定义线路的站点
    private String mLineId;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, CustomSiteActivity.class));
    }

    public static void start(Activity activity, String lineId, String lineName) {
        Intent intent = new Intent(activity, CustomSiteActivity.class);
        intent.putExtra("line", lineId);
        intent.putExtra("title", lineName);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCustomSiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mLineId = getIntent().getStringExtra("line");
        String title = getIntent().getStringExtra("title");
        if (title != null && !title.isEmpty()) mBinding.title.setText(title);
        mAdapter = new CustomSiteListAdapter(this);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        mBinding.recycler.setAdapter(mAdapter);
        refreshList();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPress());
        mBinding.add.setOnClickListener(v -> {
            if (mLineId == null) CustomSiteDialog.create(this).setOnSaved(this::refreshList).show();
            else CustomSiteDialog.create(this, mLineId).setOnSaved(this::refreshList).show();
        });
        mBinding.share.setOnClickListener(v -> onShare());
        mBinding.importBtn.setOnClickListener(v -> onImport());
        mBinding.check.setOnClickListener(v -> onCheck());
    }

    @Override
    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        finish();
    }

    private void refreshList() {
        List<CustomSite> items = readSites();
        mBinding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter.addAll(items);
        // 进入页面自动检测一次可用性，不弹进度框，圆点实时更新
        if (!items.isEmpty()) startCheck(false);
    }

    private CustomLine getLine() {
        return mLineId == null ? null : CustomLine.find(mLineId);
    }

    private List<CustomSite> readSites() {
        CustomLine line = getLine();
        return line == null ? new ArrayList<>(CustomSite.getAll()) : line.sites();
    }

    private void writeSites(List<CustomSite> items) {
        CustomLine line = getLine();
        if (line == null) CustomSite.saveAll(items);
        else line.sites(items).save();
    }

    @Override
    public void onToggle(CustomSite item, boolean enabled) {
        item.setEnabled(enabled);
        List<CustomSite> items = readSites();
        items.remove(item);
        items.add(item);
        writeSites(items);
        refreshConfig();
    }

    @Override
    public void onEdit(CustomSite item) {
        if (mLineId == null) CustomSiteDialog.create(this, item).setOnSaved(this::refreshList).show();
        else CustomSiteDialog.create(this, mLineId, item).setOnSaved(this::refreshList).show();
    }

    @Override
    public void onCopy(CustomSite item) {
        Util.copy(item.getApi());
    }

    @Override
    public void onDelete(CustomSite item) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_custom_site_title).setMessage(R.string.dialog_delete_custom_site).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            List<CustomSite> items = readSites();
            items.remove(item);
            writeSites(items);
            Notify.show(R.string.custom_site_deleted);
            refreshConfig();
            refreshList();
        }).show();
    }

    // ==================== 分享 ====================

    private void onShare() {
        List<CustomSite> items = readSites();
        if (items.isEmpty()) {
            Notify.show(R.string.custom_site_empty);
            return;
        }
        new MaterialAlertDialogBuilder(this)
            .setItems(new CharSequence[]{
                ResUtil.getString(R.string.custom_site_share_file),
                ResUtil.getString(R.string.custom_site_share_clipboard)
            }, (dialog, which) -> {
                if (which == 0) {
                    saveShareFile();
                } else {
                    shareToClipboard();
                }
            }).show();
    }

    private String buildShareJson() {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        for (CustomSite item : readSites()) {
            array.add(App.gson().toJsonTree(item));
        }
        object.add("sites", array);
        return object.toString();
    }

    private void saveShareFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "custom_sites.json");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SAVE_FILE);
        } else {
            // 回退到本地目录写入
            writeShareToLocal();
        }
    }

    private void writeShareToLocal() {
        App.execute(() -> {
            try {
                File dir = new File(Path.root(), "Download");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "custom_sites.json");
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
            android.content.ClipboardManager manager = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(android.content.ClipData.newPlainText("", json));
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
                if (which == 0) {
                    importFromClipboard();
                } else if (which == 1) {
                    importFromUrl();
                } else {
                    importFromFile();
                }
            }).show();
    }

    private void ensurePermission(Runnable action) {
        if (!PermissionX.isGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) action.run();
                    else Notify.show(R.string.custom_site_import_fail);
                });
        } else {
            action.run();
        }
    }

    private void importFromClipboard() {
        android.content.ClipboardManager manager = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clipData = manager == null ? null : manager.getPrimaryClip();
        CharSequence clipText = "";
        if (clipData != null && clipData.getItemCount() > 0) {
            clipText = clipData.getItemAt(0).coerceToText(this);
        }
        if (TextUtils.isEmpty(clipText)) {
            Notify.show(R.string.custom_site_import_empty);
            return;
        }
        String text = clipText.toString();
        ensurePermission(() -> doImport(text));
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
                ensurePermission(() -> App.execute(() -> {
                    String content = OkHttp.string(url);
                    App.post(() -> {
                        if (TextUtils.isEmpty(content)) {
                            Notify.show(R.string.custom_site_import_fail);
                        } else {
                            doImport(content);
                        }
                    });
                }));
            }).show();
    }

    private void importFromFile() {
        Intent intent = new Intent(Util.isTvBox() ? Intent.ACTION_GET_CONTENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, ""), REQUEST_PICK_FILE);
        }
    }

    private void doImport(String json) {
        App.execute(() -> {
            try {
                JsonElement root = Json.parse(json);
                JsonArray array;
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("sites")) {
                        array = obj.getAsJsonArray("sites");
                    } else {
                        App.post(() -> Notify.show(R.string.custom_site_import_fail));
                        return;
                    }
                } else if (root.isJsonArray()) {
                    array = root.getAsJsonArray();
                } else {
                    App.post(() -> Notify.show(R.string.custom_site_import_fail));
                    return;
                }

                List<CustomSite> existing = new ArrayList<>(readSites());
                int added = 0;
                for (JsonElement element : array) {
                    CustomSite item = App.gson().fromJson(element, CustomSite.class);
                    if (item == null || TextUtils.isEmpty(item.getKey())) continue;
                    existing.remove(item);
                    existing.add(item);
                    added++;
                }
                if (added == 0) {
                    App.post(() -> Notify.show(R.string.custom_site_import_fail));
                    return;
                }
                writeSites(existing);
                App.post(() -> {
                    Notify.show(R.string.custom_site_import_success);
                    refreshList();
                    refreshConfig();
                });
            } catch (Exception e) {
                App.post(() -> Notify.show(R.string.custom_site_import_fail));
            }
        });
    }

    // ==================== 检测可用性 ====================

    private void onCheck() {
        startCheck(true);
    }

    private void startCheck(boolean manual) {
        List<CustomSite> items = readSites();
        if (items.isEmpty()) {
            if (manual) Notify.show(R.string.custom_site_empty);
            return;
        }
        // 手动检测显示提示，自动检测静默执行
        if (manual) {
            Notify.progress(this);
        }
        mAdapter.clearStatus();

        int total = items.size();
        // 专用线程池，池大小根据站点数动态调整，最大32，确保全部并发
        int poolSize = Math.min(total, 32);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(poolSize);
        AtomicInteger available = new AtomicInteger(0);
        AtomicInteger unavailable = new AtomicInteger(0);
        AtomicInteger done = new AtomicInteger(0);

        for (CustomSite item : items) {
            executor.execute(() -> {
                boolean ok = checkSite(item.getApi());
                if (ok) {
                    available.incrementAndGet();
                    App.post(() -> mAdapter.setStatus(item.getKey(), CustomSiteListAdapter.STATUS_AVAILABLE));
                } else {
                    unavailable.incrementAndGet();
                    App.post(() -> mAdapter.setStatus(item.getKey(), CustomSiteListAdapter.STATUS_UNAVAILABLE));
                }
                if (done.incrementAndGet() == total) {
                    App.post(() -> {
                        if (manual) Notify.dismiss();
                    });
                    executor.shutdown();
                }
            });
        }
    }

    private boolean checkSite(String api) {
        if (TextUtils.isEmpty(api)) return false;
        try {
            okhttp3.OkHttpClient client = OkHttp.client(5000);
            okhttp3.Call call = OkHttp.newCall(client, api);
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

    // ==================== 文件操作回调 ====================

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

    // ==================== 配置刷新 ====================

    private void refreshConfig() {
        Config config = VodConfig.get().getConfig();
        if (config != null && config.isCustom()) {
            VodConfig.load(config, new Callback() {
                @Override
                public void success(String result) {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }

                @Override
                public void success() {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }

                @Override
                public void error(String msg) {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }
            });
        } else {
            RefreshEvent.video();
            RefreshEvent.config();
        }
    }
}
