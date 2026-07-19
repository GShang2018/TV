package com.fongmi.android.tv.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Page;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.FilterPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Prefers;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VodFragment extends BaseFragment implements CustomScroller.Callback, VodPresenter.OnClickListener {

    private HashMap<String, String> mExtends;
    private FragmentVodBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private List<Filter> mFilters;
    private List<Page> mPages;
    private boolean mOpen;
    private Page mPage;
    private Style mStyle;
    private Result mResult;

    public static VodFragment newInstance(String key, String typeId, Style style, HashMap<String, String> extend, boolean folder) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
        args.putParcelable("style", style);
        args.putSerializable("extend", extend);
        VodFragment fragment = new VodFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKey() {
        return getArguments().getString("key");
    }

    private String getTypeId() {
        return mPages.isEmpty() ? getArguments().getString("typeId") : getLastPage().getVodId();
    }

    private List<Filter> getFilter() {
        return Filter.arrayFrom(Prefers.getString("filter_" + getKey() + "_" + getTypeId()));
    }

    private HashMap<String, String> getExtend() {
        Serializable extend = getArguments().getSerializable("extend");
        return extend == null ? new HashMap<>() : (HashMap<String, String>) extend;
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private boolean isIndexs() {
        return getSite().isIndexs();
    }

    private Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    private Style getStyle() {
        return isFolder() ? Style.list() : getSite().getStyle(mPages.isEmpty() ? mStyle : getLastPage().getStyle());
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mPages = new ArrayList<>();
        mExtends = getExtend();
        mFilters = getFilter();
        mStyle = getArguments().getParcelable("style");
        setRecyclerView();
        setViewModel();
        setFilters();
    }

    @Override
    protected void initData() {
        getVideo();
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8, FocusHighlight.ZOOM_FACTOR_NONE, HorizontalGridView.FOCUS_SCROLL_ALIGNED), FilterPresenter.class);
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setHeader(getActivity().findViewById(R.id.recycler));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            boolean first = mScroller.first();
            int size = result.getList().size();
            if (size > 0) addVideo(result);
            mScroller.endLoading(result);
            checkPosition(first);
            checkMore(size);
            hideProgress();
        });
    }

    private void setFilters() {
        for (Filter filter : mFilters) {
            if (mExtends.containsKey(filter.getKey())) {
                filter.setActivated(mExtends.get(filter.getKey()));
            }
        }
    }

    private void setClick(ArrayObjectAdapter adapter, String key, Value item) {
        for (int i = 0; i < adapter.size(); i++) ((Value) adapter.get(i)).setActivated(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        if (item.isActivated()) mExtends.put(key, item.getV());
        else mExtends.remove(key);
        onRefresh();
    }

    private void getVideo() {
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void getVideo(String typeId, String page) {
        boolean first = "1".equals(page);
        if (first) mLast = null;
        if (first) showProgress();
        int filterSize = mOpen ? mFilters.size() : 0;
        boolean clear = first && mAdapter.size() > filterSize;
        if (clear) mAdapter.removeItems(filterSize, mAdapter.size() - filterSize);
        mViewModel.categoryContent(getKey(), typeId, page, true, mExtends);
    }

    private void addVideo(Result result) {
        mResult = result;
        // 始终使用用户设置的布局样式，忽略 API 返回数据自带的 style
        Style style = Setting.getCategoryViewType() == com.fongmi.android.tv.ui.base.ViewType.PORTRAIT ? new Style("rect", 0.75f) : Style.rect();
        if (style.isList()) mAdapter.addAll(mAdapter.size(), result.getList());
        else addGrid(result.getList(), style);
    }

    private void checkPosition(boolean first) {
        if (mPage != null && mPage.getPosition() > 0) mBinding.recycler.hideHeader();
        if (mPage != null && mPage.getPosition() < 1) mBinding.recycler.showHeader();
        if (mPage != null) mBinding.recycler.setSelectedPosition(mPage.getPosition());
        else if (first && !mOpen) mBinding.recycler.moveToTop();
        mPage = null;
    }

    private void checkMore(int count) {
        if (mScroller.isDisable() || count == 0 || mAdapter.size() >= 5) return;
        getVideo(getTypeId(), String.valueOf(mScroller.addPage()));
    }

    private boolean checkLastSize(List<Vod> items, Style style) {
        if (mLast == null || items.size() == 0) return false;
        int size = Product.getColumn(style) - mLast.size();
        if (size == 0) return false;
        size = Math.min(size, items.size());
        mLast.addAll(mLast.size(), new ArrayList<>(items.subList(0, size)));
        addGrid(new ArrayList<>(items.subList(size, items.size())), style);
        return true;
    }

    private void addGrid(List<Vod> items, Style style) {
        if (checkLastSize(items, style)) return;
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            mLast = new ArrayObjectAdapter(new VodPresenter(this, style));
            mLast.setItems(part, null);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private ListRow getRow(Filter filter) {
        FilterPresenter presenter = new FilterPresenter(filter.getKey());
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        presenter.setOnClickListener((key, item) -> setClick(adapter, key, item));
        adapter.setItems(filter.getValue(), null);
        return new ListRow(adapter);
    }

    private void showProgress() {
        if (!mOpen) mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    private void showFilter() {
        List<ListRow> rows = new ArrayList<>();
        for (Filter filter : mFilters) rows.add(getRow(filter));
        App.post(() -> mBinding.recycler.scrollToPosition(0), 48);
        mAdapter.addAll(0, rows);
        hideProgress();
    }

    private void hideFilter() {
        mAdapter.removeItems(0, mFilters.size());
    }

    public void toggleFilter(boolean open) {
        if (open) showFilter();
        else hideFilter();
        mOpen = open;
    }

    public void onRefresh() {
        getVideo();
    }

    public boolean canBack() {
        return !mPages.isEmpty();
    }

    public void goBack() {
        if (mPages.size() == 1) mBinding.recycler.setMoveTop(true);
        mPages.remove(mPage = getLastPage());
        onRefresh();
    }

    public boolean goRoot() {
        if (mPages.isEmpty()) return false;
        mPages.clear();
        getVideo();
        return true;
    }

    public void refreshStyle() {
        // 分类页使用独立的布局设置
        if (Setting.getCategoryViewType() == com.fongmi.android.tv.ui.base.ViewType.PORTRAIT) {
            mStyle = new Style("rect", 0.75f);
        } else {
            mStyle = Style.rect();
        }
        // 如果没有数据，触发网络加载
        if (mResult == null || mResult.getList().isEmpty()) {
            onRefresh();
            return;
        }
        // 保存当前滚动位置和子项位置
        int position = mBinding.recycler.getSelectedPosition();
        int subPosition = 0;
        RecyclerView.ViewHolder rowHolder = mBinding.recycler.findViewHolderForAdapterPosition(position);
        if (rowHolder != null && rowHolder.itemView instanceof ViewGroup) {
            ViewGroup rowGroup = (ViewGroup) rowHolder.itemView;
            // ListRowPresenter 的布局中，HorizontalGridView 是第一个 RecyclerView
            for (int i = 0; i < rowGroup.getChildCount(); i++) {
                View child = rowGroup.getChildAt(i);
                if (child instanceof RecyclerView) {
                    RecyclerView horizontalGrid = (RecyclerView) child;
                    View focusedChild = horizontalGrid.getFocusedChild();
                    if (focusedChild != null) {
                        subPosition = horizontalGrid.getChildAdapterPosition(focusedChild);
                    } else {
                        subPosition = ((androidx.leanback.widget.VerticalGridView) mBinding.recycler).getSelectedSubPosition();
                    }
                    break;
                }
            }
        }
        // 获取 filter 行数
        int filterSize = mOpen ? mFilters.size() : 0;
        // 移除所有非 filter 的行（vod 数据行）
        if (mAdapter.size() > filterSize) {
            mAdapter.removeItems(filterSize, mAdapter.size() - filterSize);
        }
        // 使用 mResult 中的数据重新添加 vod 行，使用新的 style
        Style style = mResult.getStyle(mStyle);
        if (style.isList()) {
            mAdapter.addAll(mAdapter.size(), mResult.getList());
        } else {
            mLast = null;
            for (List<Vod> part : Lists.partition(mResult.getList(), Product.getColumn(style))) {
                mLast = new ArrayObjectAdapter(new VodPresenter(this, style));
                mLast.setItems(part, null);
                mAdapter.add(new ListRow(mLast));
            }
        }
        // 强制 ItemBridgeAdapter 刷新所有行
        mBinding.recycler.swapAdapter(mBinding.recycler.getAdapter(), true);
        // 恢复滚动位置和焦点
        if (position < mAdapter.size()) {
            mBinding.recycler.setSelectedPosition(position);
            mBinding.recycler.requestFocus();
        }
        mBinding.recycler.requestLayout();
        // 延迟恢复子项焦点（等待布局完成）
        final int finalPosition = position;
        final int finalSubPosition = subPosition;
        App.post(() -> {
            if (finalPosition < mAdapter.size()) {
                RecyclerView.ViewHolder vh = mBinding.recycler.findViewHolderForAdapterPosition(finalPosition);
                if (vh != null && vh.itemView instanceof ViewGroup) {
                    ViewGroup rowGroup = (ViewGroup) vh.itemView;
                    for (int i = 0; i < rowGroup.getChildCount(); i++) {
                        View child = rowGroup.getChildAt(i);
                        if (child instanceof RecyclerView) {
                            RecyclerView horizontalGrid = (RecyclerView) child;
                            if (finalSubPosition >= 0 && finalSubPosition < horizontalGrid.getAdapter().getItemCount()) {
                                horizontalGrid.getLayoutManager().scrollToPosition(finalSubPosition);
                                View itemView = horizontalGrid.getLayoutManager().findViewByPosition(finalSubPosition);
                                if (itemView != null) {
                                    itemView.requestFocus();
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }, 150);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) {
            mViewModel.action(getKey(), item.getAction());
        } else if (item.isFolder()) {
            mPages.add(Page.get(item, mBinding.recycler.getSelectedPosition()));
            mBinding.recycler.setMoveTop(false);
            getVideo(item.getVodId(), "1");
        } else {
            if (isIndexs()) CollectActivity.start(getActivity(), item.getVodName());
            else if (!isFolder()) VideoActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName(), item.getVodPic());
            else VideoActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName(), item.getVodPic(), item.getVodName());
        }
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

    @Override
    public void onLoadMore(String page) {
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
