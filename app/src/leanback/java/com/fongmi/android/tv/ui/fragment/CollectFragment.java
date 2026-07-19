package com.fongmi.android.tv.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.activity.VodActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class CollectFragment extends BaseFragment implements CustomScroller.Callback, VodPresenter.OnClickListener {

    private FragmentVodBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private Collect mCollect;
    private String mKeyword;
    private List<Vod> mSavedItems;

    public static CollectFragment newInstance(String keyword, Collect collect) {
        Bundle args = new Bundle();
        args.putString("keyword", keyword);
        CollectFragment fragment = new CollectFragment().setCollect(collect);
        fragment.setArguments(args);
        return fragment;
    }

    private String getKeyword() {
        return mKeyword = mKeyword == null ? getArguments().getString("keyword") : mKeyword;
    }

    private CollectFragment setCollect(Collect collect) {
        this.mCollect = collect;
        return this;
    }

    private Style getViewStyle() {
        return Setting.getCollectViewType() == ViewType.PORTRAIT ? new Style("rect", 0.75f) : Style.rect();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setHeader(getActivity().findViewById(R.id.result), getActivity().findViewById(R.id.recycler));
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, result -> {
            mScroller.endLoading(result);
            addVideo(result.getList());
        });
    }

    @Override
    protected void initData() {
        if (mCollect != null) addVideo(mCollect.getList());
    }

    private boolean checkLastSize(List<Vod> items) {
        if (mLast == null || items.size() == 0) return false;
        Style style = getViewStyle();
        int column = Product.getColumn(style);
        int size = column - mLast.size();
        if (size == 0) return false;
        size = Math.min(size, items.size());
        mLast.addAll(mLast.size(), new ArrayList<>(items.subList(0, size)));
        addVideo(new ArrayList<>(items.subList(size, items.size())));
        return true;
    }

    public void addVideo(List<Vod> items) {
        if (checkLastSize(items) || getActivity() == null || getActivity().isFinishing()) return;
        if (mSavedItems == null) mSavedItems = new ArrayList<>();
        mSavedItems.addAll(items);
        Style style = getViewStyle();
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            mLast = new ArrayObjectAdapter(new VodPresenter(this, style));
            mLast.setItems(part, null);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    public void refreshStyle() {
        if (mSavedItems == null || mSavedItems.isEmpty()) return;
        // 保存当前滚动位置和子项位置
        int position = mBinding.recycler.getSelectedPosition();
        int subPosition = 0;
        RecyclerView.ViewHolder rowHolder = mBinding.recycler.findViewHolderForAdapterPosition(position);
        if (rowHolder != null && rowHolder.itemView instanceof ViewGroup) {
            ViewGroup rowGroup = (ViewGroup) rowHolder.itemView;
            for (int i = 0; i < rowGroup.getChildCount(); i++) {
                View child = rowGroup.getChildAt(i);
                if (child instanceof RecyclerView) {
                    RecyclerView horizontalGrid = (RecyclerView) child;
                    View focusedChild = horizontalGrid.getFocusedChild();
                    if (focusedChild != null) {
                        subPosition = horizontalGrid.getChildAdapterPosition(focusedChild);
                    } else {
                        subPosition = mBinding.recycler.getSelectedSubPosition();
                    }
                    break;
                }
            }
        }
        mAdapter.clear();
        mLast = null;
        Style style = getViewStyle();
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(mSavedItems, Product.getColumn(style))) {
            mLast = new ArrayObjectAdapter(new VodPresenter(this, style));
            mLast.setItems(part, null);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(0, rows);
        if (position < mAdapter.size()) {
            mBinding.recycler.setSelectedPosition(position);
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
        getActivity().setResult(Activity.RESULT_OK);
        if (item.isFolder()) VodActivity.start(getActivity(), item.getSiteKey(), Result.folder(item));
        else VideoActivity.collect(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }

    @Override
    public void onLoadMore(String page) {
        if (mCollect == null || "all".equals(mCollect.getSite().getKey())) return;
        mViewModel.searchContent(mCollect.getSite(), getKeyword(), page);
        mScroller.setLoading(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
