package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentSourceBinding;
import com.fongmi.android.tv.ui.adapter.SourceChooseAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

// 播放源弹窗的分页内容：一个站点一页，内部是与改造前一致的两列源列表
public class SourceFragment extends BaseFragment implements SourceChooseAdapter.OnClickListener {

    private FragmentSourceBinding mBinding;
    private SourceChooseAdapter mAdapter;
    private SourceChooseAdapter.OnClickListener mListener;
    private List<Vod> mItems = new ArrayList<>();
    private Vod mSelected;
    private int mListHeight;

    public static SourceFragment newInstance(String site) {
        Bundle args = new Bundle();
        args.putString("site", site);
        SourceFragment fragment = new SourceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public String getSite() {
        return getArguments() == null ? "" : getArguments().getString("site", "");
    }

    // 点击回传给宿主：弹窗自身实现了 SourceChooseAdapter.OnClickListener
    public void setListener(SourceChooseAdapter.OnClickListener listener) {
        this.mListener = listener;
    }

    // 数据由弹窗推入：检索结果会持续增长，每页只负责渲染自己站点的数据
    public void apply(List<Vod> items, Vod selected) {
        this.mItems = items == null ? new ArrayList<>() : items;
        this.mSelected = selected;
        if (mBinding != null) refresh();
    }

    // 列表高度由弹窗统一下发（可用最大高度，超出一屏时内部滚动）：各页固定同高，切 tab 时弹窗高度不跳动
    public void setListHeight(int height) {
        this.mListHeight = height;
        if (mBinding != null) applyHeight();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSourceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        mBinding.recycler.setAdapter(mAdapter = new SourceChooseAdapter(this));
        applyHeight();
        refresh();
    }

    private void applyHeight() {
        if (mListHeight <= 0) return;
        ViewGroup.LayoutParams params = mBinding.recycler.getLayoutParams();
        if (params == null) {
            // 兜底：拿不到 LP 时至少用 maxHeight 限制，避免这一帧被内容撑高
            mBinding.recycler.setMaxHeight(mListHeight);
            return;
        }
        params.height = mListHeight;
        mBinding.recycler.setLayoutParams(params);
    }

    private void refresh() {
        mAdapter.addAll(mItems);
        mAdapter.setSelected(mSelected);
        // 当前正在播放的源在本页时滚动到它（对齐改造前"打开弹窗即定位到选中项"的行为）
        int position = mAdapter.getSelectedPosition();
        if (position >= 0) mBinding.recycler.scrollToPosition(position);
    }

    @Override
    public void onItemClick(Vod item) {
        if (mListener != null) mListener.onItemClick(item);
    }
}
