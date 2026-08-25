package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.ActivityLiveListBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.ui.adapter.ChannelGridAdapter;
import com.fongmi.android.tv.ui.adapter.GroupTabAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class LiveListActivity extends BaseActivity implements LiveCallback, GroupTabAdapter.OnClickListener, ChannelGridAdapter.OnClickListener {

    private ActivityLiveListBinding mBinding;
    private ChannelGridAdapter mChannelAdapter;
    private GroupTabAdapter mGroupAdapter;
    private Observer<Live> mObserveLive;
    private Observer<Epg> mObserveEpg;
    private LiveViewModel mViewModel;
    private List<Group> mHides;
    private Live mLive;
    private Group mGroup;

    public static void start(Context context) {
        if (!LiveConfig.isEmpty()) context.startActivity(new Intent(context, LiveListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private Live getHome() {
        return LiveConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveListBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        setViewModel();
        setSiteText();
        showProgress();
        checkLive();
    }

    @Override
    protected void initEvent() {
        mBinding.siteBox.setOnClickListener(this::onSite);
        mBinding.keep.setOnClickListener(this::onKeep);
    }

    private void setSiteText() {
        String site = getHome().getName();
        if (site.isEmpty()) site = LiveConfig.get().getConfig().getDesc();
        mBinding.site.setText(site.isEmpty() ? getString(R.string.live_source) : site);
        loadLogo();
    }

    private void loadLogo() {
        String logo = LiveConfig.get().getConfig().getLogo();
        if (logo == null || logo.isEmpty()) return;
        Glide.with(this).load(UrlUtil.convert(logo)).error(R.drawable.ic_logo).into(mBinding.logo);
    }

    private void setRecyclerView() {
        mBinding.type.setHasFixedSize(true);
        mBinding.type.setItemAnimator(null);
        mBinding.type.setAdapter(mGroupAdapter = new GroupTabAdapter(this));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mChannelAdapter = new ChannelGridAdapter(this);
        // 先设置 LayoutManager 与 item 尺寸，再挂 adapter，避免布局时 size 未初始化导致 NPE
        setGrid();
        mBinding.recycler.setAdapter(mChannelAdapter);
    }

    private void setGrid() {
        // 与点播横版布局完全一致：统一走 Product 尺寸计算
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn(this, Style.land())));
        if (mChannelAdapter != null) mChannelAdapter.size(Product.getSpec(this, Style.land()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mObserveLive = this::onLive;
        mObserveEpg = this::setEpg;
        mViewModel.live.observeForever(mObserveLive);
        mViewModel.epg.observeForever(mObserveEpg);
    }

    private void checkLive() {
        if (LiveConfig.isEmpty()) LiveConfig.get().init().load(getCallback());
        else getLive();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                getLive();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    private void getLive() {
        showProgress();
        mViewModel.getLive(getHome());
    }

    private void onLive(Live live) {
        if (live.isEmpty()) return;
        mLive = live;
        hideProgress();
        setGroup(live);
        setSiteText();
    }

    private void setGroup(Live live) {
        List<Group> items = new ArrayList<>();
        mHides = new ArrayList<>();
        for (Group group : live.getGroups()) (group.isHidden() ? mHides : items).add(group);
        mGroupAdapter.addAll(items);
        setPosition(LiveConfig.get().find(items));
    }

    private void setPosition(int[] position) {
        if (position[0] == -1 || mGroupAdapter.getItemCount() == 0) return;
        int size = mGroupAdapter.getItemCount();
        if (size == 1 || position[0] >= size) return;
        mGroup = mGroupAdapter.get(position[0]);
        mGroup.setPosition(position[1]);
        onItemClick(mGroup);
    }

    private void showGroup(Group item) {
        mGroupAdapter.setSelected(mGroup = item);
        mChannelAdapter.addAll(item.getChannel());
        if (item.getPosition() >= 0 && item.getPosition() < item.getChannel().size()) mChannelAdapter.setSelected(item.getChannel().get(item.getPosition()));
        mBinding.recycler.scrollToPosition(0);
        mBinding.empty.setVisibility(item.getChannel().isEmpty() ? View.VISIBLE : View.GONE);
        mViewModel.getEpgList(item.getChannel());
    }

    private void setEpg(Epg epg) {
        if (mGroup == null) return;
        for (Channel item : mGroup.getChannel()) {
            if (item.getTvgName().equals(epg.getKey())) {
                mChannelAdapter.changed(item);
                break;
            }
        }
    }

    private void onSite(View view) {
        LiveDialog.create().show(this);
    }

    private void onKeep(View view) {
        KeepActivity.start(this);
    }

    @Override
    public void onItemClick(Group item) {
        showGroup(item);
    }

    @Override
    public void onItemClick(Channel item) {
        if (item.getUrls().isEmpty()) return;
        LiveActivity.start(this, mGroup.getName(), item.getName());
    }

    @Override
    public void setLive(Live item) {
        if (item.isActivated()) item.getGroups().clear();
        LiveConfig.get().setHome(item);
        getLive();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setGrid();
        if (mChannelAdapter != null) mChannelAdapter.notifyDataSetChanged();
    }

    private void showProgress() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLive != null) getLive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.live.removeObserver(mObserveLive);
        mViewModel.epg.removeObserver(mObserveEpg);
    }
}
