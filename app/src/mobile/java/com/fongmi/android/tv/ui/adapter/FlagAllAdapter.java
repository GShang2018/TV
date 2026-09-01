package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.databinding.ItemFlagAllBinding;

import java.util.ArrayList;
import java.util.List;

// 线路"全部"弹窗专用：2 列网格中每个 chip 铺满整列宽度（match_parent），替代主列表默认的 wrap_content chip
public class FlagAllAdapter extends RecyclerView.Adapter<FlagAllAdapter.ViewHolder> {

    private final List<Flag> mItems;
    private final OnClickListener mListener;

    public FlagAllAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Flag item);
    }

    public void addAll(List<Flag> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    // 当前激活线路的位置，弹窗打开时自动滚动到位
    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemFlagAllBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Flag item = mItems.get(position);
        holder.binding.text.setText(item.getShow());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFlagAllBinding binding;

        ViewHolder(@NonNull ItemFlagAllBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
