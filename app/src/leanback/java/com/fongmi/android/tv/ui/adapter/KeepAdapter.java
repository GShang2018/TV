package com.fongmi.android.tv.ui.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class KeepAdapter extends RecyclerView.Adapter<KeepAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Keep> mItems;
    private int width, height;
    private boolean delete;
    private Style mStyle;

    public KeepAdapter(OnClickListener listener) {
        this(listener, Style.rect());
    }

    public KeepAdapter(OnClickListener listener, Style style) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        this.mStyle = style;
        setLayoutSize();
    }

    public void setStyle(Style style) {
        this.mStyle = style;
        setLayoutSize();
    }

    private void setLayoutSize() {
        int column = Product.getColumn(mStyle);
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (column - 1));
        int base = ResUtil.getScreenWidth() - space;
        width = base / column;
        height = (int) (width / mStyle.getRatio());
    }

    public interface OnClickListener {

        void onItemClick(Keep item);

        void onItemDelete(Keep item);

        boolean onLongClick();
    }

    public void addAll(List<Keep> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void delete(Keep item) {
        int index = mItems.indexOf(item);
        if (index == -1) return;
        mItems.remove(index);
        notifyItemRemoved(index);
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, mItems.size());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height + ResUtil.dp2px(32);
        holder.binding.image.getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Keep item = mItems.get(position);
        // 每次绑定都更新 LayoutParams，确保切换布局后封面大小正确
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height + ResUtil.dp2px(32);
        holder.binding.image.getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        setFocusListener(holder.binding);
        setClickListener(holder.itemView, item);
        holder.binding.name.setText(item.getVodName());
        holder.binding.remark.setVisibility(View.GONE);
        holder.binding.site.setVisibility(View.VISIBLE);
        holder.binding.site.setText(item.getSiteName());
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
        BaseVodHolder.setTagMaxWidth(holder.binding.image, 8, holder.binding.site);
    }

    private void setFocusListener(AdapterVodBinding binding) {
        binding.getRoot().setOnFocusChangeListener((v, hasFocus) -> {
            v.setSelected(hasFocus);
            binding.name.setSelected(hasFocus);
            if (hasFocus) {
                AnimatorSet animator = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 1.05f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 1.05f);
                animator.setDuration(200);
                animator.playTogether(scaleX, scaleY);
                animator.start();
                v.bringToFront();
            } else {
                AnimatorSet animator = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.05f, 1.0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.05f, 1.0f);
                animator.setDuration(200);
                animator.playTogether(scaleX, scaleY);
                animator.start();
            }
        });
    }

    private void setClickListener(View root, Keep item) {
        root.setOnLongClickListener(view -> mListener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        public ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
