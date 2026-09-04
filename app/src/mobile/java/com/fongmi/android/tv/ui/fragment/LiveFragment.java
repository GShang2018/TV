package com.fongmi.android.tv.ui.fragment;

import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.SparseArray;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.FragmentLiveBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.LiveHistoryActivity;
import com.fongmi.android.tv.ui.activity.LiveKeepActivity;
import com.fongmi.android.tv.ui.activity.SubscriptionActivity;
import com.fongmi.android.tv.ui.adapter.ChannelGridAdapter;
import com.fongmi.android.tv.ui.adapter.GroupTabAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;
import com.fongmi.android.tv.ui.dialog.GroupDialog;
import com.fongmi.android.tv.ui.dialog.LineSelectDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class LiveFragment extends BaseFragment implements LiveCallback, GroupTabAdapter.OnClickListener, ChannelGridAdapter.OnClickListener {

    private FragmentLiveBinding mBinding;
    private GroupTabAdapter mGroupAdapter;
    private Observer<Live> mObserveLive;
    private Observer<Epg> mObserveEpg;
    private Observer<Boolean> mObserveXml;
    private LiveViewModel mViewModel;
    private SparseArray<ChannelGridAdapter> mAdapters;
    private SparseArray<RecyclerView> mViews;
    private List<Group> mHides;
    private Live mLive;
    private Group mGroup;
    private boolean mChecked;

    public static LiveFragment newInstance() {
        return new LiveFragment();
    }

    private Live getHome() {
        return LiveConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentLiveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setRecyclerView();
        setViewModel();
        setSiteText();
        showProgress();
        checkLive();
    }

    @Override
    protected void initEvent() {
        mBinding.logo.setOnClickListener(this::onLogo);
        // 与点播一致：logo 长按刷新配置
        mBinding.logo.setOnLongClickListener(this::onRefresh);
        mBinding.siteView.setOnClickListener(this::onSite);
        mBinding.keep.setOnClickListener(this::onKeep);
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.history.setOnClickListener(this::onHistory);
        mBinding.typeMore.setOnClickListener(this::onTypeMore);
        mBinding.addSubscribe.setOnClickListener(v -> SubscriptionActivity.start(requireActivity(), 1, false));
    }

    // logo 长按刷新（与点播 onRefresh 一致）：清缓存后重新拉取直播配置，收藏组也会按数据库重建
    private boolean onRefresh(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                LiveConfig.get().init().load(new Callback() {
                    @Override
                    public void success() {
                        RefreshEvent.live();
                        Notify.show(R.string.config_refreshed);
                    }

                    @Override
                    public void error(String msg) {
                        Notify.show(msg);
                    }
                });
            }
        });
        return true;
    }

    // 顶栏收藏按钮：进入直播收藏页（参考点播首页收藏入口）
    private void onKeep(View view) {
        LiveKeepActivity.start(getActivity());
    }

    // 顶栏历史按钮：进入直播历史页（参考点播首页历史入口）
    private void onHistory(View view) {
        LiveHistoryActivity.start(getActivity());
    }

    private void setSiteText() {
        String site = getHome().getName();
        if (site.isEmpty()) site = LiveConfig.get().getConfig().getDesc();
        mBinding.site.setText(site.isEmpty() ? getString(R.string.live_source) : site);
        loadLogo();
        // 名称就绪后：胶囊由仅 logo 的圆形横向展开出名称
        expandSiteView();
    }

    private void expandSiteView() {
        if (!Setting.isHomeDisplayName()) {
            mBinding.siteView.setVisibility(View.GONE);
            return;
        }
        if (mBinding.site.getText().length() == 0) return;
        final View siteView = mBinding.siteView;
        siteView.setVisibility(View.VISIBLE);
        final ViewGroup.LayoutParams params = siteView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        siteView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int target = Math.max(siteView.getMeasuredWidth(), 1);
        params.width = 0;
        siteView.requestLayout();
        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(280);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            params.width = (Integer) animation.getAnimatedValue();
            siteView.setAlpha(0.2f + 0.8f * animation.getAnimatedFraction());
            siteView.requestLayout();
        });
        animator.start();
    }

    private void loadLogo() {
        String logo = LiveConfig.get().getConfig().getLogo();
        if (logo == null || logo.isEmpty()) return;
        Glide.with(this).load(UrlUtil.convert(logo)).error(R.drawable.ic_logo).into(mBinding.logo);
    }

    private void setRecyclerView() {
        mAdapters = new SparseArray<>();
        mViews = new SparseArray<>();
        // 分类标签展示改由原生 TabLayout 承担（可横向滑动、指示器随页面联动）；
        // GroupTabAdapter 保留为分组数据源（GroupDialog 弹窗等仍在使用）
        mGroupAdapter = new GroupTabAdapter(this);
        mBinding.pager.setAdapter(new ChannelPagerAdapter());
        mBinding.type.setupWithViewPager(mBinding.pager);
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                selectGroup(position);
            }
        });
        setGrid();
    }

    private void setGrid() {
        int viewType = Setting.getLiveViewType();
        for (int i = 0; i < mViews.size(); i++) {
            int position = mViews.keyAt(i);
            RecyclerView recycler = mViews.get(position);
            ChannelGridAdapter adapter = mAdapters.get(position);
            if (recycler == null || adapter == null) continue;
            adapter.setType(viewType);
            setLayoutManager(recycler, adapter, viewType);
            adapter.notifyDataSetChanged();
        }
    }

    private void setLayoutManager(RecyclerView recycler, ChannelGridAdapter adapter, int viewType) {
        // 与点播首页完全一致：横版用 land、竖版用 rect、列表用 list，统一走 Product 尺寸计算
        Style style = viewType == ViewType.PORTRAIT ? Style.rect() : viewType == ViewType.LIST ? Style.list() : Style.land();
        recycler.setLayoutManager(new GridLayoutManager(getContext(), Product.getColumn(requireContext(), style)));
        if (viewType != ViewType.LIST && adapter != null) adapter.size(Product.getSpec(requireContext(), style));
    }

    private void toggleView(View view) {
        ViewTypeMenu.show(requireContext(), view, R.menu.menu_view_type_live, Setting.getLiveViewType(), viewType -> {
            Setting.putLiveViewType(viewType);
            setGrid();
        });
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mObserveLive = this::onLive;
        mObserveEpg = this::setEpg;
        mObserveXml = this::setEpg;
        mViewModel.live.observeForever(mObserveLive);
        mViewModel.epg.observeForever(mObserveEpg);
        mViewModel.xml.observeForever(mObserveXml);
    }

    private void checkLive() {
        if (LiveConfig.isEmpty()) LiveConfig.get().init().load(getCallback());
        else getLive();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                mChecked = true;
                getLive();
            }

            @Override
            public void error(String msg) {
                mChecked = true;
                hideProgress();
                // 无直播订阅时展示"暂无订阅 + 添加订阅"引导
                if (LiveConfig.hasUrl()) {
                    if (!TextUtils.isEmpty(msg)) Notify.show(msg);
                } else {
                    showEmpty();
                }
            }
        };
    }

    private void getLive() {
        showProgress();
        hideEmpty();
        // 切换订阅/源时先清空旧数据，避免旧分组残留导致内容不同步（与点播 homeContent 一致）
        mGroupAdapter.clear();
        mAdapters.clear();
        mViews.clear();
        if (mBinding.pager.getAdapter() != null) mBinding.pager.getAdapter().notifyDataSetChanged();
        mViewModel.getLive(getHome());
    }

    private void onLive(Live live) {
        // 加载失败/超时时 live 为空对象：也要隐藏进度并清空列表，避免进度条卡住、分类区域空白
        mLive = live;
        hideProgress();
        mChecked = true;
        if (live.isEmpty()) {
            mGroupAdapter.clear();
            mAdapters.clear();
            mViews.clear();
            if (mBinding.pager.getAdapter() != null) mBinding.pager.getAdapter().notifyDataSetChanged();
            showEmpty();
            return;
        }
        hideEmpty();
        // XML 节目单与播放页一致：live 加载后触发解析，数据写入各频道后由 setEpg(boolean) 刷新列表
        mViewModel.getXml(live);
        setGroup(live);
        setSiteText();
    }

    private void setGroup(Live live) {
        List<Group> items = new ArrayList<>();
        mHides = new ArrayList<>();
        for (Group group : live.getGroups()) (group.isHidden() ? mHides : items).add(group);
        mGroupAdapter.addAll(items);
        mAdapters.clear();
        mViews.clear();
        if (mBinding.pager.getAdapter() != null) mBinding.pager.getAdapter().notifyDataSetChanged();
        setPosition(LiveConfig.get().find(items));
        mBinding.typeLayout.post(this::checkTypeOverflow);
    }

    private void setPosition(int[] position) {
        if (position[0] == -1 || mGroupAdapter.getItemCount() == 0) return;
        int size = mGroupAdapter.getItemCount();
        if (position[0] >= size) position[0] = size - 1;
        mGroup = mGroupAdapter.get(position[0]);
        mGroup.setPosition(position[1]);
        mBinding.pager.setCurrentItem(position[0], false);
        selectGroup(position[0]);
    }

    private void selectGroup(int position) {
        if (position < 0 || position >= mGroupAdapter.getItemCount()) return;
        mGroup = mGroupAdapter.get(position);
        // 标签选中与滚动交由 TabLayout 托管，这里仅维护分组数据与频道列表选中态
        ChannelGridAdapter adapter = mAdapters.get(position);
        if (adapter != null && mGroup.getPosition() >= 0 && mGroup.getPosition() < mGroup.getChannel().size()) {
            adapter.setSelected(mGroup.getChannel().get(mGroup.getPosition()));
        }
        mViewModel.getEpgList(mGroup.getChannel());
        updateEmpty();
    }

    private void checkTypeOverflow() {
        // TabLayout 继承 HorizontalScrollView，可向左或向右滚动即说明标签溢出
        boolean overflow = mBinding.type.getWidth() > 0 && (mBinding.type.canScrollHorizontally(1) || mBinding.type.canScrollHorizontally(-1));
        mBinding.typeMore.setVisibility(overflow ? View.VISIBLE : View.GONE);
    }

    private void onTypeMore(View view) {
        GroupDialog.create(mGroupAdapter.getItems(), mBinding.pager.getCurrentItem(), this).show(getChildFragmentManager(), "groupDialog");
    }

    private void updateEmpty() {
        if (mGroup == null) return;
        // 有分组说明已有订阅，隐藏"暂无订阅"引导
        mBinding.emptyState.setVisibility(View.GONE);
        mBinding.empty.setVisibility(mGroup.getChannel().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showGroup(Group item) {
        int position = mGroupAdapter.indexOf(item);
        if (position == -1) return;
        if (mBinding.pager.getCurrentItem() != position) mBinding.pager.setCurrentItem(position, true);
        else selectGroup(position);
    }

    // XML 节目单为异步解析，完成后刷新已创建的频道页，确保每个频道显示各自当前节目
    private void setEpg(boolean success) {
        if (!success) return;
        for (int i = 0; i < mAdapters.size(); i++) mAdapters.valueAt(i).notifyDataSetChanged();
    }

    private void setEpg(Epg epg) {
        if (mGroup == null) return;
        ChannelGridAdapter adapter = mAdapters.get(mBinding.pager.getCurrentItem());
        if (adapter == null) return;
        for (Channel item : mGroup.getChannel()) {
            if (item.getTvgName().equals(epg.getKey())) {
                // 数据已写入 Channel，整体刷新当前页条目，确保节目信息显示（单条 changed 在首载时序下可能丢失）
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    private void onLogo(View view) {
        LineSelectDialog.createLiveAll(this).show(this);
    }

    private void onSite(View view) {
        LiveDialog.create().show(this);
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
    public void setLive(Live item) {
        if (item.isActivated()) item.getGroups().clear();
        LiveConfig.get().setHome(item);
        // 与点播 setSite 一致：先同步站点名再重新加载内容
        setSiteText();
        getLive();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setGrid();
        mBinding.typeLayout.post(this::checkTypeOverflow);
    }

    private void showProgress() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    private void showEmpty() {
        mBinding.emptyState.setVisibility(View.VISIBLE);
        mBinding.empty.setVisibility(View.GONE);
    }

    private void hideEmpty() {
        mBinding.emptyState.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 订阅/线路切换均通过 RefreshEvent 同步刷新；此处仅兜底无订阅场景（覆盖刚添加订阅返回）
        if (mChecked && LiveConfig.isEmpty()) checkLive();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case LIVE:
            case VIDEO:
                getLive();
                break;
            case CONFIG:
                setSiteText();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.live.removeObserver(mObserveLive);
        mViewModel.epg.removeObserver(mObserveEpg);
        mViewModel.xml.removeObserver(mObserveXml);
    }

    class ChannelPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mGroupAdapter == null ? 0 : mGroupAdapter.getItemCount();
        }

        // TabLayout 标签文本取自分组名，数据变化 notify 后标签自动重建
        @Override
        public CharSequence getPageTitle(int position) {
            return mGroupAdapter.get(position).getName();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Group group = mGroupAdapter.get(position);
            RecyclerView recycler = new RecyclerView(container.getContext());
            recycler.setHasFixedSize(true);
            recycler.setItemAnimator(null);
            recycler.setClipToPadding(false);
            int padding = ResUtil.dp2px(8);
            recycler.setPadding(padding, padding, padding, padding);
            ChannelGridAdapter adapter = new ChannelGridAdapter(LiveFragment.this);
            // 新页面必须设置当前视图类型，否则默认 GRID 导致列表模式下仍用宫格条目渲染
            adapter.setType(Setting.getLiveViewType());
            adapter.addAll(group.getChannel());
            if (group.getPosition() >= 0 && group.getPosition() < group.getChannel().size()) {
                adapter.setSelected(group.getChannel().get(group.getPosition()));
            }
            setLayoutManager(recycler, adapter, Setting.getLiveViewType());
            recycler.setAdapter(adapter);
            mAdapters.put(position, adapter);
            mViews.put(position, recycler);
            container.addView(recycler);
            return recycler;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
            mAdapters.remove(position);
            mViews.remove(position);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }
    }
}