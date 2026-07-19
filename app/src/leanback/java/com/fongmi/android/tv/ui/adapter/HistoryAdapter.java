package com.fongmi.android.tv.ui.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<History> mItems;
    private int width, height;
    private boolean delete;
    private Style mStyle;

    public HistoryAdapter(OnClickListener listener) {
        this(listener, Style.rect());
    }

    public HistoryAdapter(OnClickListener listener, Style style) {
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

        void onItemClick(History item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<History> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        setDelete(false);
        notifyDataSetChanged();
        History.delete(VodConfig.getCid());
    }

    public int delete(History item) {
        int index = mItems.indexOf(item);
        if (index == -1) return index;
        mItems.remove(index);
        notifyItemRemoved(index);
        return index;
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
        History item = mItems.get(position);
        // 每次绑定都更新 LayoutParams，确保切换布局后封面大小正确
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height + ResUtil.dp2px(32);
        holder.binding.image.getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        setFocusListener(holder.binding);
        setClickListener(holder.itemView, item);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        holder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
        ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
        BaseVodHolder.setTagMaxWidth(holder.binding.image, 8, holder.binding.site, holder.binding.remark);
    }

    private void setFocusListener(AdapterVodBinding binding) {
        binding.getRoot().setOnFocusChangeListener((v, hasFocus) -> {
            v.setSelected(hasFocus);
            binding.name.setSelected(hasFocus);
            binding.remark.setSelected(hasFocus);
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

    private void setClickListener(View root, History item) {
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
