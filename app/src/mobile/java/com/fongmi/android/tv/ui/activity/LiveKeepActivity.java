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
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.ActivityLiveRecordBinding;
import com.fongmi.android.tv.ui.adapter.LiveChannelAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// 直播收藏页：数据来自 Keep(type=1)，与点播收藏页结构一致
public class LiveKeepActivity extends BaseActivity implements LiveChannelAdapter.OnClickListener {

    private ActivityLiveRecordBinding mBinding;
    private LiveChannelAdapter mAdapter;
    private boolean mInited;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, LiveKeepActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveRecordBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.title.setText(R.string.live_keep);
        setRecyclerView();
        getData();
        updateViewIcon();
        mInited = true;
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPressed());
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.checkAll.setOnClickListener(this::onCheckAll);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setAdapter(mAdapter = new LiveChannelAdapter(this));
        setLayout(Setting.getLiveKeepViewType());
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
        ViewTypeMenu.show(this, view, R.menu.menu_view_type_simple, Setting.getLiveKeepViewType(), viewType -> {
            Setting.putLiveKeepViewType(viewType);
            setLayout(viewType);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void updateViewIcon() {
        mBinding.view.setImageResource(R.drawable.ic_action_view);
    }

    private void getData() {
        List<Channel> items = new ArrayList<>();
        for (Keep keep : Keep.getLive()) {
            Channel channel = findChannel(keep.getKey());
            if (channel == null) channel = Channel.create(keep.getKey());
            items.add(channel);
        }
        mAdapter.addAll(items);
    }

    // 在直播源分组中查找频道（跳过收藏分组定位原分组），找不到返回 null
    private Channel findChannel(String name) {
        Live home = LiveConfig.get().getHome();
        if (home.isEmpty()) return null;
        for (Group group : home.getGroups()) {
            if (group.isKeep()) continue;
            for (Channel channel : group.getChannel()) if (channel.getName().equals(name)) return channel.group(group);
        }
        return null;
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
                    for (Channel item : mAdapter.getSelected()) Keep.delete(item.getName());
                    getData();
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从播放页返回后刷新（收藏状态可能已变化）
        if (mInited) getData();
    }

    @Override
    public void onItemClick(Channel item) {
        Channel channel = findChannel(item.getName());
        if (channel == null) {
            Notify.show(R.string.live_channel_missing);
            return;
        }
        LiveActivity.start(this, channel.getGroup().getName(), channel.getName());
    }

    @Override
    public void onSelectChanged(int count) {
        boolean select = mAdapter.isSelect();
        mBinding.view.setVisibility(select ? View.GONE : View.VISIBLE);
        mBinding.checkAll.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.checkAll.setImageResource(mAdapter.isAllChecked() ? R.drawable.ic_action_select_all : R.drawable.ic_action_select_none);
        mBinding.delete.setVisibility(select ? View.VISIBLE : View.GONE);
        mBinding.delete.setEnabled(count > 0);
        mBinding.delete.setAlpha(count > 0 ? 1.0f : 0.4f);
        mBinding.back.setImageResource(select ? R.drawable.ic_action_close : R.drawable.ic_control_back);
        mBinding.title.setText(select ? getString(R.string.select_count, count) : getString(R.string.live_keep));
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isSelect()) mAdapter.setSelect(false);
        else super.onBackPressed();
    }
}
