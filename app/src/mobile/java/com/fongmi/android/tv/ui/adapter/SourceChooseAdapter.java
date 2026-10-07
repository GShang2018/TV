package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterQuickBinding;

import java.util.ArrayList;
import java.util.List;

public class SourceChooseAdapter extends RecyclerView.Adapter<SourceChooseAdapter.ViewHolder> {

    private static final int NO_SELECTED = -1;

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private Vod mSelected;
    private int mSelectedPosition = NO_SELECTED;

    // 主题文字颜色缓存
    private int mColorOnSurface = -1;
    private int mColorOnSurfaceVariant = -1;
    private int mColorOnSurfaceVariantDim = -1;

    public SourceChooseAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    public void addAll(List<Vod> items) {
        mItems.clear();
        mItems.addAll(items);
        // 数据更新后按对象重新定位选中项（每页的下标只在页内有效）
        mSelectedPosition = indexOf(mSelected);
        notifyDataSetChanged();
    }

    /**
     * 设置当前正在播放的源。按站点 key + vodId 比对而不是外部下标：
     * 弹窗按站点分页后，各页的下标互不相干，且同一个 vodId 可能出现在多个站点上。
     */
    public void setSelected(Vod item) {
        this.mSelected = item;
        this.mSelectedPosition = indexOf(item);
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    private int indexOf(Vod item) {
        if (item == null) return NO_SELECTED;
        for (int i = 0; i < mItems.size(); i++) {
            Vod it = mItems.get(i);
            if (TextUtils.equals(it.getSiteKey(), item.getSiteKey()) && TextUtils.equals(it.getVodId(), item.getVodId())) return i;
        }
        return NO_SELECTED;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterQuickBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    private void resolveThemeColors(ViewHolder holder) {
        if (mColorOnSurface != -1) return;
        TypedValue tv = new TypedValue();
        holder.itemView.getContext().getTheme().resolveAttribute(R.attr.colorOnSurface, tv, true);
        mColorOnSurface = tv.data;
        holder.itemView.getContext().getTheme().resolveAttribute(R.attr.colorOnSurfaceVariant, tv, true);
        mColorOnSurfaceVariant = tv.data;
        // 二级字体色：在 colorOnSurfaceVariant 基础上进一步降低透明度
        int alpha = android.graphics.Color.alpha(mColorOnSurfaceVariant);
        int dimmedAlpha = (int) (alpha * 0.6f);
        mColorOnSurfaceVariantDim = (mColorOnSurfaceVariant & 0x00FFFFFF) | (dimmedAlpha << 24);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod item = mItems.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getVodRemarks()) ? View.GONE : View.VISIBLE);
        boolean isSelected = position == mSelectedPosition;
        // 背景交给 selector_item_bg 的 state_selected 自动切换：
        // 选中 = shape_item_selected（主题色 + padding 12/8/12/8 + 圆角），未选中 = shape_item
        holder.itemView.setSelected(isSelected);
        resolveThemeColors(holder);
        if (isSelected) {
            // 选中：主题色背景（由 selector 提供）+ 白色标题 + 淡白二级（参考视频播放页 white_70）
            holder.binding.name.setTextColor(android.graphics.Color.WHITE);
            holder.binding.site.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white_70));
            holder.binding.remark.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white_70));
        } else {
            holder.binding.name.setTextColor(mColorOnSurface);
            holder.binding.site.setTextColor(mColorOnSurfaceVariantDim);
            holder.binding.remark.setTextColor(mColorOnSurfaceVariantDim);
        }
        holder.binding.getRoot().setOnClickListener(v -> {
            int clickPosition = holder.getBindingAdapterPosition();
            if (clickPosition == RecyclerView.NO_POSITION) return;
            mSelected = item;
            mSelectedPosition = clickPosition;
            notifyDataSetChanged();
            mListener.onItemClick(item);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterQuickBinding binding;

        ViewHolder(@NonNull AdapterQuickBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
