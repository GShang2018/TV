package com.fongmi.android.tv.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.CustomVod;
import com.fongmi.android.tv.databinding.FragmentMineBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.activity.MineEditActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.adapter.MineAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.MineBackup;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.Locale;

public class MineFragment extends BaseFragment implements MineAdapter.OnClickListener {

    private FragmentMineBinding mBinding;
    private MineAdapter mAdapter;

    public static MineFragment newInstance() {
        return new MineFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentMineBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setRecyclerView();
        getVod();
    }

    @Override
    protected void initEvent() {
        mBinding.add.setOnClickListener(view -> MineEditActivity.start(requireActivity()));
        mBinding.importBtn.setOnClickListener(this::onImport);
        mBinding.exportBtn.setOnClickListener(this::onExport);
        mBinding.mode.setOnClickListener(this::onMode);
        mBinding.view.setOnClickListener(this::onViewType);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setAdapter(mAdapter = new MineAdapter(this));
        setLayout(Setting.getMineViewType());
    }

    private void setLayout(int viewType) {
        int column = viewType == ViewType.LIST ? Product.getListColumn(requireActivity()) : (viewType == ViewType.PORTRAIT ? Product.getColumn(requireActivity()) : Product.getColumn(requireActivity()) - 1);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), column));
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (column - 1));
        int imageWidth = (ResUtil.getScreenWidth(requireActivity()) - space) / column;
        int imageHeight = viewType == ViewType.PORTRAIT ? imageWidth * 4 / 3 : imageWidth * 3 / 4;
        mAdapter.setSize(new int[]{imageWidth, imageHeight});
        mAdapter.setViewType(viewType);
    }

    /** 顶栏 编辑/预览 切换：编辑模式可改可删，预览模式纯浏览播放 */
    private void onMode(View view) {
        mAdapter.setEdit(!mAdapter.isEdit());
        updateModeIcon();
        updateDeleteVisibility();
    }

    /** 布局切换（竖版海报/横版海报/列表），与首页 ViewTypeMenu 一致 */
    private void onViewType(View view) {
        ViewTypeMenu.show(requireActivity(), view, R.menu.menu_view_type_simple, Setting.getMineViewType(), viewType -> {
            Setting.putMineViewType(viewType);
            setLayout(viewType);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void updateModeIcon() {
        if (mAdapter.isEdit()) {
            mBinding.mode.setImageResource(R.drawable.ic_action_eye);
            mBinding.mode.setContentDescription(getString(R.string.mine_mode_preview));
        } else {
            mBinding.mode.setImageResource(R.drawable.ic_action_edit);
            mBinding.mode.setContentDescription(getString(R.string.mine_mode_edit));
        }
    }

    private void updateDeleteVisibility() {
        mBinding.delete.setVisibility(mAdapter.isEdit() && mAdapter.getItemCount() > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void getVod() {
        mAdapter.addAll(CustomVod.getAll());
        mBinding.empty.setVisibility(mAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        if (mAdapter.getItemCount() == 0 && mAdapter.isEdit()) mAdapter.setEdit(false);
        updateModeIcon();
        updateDeleteVisibility();
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
            Notify.show(R.string.mine_import_fail);
        }
    }

    private void onExport(View view) {
        File file = MineBackup.export();
        if (file != null) Notify.show(getString(R.string.mine_export_success, file.getName()));
        else Notify.show(R.string.mine_import_fail);
    }

    private void onDelete(View view) {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.mine_delete_confirm_title)
                .setMessage(R.string.mine_delete_all_confirm_msg)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    mAdapter.clear();
                    getVod();
                })
                .show();
    }

    @Override
    public void onItemClick(CustomVod item) {
        play(item);
    }

    @Override
    public void onEdit(CustomVod item) {
        MineEditActivity.start(requireActivity(), item.getId());
    }

    @Override
    public void onItemDelete(CustomVod item) {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.mine_delete_confirm_title)
                .setMessage(R.string.mine_delete_one_confirm_msg)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    item.delete();
                    mAdapter.remove(item);
                    if (mAdapter.getItemCount() == 0) {
                        mAdapter.setEdit(false);
                        updateModeIcon();
                        mBinding.empty.setVisibility(View.VISIBLE);
                    }
                    updateDeleteVisibility();
                })
                .show();
    }

    /** 解析 CMS 标准播放串（$$$ 线路 / # 集数 / $ 名称分隔），选择后直链播放 */
    private void play(CustomVod item) {
        if (!item.hasPlayUrl()) {
            Notify.show(R.string.mine_play_url_empty);
            return;
        }
        String[] lines = item.getVodPlayUrl().split("\\$\\$\\$");
        if (lines.length == 1) {
            showEpisodeDialog(item, getLineName(item, 0), lines[0]);
        } else {
            String[] names = new String[lines.length];
            for (int i = 0; i < lines.length; i++) names[i] = getLineName(item, i);
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.mine_line)
                    .setItems(names, (dialog, which) -> showEpisodeDialog(item, names[which], lines[which]))
                    .setNegativeButton(R.string.dialog_negative, null)
                    .show();
        }
    }

    private void showEpisodeDialog(CustomVod item, String lineName, String data) {
        String[] eps = data.split("#");
        if (eps.length == 1) {
            playUrl(item, getEpisodeUrl(eps[0], 1));
            return;
        }
        String[] names = new String[eps.length];
        for (int i = 0; i < eps.length; i++) names[i] = getEpisodeName(eps[i], i + 1);
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(lineName)
                .setItems(names, (dialog, which) -> playUrl(item, getEpisodeUrl(eps[which], which + 1)))
                .setNegativeButton(R.string.dialog_negative, null)
                .show();
    }

    private String getLineName(CustomVod item, int index) {
        String[] froms = item.getVodPlayFrom().split("\\$\\$\\$");
        if (index < froms.length && !froms[index].trim().isEmpty()) return froms[index].trim();
        return getString(R.string.mine_line) + " " + (index + 1);
    }

    private String getEpisodeName(String data, int index) {
        String[] split = data.split("\\$");
        return split.length > 1 ? split[0].trim() : String.format(Locale.getDefault(), "%02d", index);
    }

    private String getEpisodeUrl(String data, int index) {
        String[] split = data.split("\\$");
        String url = split.length > 1 ? split[1].trim() : data.trim();
        if (url.isEmpty() && split.length > 1) url = split[0].trim();
        return url;
    }

    private void playUrl(CustomVod item, String url) {
        if (TextUtils.isEmpty(url)) {
            Notify.show(R.string.mine_play_url_empty);
            return;
        }
        VideoActivity.start(requireActivity(), "push_agent", url, item.getVodName(), item.getVodPic());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE || data == null) return;
        String path = FileChooser.getPathFromUri(getContext(), data.getData());
        if (path == null) {
            Notify.show(R.string.mine_import_fail);
            return;
        }
        boolean ok = MineBackup.importFile(new File(path));
        Notify.show(ok ? R.string.mine_import_success : R.string.mine_import_fail);
        getVod();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType().equals(RefreshEvent.Type.MINE)) getVod();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null && mAdapter.getItemCount() > 0) getVod();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }
}
