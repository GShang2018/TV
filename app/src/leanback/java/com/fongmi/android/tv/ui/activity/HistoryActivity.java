package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
    protected void initView() {
        setRecyclerView();
        getHistory();
        updateViewIcon();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPressed());
        mBinding.checkAll.setOnClickListener(this::onCheckAll);
        mBinding.delete.setOnClickListener(this::onDelete);
        mBinding.viewToggle.setOnClickListener(this::toggleView);
    }

    private void toggleView(View view) {
        ViewTypeMenu.show(this, view, R.menu.menu_view_type_simple, Setting.getHistoryViewType(), viewType -> {
            Setting.putHistoryViewType(viewType);
            if (mAdapter != null) {
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
        });
    }

    private void updateViewIcon() {
        mBinding.viewToggle.setImageResource(R.drawable.ic_action_view);
    }

    private Style getViewStyle() {
        switch (Setting.getHistoryViewType()) {
            case ViewType.PORTRAIT:
                return Style.rect();
            case ViewType.LIST:
                return Style.list();
            default:
                return Style.land();
        }
    }

    private void setRecyclerView() {
        Style style = getViewStyle();
        int column = Product.getColumn(style);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setClipToPadding(false);
        int padding = ResUtil.dp2px(8);
        mBinding.recycler.setPadding(padding, padding, padding, padding);
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this, style));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(column, 16));
    }

    private void getHistory() {
        mAdapter.addAll(History.get());
        mBinding.recycler.requestFocus();
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
                    mBinding.recycler.requestFocus();
                }).show();
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onSelectChanged(int count) {
        boolean select = mAdapter.isSelect();
        mBinding.back.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.checkAll.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.checkAll.setImageResource(mAdapter.isAllChecked() ? R.drawable.ic_action_select_all : R.drawable.ic_action_select_none);
        mBinding.delete.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.delete.setEnabled(count > 0);
        mBinding.delete.setAlpha(count > 0 ? 1.0f : 0.4f);
        mBinding.viewToggle.setVisibility(select ? View.GONE : View.VISIBLE);
        mBinding.title.setText(select ? getString(R.string.select_count, count) : getString(R.string.home_history));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        switch (event.getType()) {
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getHistory();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isSelect()) {
            mAdapter.setSelect(false);
            mBinding.recycler.requestFocus();
        } else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新封面样式
        if (mAdapter != null) {
            Style style = getViewStyle();
            int column = Product.getColumn(style);
            mAdapter.setStyle(style);
            mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
            // 清除旧的 ItemDecoration 并添加新的
            while (mBinding.recycler.getItemDecorationCount() > 0) {
                mBinding.recycler.removeItemDecorationAt(0);
            }
            mBinding.recycler.addItemDecoration(new SpaceItemDecoration(column, 16));
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RefreshEvent.history();
    }

}
