package com.fongmi.android.tv.ui.fragment;

import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.FragmentLiveBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.adapter.ChannelGridAdapter;
import com.fongmi.android.tv.ui.adapter.GroupTabAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.ui.dialog.LineSelectDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class LiveFragment extends BaseFragment implements LiveCallback, GroupTabAdapter.OnClickListener, ChannelGridAdapter.OnClickListener {

    private FragmentLiveBinding mBinding;
    private ChannelGridAdapter mChannelAdapter;
    private GroupTabAdapter mGroupAdapter;
    private Observer<Live> mObserveLive;
    private Observer<Epg> mObserveEpg;
    private LiveViewModel mViewModel;
    private List<Group> mHides;
    private Live mLive;
    private Group mGroup;
    private int mColumn;

    public static LiveFragment newInstance() {
        return new LiveFragment();
    }

    private Live getHome() {
        return LiveConfig.get().getHome();
    }

    private Group getKeep() {
        return mLive == null || mLive.getGroups().isEmpty() ? Group.create() : mLive.getGroups().get(0);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentLiveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        setSiteText();
        showProgress();
        checkLive();
    }

    @Override
    protected void initEvent() {
        mBinding.logo.setOnClickListener(this::onLogo);
        mBinding.siteBox.setOnClickListener(this::onSite);
        mBinding.view.setOnClickListener(this::toggleView);
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
        mBinding.recycler.setAdapter(mChannelAdapter = new ChannelGridAdapter(this));
        setGrid();
    }

    private void setGrid() {
        int viewType = Setting.getLiveViewType();
        boolean land = ResUtil.isLand(requireContext());
        if (mChannelAdapter != null) mChannelAdapter.setType(viewType);
        if (viewType == ViewType.LIST) {
            mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), Product.getListColumn(requireContext())));
        } else {
            mColumn = viewType == ViewType.PORTRAIT ? (land ? 8 : 5) : (land ? 6 : 4);
            int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (mColumn - 1));
            int base = ResUtil.getScreenWidth(requireContext()) - space;
            int width = base / mColumn;
            int height = viewType == ViewType.PORTRAIT ? width * 4 / 3 : width * 3 / 4;
            mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), mColumn));
            if (mChannelAdapter != null) mChannelAdapter.size(new int[]{width, height});
        }
    }

    private void toggleView(View view) {
        ViewTypeMenu.show(requireContext(), view, R.menu.menu_view_type_live, Setting.getLiveViewType(), viewType -> {
            Setting.putLiveViewType(viewType);
            setGrid();
            if (mChannelAdapter != null) mChannelAdapter.notifyDataSetChanged();
        });
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

    private void onLogo(View view) {
        LineSelectDialog.createLiveAll(this).show(this);
    }

    private void onSite(View view) {
        LiveDialog.create().show(this);
    }

    private void addKeep(Channel item) {
        getKeep().add(item);
        Keep keep = new Keep();
        keep.setKey(item.getName());
        keep.setType(1);
        keep.save();
    }

    private void delKeep(Channel item) {
        if (mGroup.isKeep()) mChannelAdapter.clear();
        getKeep().getChannel().remove(item);
        Keep.delete(item.getName());
    }

    @Override
    public void onItemClick(Group item) {
        showGroup(item);
    }

    @Override
    public void onItemClick(Channel item) {
        if (item.getUrls().isEmpty()) return;
        LiveActivity.start(requireContext(), mGroup.getName(), item.getName());
    }

    @Override
    public boolean onLongClick(Channel item) {
        boolean exist = Keep.exist(item.getName());
        Notify.show(exist ? R.string.keep_del : R.string.keep_add);
        if (exist) delKeep(item);
        else addKeep(item);
        return true;
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
    public void onResume() {
        super.onResume();
        if (mLive != null) getLive();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.live.removeObserver(mObserveLive);
        mViewModel.epg.removeObserver(mObserveEpg);
    }
}
