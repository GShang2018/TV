package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.databinding.ActivityKeepBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.KeepAdapter;
import com.fongmi.android.tv.ui.adapter.KeepFolderAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SyncDialog;
import com.fongmi.android.tv.ui.dialog.WebDavDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.KeepBackup;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KeepActivity extends BaseActivity implements KeepAdapter.OnClickListener, KeepFolderAdapter.OnClickListener {

    private static final int MODE_FOLDER = 0;
    private static final int MODE_KEEP = 1;

    private ActivityKeepBinding mBinding;
    private KeepAdapter mAdapter;
    private KeepFolderAdapter mFolderAdapter;
    private int mMode = MODE_FOLDER;
    private int mFolderId;
    private String mFolderName;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, KeepActivity.class));
    }

    public static void start(Activity activity, int folderId) {
        Intent intent = new Intent(activity, KeepActivity.class);
        intent.putExtra("folderId", folderId);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityKeepBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        int folderId = getIntent().getIntExtra("folderId", -1);
        if (folderId == 0) {
            KeepFolder def = new KeepFolder(getString(R.string.keep_folder_default));
            def.setId(0);
            showKeep(def);
        } else if (folderId > 0) {
            KeepFolder folder = KeepFolder.find(folderId);
            if (folder != null) {
                showKeep(folder);
            } else {
                showFolder();
            }
        } else {
            showFolder();
        }
        updateViewIcon();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(view -> onBackPressed());
        mBinding.add.setOnClickListener(view -> createFolder());
        mBinding.importBtn.setOnClickListener(this::onImport);
        mBinding.exportBtn.setOnClickListener(this::onExport);
        mBinding.sync.setOnClickListener(this::onSync);
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setAdapter(mFolderAdapter = new KeepFolderAdapter(this));
    }

    private void setLayout(int viewType) {
        int column = viewType == ViewType.LIST ? Product.getListColumn(this) : (viewType == ViewType.PORTRAIT ? Product.getColumn(this) : Product.getColumn(this) - 1);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (column - 1));
        int imageWidth = (ResUtil.getScreenWidth(this) - space) / column;
        int imageHeight = viewType == ViewType.PORTRAIT ? imageWidth * 4 / 3 : imageWidth * 3 / 4;
        if (mAdapter != null) mAdapter.setSize(new int[]{imageWidth, imageHeight});
        if (mAdapter != null) mAdapter.setViewType(viewType);
    }

    private void toggleView(View view) {
        ViewTypeMenu.show(this, view, R.menu.menu_view_type_simple, Setting.getKeepViewType(), viewType -> {
            Setting.putKeepViewType(viewType);
            setLayout(viewType);
            if (mAdapter != null) mAdapter.notifyDataSetChanged();
        });
    }

    private void updateViewIcon() {
        mBinding.view.setImageResource(R.drawable.ic_action_view);
    }

    private void getFolder() {
        List<KeepFolder> folders = new ArrayList<>();
        KeepFolder def = new KeepFolder(getString(R.string.keep_folder_default));
        def.setId(0);
        folders.add(def);
        folders.addAll(KeepFolder.getAll());
        mFolderAdapter.addAll(folders);
        mBinding.delete.setVisibility(mFolderAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void getKeep() {
        mAdapter.addAll(Keep.getVod(mFolderId));
        mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void showFolder() {
        mMode = MODE_FOLDER;
        mFolderId = 0;
        mFolderName = null;
        mBinding.recycler.setAdapter(mFolderAdapter);
        // 收藏夹列表：一行一个
        mBinding.recycler.setLayoutManager(new LinearLayoutManager(this));
        getFolder();
        updateToolbar();
    }

    private void showKeep(KeepFolder folder) {
        mMode = MODE_KEEP;
        mFolderId = folder.getId();
        mFolderName = folder.getName();
        if (mAdapter == null) mAdapter = new KeepAdapter(this);
        mBinding.recycler.setAdapter(mAdapter);
        setLayout(Setting.getKeepViewType());
        getKeep();
        updateToolbar();
    }

    private void updateToolbar() {
        boolean folder = mMode == MODE_FOLDER;
        mBinding.back.setVisibility(View.VISIBLE);
        mBinding.title.setText(folder ? getString(R.string.app_keep) : mFolderName);
        mBinding.add.setVisibility(folder ? View.VISIBLE : View.GONE);
        mBinding.importBtn.setVisibility(folder ? View.VISIBLE : View.GONE);
        mBinding.exportBtn.setVisibility(folder ? View.VISIBLE : View.GONE);
        mBinding.sync.setVisibility(View.VISIBLE);
        mBinding.view.setVisibility(folder ? View.GONE : View.VISIBLE);
        mBinding.delete.setVisibility(View.VISIBLE);
    }

    private void createFolder() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        EditText editText = dialogView.findViewById(R.id.editText);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.keep_folder_create)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    String name = editText.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) return;
                    new KeepFolder(name).save();
                    getFolder();
                }).show();
    }

    private void onImport(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/*", "*/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        try {
            startActivityForResult(Intent.createChooser(intent, ""), FileChooser.REQUEST_PICK_FILE);
        } catch (Exception e) {
            Notify.show(R.string.keep_import_fail);
        }
    }

    private void onExport(View view) {
        File file = KeepBackup.export();
        Notify.show(file != null ? R.string.keep_export_success : R.string.keep_import_fail);
    }

    private void onSync(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.keep_sync)
                .setItems(new CharSequence[]{getString(R.string.keep_sync_lan), getString(R.string.keep_sync_webdav)}, (dialog, which) -> {
                    if (which == 0) SyncDialog.create().keep().show(this);
                    else WebDavDialog.create().show(this);
                }).show();
    }

    private void onDelete(View view) {
        if (mMode == MODE_FOLDER) {
            if (mFolderAdapter.isDelete()) {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_keep).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> mFolderAdapter.setDelete(false)).show();
            } else if (mFolderAdapter.getItemCount() > 0) {
                mFolderAdapter.setDelete(true);
            } else {
                mBinding.delete.setVisibility(View.GONE);
            }
        } else {
            if (mAdapter.isDelete()) {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_keep).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> mAdapter.clear()).show();
            } else if (mAdapter.getItemCount() > 0) {
                mAdapter.setDelete(true);
            } else {
                mBinding.delete.setVisibility(View.GONE);
            }
        }
    }

    private void loadConfig(Config config, Keep item) {
        VodConfig.load(config, new Callback() {
            @Override
            public void success() {
                VideoActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE || data == null) return;
        String path = FileChooser.getPathFromUri(this, data.getData());
        if (path == null) {
            Notify.show(R.string.keep_import_fail);
            return;
        }
        boolean ok = KeepBackup.importFile(new File(path));
        Notify.show(ok ? R.string.keep_import_success : R.string.keep_import_fail);
        getFolder();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (!event.getType().equals(RefreshEvent.Type.KEEP)) return;
        if (mMode == MODE_FOLDER) getFolder();
        else getKeep();
    }

    @Override
    public void onItemClick(Keep item) {
        Config config = Config.find(item.getCid());
        if (config == null) CollectActivity.start(this, item.getVodName());
        else if (item.getCid() != VodConfig.getCid()) loadConfig(config, item);
        else VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(Keep item) {
        // 在收藏夹视图中删除，只从当前收藏夹移出；在全部视图中删除，取消所有收藏
        if (mMode == MODE_KEEP) item.deleteFromFolder(mFolderId);
        else item.delete();
        mAdapter.remove(item);
        if (mAdapter.getItemCount() > 0) return;
        mBinding.delete.setVisibility(View.GONE);
        mAdapter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(!mAdapter.isDelete());
        return true;
    }

    @Override
    public void onItemClick(KeepFolder item) {
        showKeep(item);
    }

    @Override
    public void onItemDelete(KeepFolder item) {
        if (item.getId() == 0) return;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.keep_folder_delete).setMessage(R.string.keep_folder_delete_msg).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            KeepBackup.deleteFolder(item);
            getFolder();
        }).show();
    }

    @Override
    public void onBackPressed() {
        if (mMode == MODE_KEEP) {
            showFolder();
        } else if (mFolderAdapter.isDelete()) {
            mFolderAdapter.setDelete(false);
        } else {
            super.onBackPressed();
        }
    }
}
