package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.ActivityKeepBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.KeepAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Notify;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class KeepActivity extends BaseActivity implements KeepAdapter.OnClickListener {

    private ActivityKeepBinding mBinding;
    private KeepAdapter mAdapter;

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
        getKeep();
        updateViewIcon();
    }

    @Override
    protected void initEvent() {
        mBinding.delete.setOnClickListener(this::onDelete);
        mBinding.viewToggle.setOnClickListener(this::toggleView);
    }

    private Style getViewStyle() {
        return Setting.getKeepViewType() == ViewType.PORTRAIT ? Style.rect() : Style.land();
    }

    private void toggleView(View view) {
        if (Setting.getKeepViewType() == ViewType.PORTRAIT) {
            Setting.putKeepViewType(ViewType.GRID);
        } else {
            Setting.putKeepViewType(ViewType.PORTRAIT);
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
        if (Setting.getKeepViewType() == ViewType.PORTRAIT) {
            mBinding.viewToggle.setImageResource(R.drawable.ic_action_grid);
        } else {
            mBinding.viewToggle.setImageResource(R.drawable.ic_action_portrait);
        }
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            mAdapter.setDelete(false);
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
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
        mBinding.recycler.setAdapter(mAdapter = new KeepAdapter(this, style));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(column, 16));
    }

    private void getKeep() {
        mAdapter.addAll(Keep.getVod());
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.KEEP) getKeep();
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
        mAdapter.delete(item.delete());
        if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(true);
        return true;
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
}
