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
        mBinding.delete.setOnClickListener(this::onDelete);
        mBinding.viewToggle.setOnClickListener(this::toggleView);
    }

    private void toggleView(View view) {
        if (Setting.getHistoryViewType() == ViewType.PORTRAIT) {
            Setting.putHistoryViewType(ViewType.GRID);
        } else {
            Setting.putHistoryViewType(ViewType.PORTRAIT);
        }
        updateViewIcon();
        // 刷新封面样式
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
    }

    private void updateViewIcon() {
        if (Setting.getHistoryViewType() == ViewType.PORTRAIT) {
            mBinding.viewToggle.setImageResource(R.drawable.ic_action_grid);
        } else {
            mBinding.viewToggle.setImageResource(R.drawable.ic_action_portrait);
        }
    }

    private Style getViewStyle() {
        return Setting.getHistoryViewType() == ViewType.PORTRAIT ? new Style("rect", 0.75f) : Style.rect();
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
        mBinding.delete.setFocusable(false);
        mAdapter.addAll(History.get());
        App.post(() -> {
            mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
            mBinding.delete.setFocusable(true);
        }, 500);
        mBinding.recycler.requestFocus();
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_history).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> mAdapter.clear()).show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mBinding.delete.setFocusable(false);
        int index = mAdapter.delete(item.delete());
        if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
        App.post(() -> {
            mBinding.delete.setFocusable(true);
        }, 300);
        if (mAdapter.getItemCount() > 0) {
            int nextIndex = index + 1;
            if (index == mAdapter.getItemCount()) nextIndex = index - 1;
            View view  = mBinding.recycler.getLayoutManager().findViewByPosition(nextIndex);
            if (view != null) view.requestFocus();
        }
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(true);
        return true;
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
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackPressed();
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
