package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.ActivityKeepBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.KeepAdapter;
import com.fongmi.android.tv.ui.adapter.KeepFolderAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.KeepBackup;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
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

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityKeepBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        showFolder();
        updateViewIcon();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(view -> onBackPressed());
        mBinding.add.setOnClickListener(view -> createFolder());
        mBinding.importBtn.setOnClickListener(this::onImport);
        mBinding.exportBtn.setOnClickListener(this::onExport);
        mBinding.viewToggle.setOnClickListener(this::toggleView);
        mBinding.checkAll.setOnClickListener(this::onCheckAll);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private Style getViewStyle() {
        switch (Setting.getKeepViewType()) {
            case ViewType.PORTRAIT:
                return Style.rect();
            case ViewType.LIST:
                return Style.list();
            default:
                return Style.land();
        }
    }

    private void toggleView(View view) {
        ViewTypeMenu.show(this, view, R.menu.menu_view_type_simple, Setting.getKeepViewType(), viewType -> {
            Setting.putKeepViewType(viewType);
            refreshStyle();
        });
    }

    private void updateViewIcon() {
        mBinding.viewToggle.setImageResource(R.drawable.ic_action_view);
    }

    private void refreshStyle() {
        if (mAdapter == null) return;
        Style style = getViewStyle();
        int column = Product.getColumn(style);
        mAdapter.setStyle(style);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
        while (mBinding.recycler.getItemDecorationCount() > 0) {
            mBinding.recycler.removeItemDecorationAt(0);
        }
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(column, 16));
        mAdapter.notifyDataSetChanged();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setClipToPadding(false);
        int padding = ResUtil.dp2px(8);
        mBinding.recycler.setPadding(padding, padding, padding, padding);
        mBinding.recycler.setAdapter(mFolderAdapter = new KeepFolderAdapter(this));
    }

    private void getFolder() {
        List<KeepFolder> folders = new ArrayList<>();
        KeepFolder def = new KeepFolder(getString(R.string.keep_folder_default));
        def.setId(0);
        folders.add(def);
        folders.addAll(KeepFolder.getAll());
        mFolderAdapter.addAll(folders);
    }

    private void getKeep() {
        mAdapter.addAll(Keep.getVod(mFolderId));
    }

    private void showFolder() {
        mMode = MODE_FOLDER;
        mFolderId = 0;
        mFolderName = null;
        mBinding.recycler.setAdapter(mFolderAdapter);
        // 收藏夹列表：一行一个
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, 1));
        while (mBinding.recycler.getItemDecorationCount() > 0) {
            mBinding.recycler.removeItemDecorationAt(0);
        }
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        getFolder();
        updateToolbar();
    }

    private void showKeep(KeepFolder folder) {
        mMode = MODE_KEEP;
        mFolderId = folder.getId();
        mFolderName = folder.getName();
        if (mAdapter == null) mAdapter = new KeepAdapter(this, getViewStyle());
        mBinding.recycler.setAdapter(mAdapter);
        refreshStyle();
        getKeep();
        updateToolbar();
    }

    private void updateToolbar() {
        boolean folder = mMode == MODE_FOLDER;
        mBinding.back.setVisibility(View.VISIBLE);
        mBinding.title.setText(folder ? getString(R.string.home_keep) : mFolderName);
        mBinding.add.setVisibility(folder ? View.VISIBLE : View.GONE);
        mBinding.importBtn.setVisibility(folder ? View.VISIBLE : View.GONE);
        mBinding.exportBtn.setVisibility(folder ? View.VISIBLE : View.GONE);
        mBinding.viewToggle.setVisibility(folder ? View.GONE : View.VISIBLE);
        mBinding.checkAll.setVisibility(View.GONE);
        mBinding.delete.setVisibility(View.GONE);
        mBinding.back.setImageResource(R.drawable.ic_action_arrow);
    }

    private boolean isSelecting() {
        return mMode == MODE_FOLDER ? mFolderAdapter.isSelect() : mAdapter.isSelect();
    }

    private int getSelectCount() {
        return mMode == MODE_FOLDER ? mFolderAdapter.getSelectCount() : mAdapter.getSelectCount();
    }

    private void updateSelectUI() {
        if (!isSelecting()) {
            updateToolbar();
            return;
        }
        int count = getSelectCount();
        mBinding.back.setImageResource(R.drawable.ic_action_close);
        mBinding.title.setText(getString(R.string.select_count, count));
        mBinding.add.setVisibility(View.GONE);
        mBinding.importBtn.setVisibility(View.GONE);
        mBinding.exportBtn.setVisibility(View.GONE);
        mBinding.viewToggle.setVisibility(View.GONE);
        mBinding.checkAll.setVisibility(View.VISIBLE);
        boolean all = mMode == MODE_FOLDER ? mFolderAdapter.isAllChecked() : mAdapter.isAllChecked();
        mBinding.checkAll.setImageResource(all ? R.drawable.ic_action_select_all : R.drawable.ic_action_select_none);
        mBinding.delete.setVisibility(View.VISIBLE);
        mBinding.delete.setEnabled(count > 0);
        mBinding.delete.setAlpha(count > 0 ? 1.0f : 0.4f);
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

    private void onCheckAll(View view) {
        if (mMode == MODE_FOLDER) mFolderAdapter.setAll(!mFolderAdapter.isAllChecked());
        else mAdapter.setAll(!mAdapter.isAllChecked());
    }

    private void onDelete(View view) {
        int count = getSelectCount();
        if (count == 0) return;
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.dialog_delete_select, count))
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.select_delete, (dialog, which) -> {
                    if (mMode == MODE_FOLDER) {
                        for (KeepFolder item : mFolderAdapter.getSelected()) KeepBackup.deleteFolder(item);
                        getFolder();
                    } else {
                        for (Keep item : mAdapter.getSelected()) {
                            // 在收藏夹视图中删除，只从当前收藏夹移出；在全部视图中删除，取消所有收藏
                            if (mMode == MODE_KEEP) item.deleteFromFolder(mFolderId);
                            else item.delete();
                        }
                        getKeep();
                    }
                    mBinding.recycler.requestFocus();
                }).show();
    }

    private void loadConfig(Config config, Keep item) {
        VodConfig.load(config, new Callback() {
            @Override
            public void success() {
                VideoActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
                RefreshEvent.history();
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
        if (event.getType() == RefreshEvent.Type.KEEP) {
            if (mMode == MODE_FOLDER) getFolder();
            else getKeep();
        }
    }

    @Override
    public void onItemClick(Keep item) {
        Config config = Config.find(item.getCid());
        if (config == null) CollectActivity.start(this, item.getVodName());
        else if (item.getCid() != VodConfig.getCid()) loadConfig(config, item);
        else VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onSelectChanged(int count) {
        updateSelectUI();
    }

    @Override
    public void onItemClick(KeepFolder item) {
        showKeep(item);
    }

    @Override
    public void onBackPressed() {
        if (isSelecting()) {
            if (mMode == MODE_FOLDER) mFolderAdapter.setSelect(false);
            else mAdapter.setSelect(false);
            mBinding.recycler.requestFocus();
        } else if (mMode == MODE_KEEP) {
            showFolder();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMode == MODE_KEEP) refreshStyle();
    }
}
