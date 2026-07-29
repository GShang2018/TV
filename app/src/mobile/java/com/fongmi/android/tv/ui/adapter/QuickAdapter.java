package com.fongmi.android.tv.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterQuickBinding;

import java.util.ArrayList;
import java.util.List;

public class QuickAdapter extends RecyclerView.Adapter<QuickAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private int mSelectedPosition = -1;

    // 主题文字颜色缓存
    private int mColorOnSurface = -1;
    private int mColorOnSurfaceVariant = -1;
    private int mColorOnSurfaceVariantDim = -1;
    // 选中状态下白色背景上的文字颜色（固定值，避免反色逻辑导致颜色混淆）
    private final int mSelectedNameColor = 0xFF212121;
    private final int mSelectedVariantColor = 0xFF999999;

    public QuickAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    public void clear() {
        mItems.clear();
        mSelectedPosition = -1;
        notifyDataSetChanged();
    }

    public void addAll(List<Vod> items) {
        int position = mItems.size() + 1;
        mItems.addAll(items);
        notifyItemRangeInserted(position, items.size());
    }

    public Vod get(int position) {
        return mItems.get(position);
    }

    public void remove(int position) {
        mItems.remove(position);
        notifyItemRemoved(position);
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
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
        int alpha = Color.alpha(mColorOnSurfaceVariant);
        int dimmedAlpha = (int) (alpha * 0.6f);
        mColorOnSurfaceVariantDim = (mColorOnSurfaceVariant & 0x00FFFFFF) | (dimmedAlpha << 24);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod item = mItems.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        boolean isSelected = position == mSelectedPosition;
        holder.itemView.setSelected(isSelected);
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        if (isSelected) {
            resolveThemeColors(holder);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(Color.WHITE);
            drawable.setCornerRadius(4 * density);
            holder.itemView.setBackground(drawable);
            holder.itemView.setPadding((int)(12 * density), (int)(8 * density), (int)(12 * density), (int)(8 * density));
            // 白色背景上使用固定颜色，标题深色醒目，二级信息浅灰色
            holder.binding.name.setTextColor(mSelectedNameColor);
            holder.binding.site.setTextColor(mSelectedVariantColor);
            holder.binding.remark.setTextColor(mSelectedVariantColor);
        } else {
            holder.itemView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.selector_item_bg));
            holder.itemView.setPadding((int)(12 * density), (int)(8 * density), (int)(12 * density), (int)(8 * density));
            // 恢复原始文字颜色（主题属性）
            resolveThemeColors(holder);
            holder.binding.name.setTextColor(mColorOnSurface);
            holder.binding.site.setTextColor(mColorOnSurfaceVariantDim);
            holder.binding.remark.setTextColor(mColorOnSurfaceVariantDim);
        }
        holder.binding.getRoot().setOnClickListener(v -> {
            int previousSelected = mSelectedPosition;
            mSelectedPosition = holder.getAdapterPosition();
            if (previousSelected != -1) notifyItemChanged(previousSelected);
            if (mSelectedPosition != -1) notifyItemChanged(mSelectedPosition);
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
