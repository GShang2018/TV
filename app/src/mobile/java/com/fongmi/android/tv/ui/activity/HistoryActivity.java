package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SyncDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class HistoryActivity extends BaseActivity implements HistoryAdapter.OnClickListener {

    private ActivityHistoryBinding mBinding;
    private HistoryAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        getHistory();
        updateViewIcon();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPressed());
        mBinding.sync.setOnClickListener(this::onSync);
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.checkAll.setOnClickListener(this::onCheckAll);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this));
        setLayout(Setting.getHistoryViewType());
    }

    private void setLayout(int viewType) {
        int column = viewType == ViewType.LIST ? Product.getListColumn(this) : (viewType == ViewType.PORTRAIT ? Product.getColumn(this) : Product.getColumn(this) - 1);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (column - 1));
        int imageWidth = (ResUtil.getScreenWidth(this) - space) / column;
        int imageHeight = viewType == ViewType.PORTRAIT ? imageWidth * 4 / 3 : imageWidth * 3 / 4;
        mAdapter.setSize(new int[]{imageWidth, imageHeight});
        mAdapter.setViewType(viewType);
    }

    private void toggleView(View view) {
        ViewTypeMenu.show(this, view, R.menu.menu_view_type_simple, Setting.getHistoryViewType(), viewType -> {
            Setting.putHistoryViewType(viewType);
            setLayout(viewType);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void updateViewIcon() {
        mBinding.view.setImageResource(R.drawable.ic_action_view);
    }

    private void getHistory() {
        mAdapter.addAll(History.get());
    }

    private void onSync(View view) {
        SyncDialog.create().history().show(this);
    }

    private void onCheckAll(View view) {
        mAdapter.setAll(!mAdapter.isAllChecked());
    }

    private void onDelete(View view) {
        int count = mAdapter.getSelectCount();
        if (count == 0) return;
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.dialog_delete_select, count))
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.select_delete, (dialog, which) -> {
                    for (History item : mAdapter.getSelected()) item.delete();
                    getHistory();
                }).show();
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onSelectChanged(int count) {
        boolean select = mAdapter.isSelect();
        mBinding.sync.setVisibility(select ? View.GONE : View.VISIBLE);
        mBinding.view.setVisibility(select ? View.GONE : View.VISIBLE);
        mBinding.checkAll.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.checkAll.setImageResource(mAdapter.isAllChecked() ? R.drawable.ic_action_select_all : R.drawable.ic_action_select_none);
        mBinding.delete.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.delete.setEnabled(count > 0);
        mBinding.delete.setAlpha(count > 0 ? 1.0f : 0.4f);
        mBinding.back.setImageResource(select ? R.drawable.ic_action_close : R.drawable.ic_control_back);
        mBinding.title.setText(select ? getString(R.string.select_count, count) : getString(R.string.app_history));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType().equals(RefreshEvent.Type.HISTORY)) getHistory();
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isSelect()) mAdapter.setSelect(false);
        else super.onBackPressed();
    }
}
