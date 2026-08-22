package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.AdapterChannelChooseBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class ChannelChooseAdapter extends RecyclerView.Adapter<ChannelChooseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Channel> mItems;

    public ChannelChooseAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Channel item);
    }

    public void addAll(List<Channel> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setActivated(int position) {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelected(i == position);
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterChannelChooseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel item = mItems.get(position);
        ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.logo);
        holder.binding.name.setText(item.getName());
        holder.binding.number.setText(item.getNumber());
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterChannelChooseBinding binding;

        ViewHolder(@NonNull AdapterChannelChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
