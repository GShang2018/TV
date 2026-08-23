package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.AdapterChannelGridBinding;
import com.fongmi.android.tv.databinding.AdapterChannelListBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class ChannelGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Channel> mItems;
    private int[] size;
    private int type = ViewType.GRID;

    public ChannelGridAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Channel item);

        boolean onLongClick(Channel item);
    }

    public ChannelGridAdapter size(int[] size) {
        this.size = size;
        return this;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Channel> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void changed(Channel item) {
        int position = mItems.indexOf(item);
        if (position == -1) return;
        notifyItemChanged(position);
    }

    public void setSelected(Channel item) {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelected(item.equals(mItems.get(i)));
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.LIST) {
            return new ListHolder(AdapterChannelListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        return new GridHolder(AdapterChannelGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Channel item = mItems.get(position);
        if (holder instanceof ListHolder) bindList((ListHolder) holder, item);
        else bindGrid((GridHolder) holder, item);
    }

    private void bindGrid(GridHolder holder, Channel item) {
        if (size != null) {
            holder.binding.image.getLayoutParams().width = size[0];
            holder.binding.image.getLayoutParams().height = size[1];
            holder.binding.remark.setMaxWidth(size[0] - ResUtil.dp2px(12));
        }
        holder.binding.name.setText(item.getName());
        holder.binding.number.setText(item.getNumber());
        holder.binding.number.setVisibility(item.getNumber().isEmpty() ? View.GONE : View.VISIBLE);
        String remark = item.getData().getEpg();
        holder.binding.remark.setText(remark);
        holder.binding.remark.setVisibility(remark.isEmpty() ? View.GONE : View.VISIBLE);
        ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.image);
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(v -> mListener.onLongClick(item));
    }

    private void bindList(ListHolder holder, Channel item) {
        holder.binding.name.setText(item.getName());
        holder.binding.number.setText(item.getNumber());
        holder.binding.number.setVisibility(item.getNumber().isEmpty() ? View.GONE : View.VISIBLE);
        String remark = item.getData().getEpg();
        holder.binding.remark.setText(remark);
        holder.binding.remark.setVisibility(remark.isEmpty() ? View.GONE : View.VISIBLE);
        ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.image);
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(v -> mListener.onLongClick(item));
    }

    static class GridHolder extends RecyclerView.ViewHolder {

        private final AdapterChannelGridBinding binding;

        GridHolder(@NonNull AdapterChannelGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ListHolder extends RecyclerView.ViewHolder {

        private final AdapterChannelListBinding binding;

        ListHolder(@NonNull AdapterChannelListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
