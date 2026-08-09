package com.fongmi.android.tv.ui.adapter;

import android.util.TypedValue;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.ItemSubscribeBinding;

import java.util.ArrayList;
import java.util.List;

public class SubscribeAdapter extends RecyclerView.Adapter<SubscribeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private List<Config> mItems;
    private String active;

    public SubscribeAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onSelect(Config item);

        void onLine(Config item);

        void onCustom(Config item);

        void onDelete(Config item);
    }

    public SubscribeAdapter addAll(List<Config> items, String active) {
        mItems = new ArrayList<>(items);
        this.active = active;
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSubscribeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = mItems.get(position);
        boolean checked = TextUtils.equals(active, item.getUrl());
        holder.binding.radio.setColorFilter(checked ? getColor(holder, R.attr.colorControlActivated) : getColor(holder, R.attr.colorControlNormal));
        holder.binding.radio.setAlpha(checked ? 1.0f : 0.3f);
        holder.binding.name.setText(item.getDesc());
        if (item.isCustom()) {
            holder.binding.url.setText(getString(holder, R.string.custom_site_list));
            holder.binding.source.setVisibility(View.GONE);
            holder.binding.delete.setVisibility(View.GONE);
            holder.binding.name.setOnClickListener(v -> mListener.onCustom(item));
            holder.binding.url.setOnClickListener(v -> mListener.onCustom(item));
        } else {
            if (item.isDepot()) {
                holder.binding.url.setText(getString(holder, R.string.subscribe_selected_line, item.getLineName()));
            } else {
                holder.binding.url.setText(item.getUrl());
            }
            if (!TextUtils.isEmpty(item.getSource())) {
                holder.binding.source.setText(getString(holder, R.string.subscribe_from, item.getSource()));
                holder.binding.source.setVisibility(View.VISIBLE);
            } else {
                holder.binding.source.setVisibility(View.GONE);
            }
            holder.binding.delete.setVisibility(View.VISIBLE);
            holder.binding.name.setOnClickListener(v -> onItemClick(item));
            holder.binding.url.setOnClickListener(v -> onItemClick(item));
        }
        holder.binding.radio.setOnClickListener(v -> mListener.onSelect(item));
        holder.binding.delete.setOnClickListener(v -> mListener.onDelete(item));
        holder.binding.divider.setVisibility(position == mItems.size() - 1 ? View.GONE : View.VISIBLE);
    }

    private void onItemClick(Config item) {
        if (item.isDepot()) mListener.onLine(item);
        else mListener.onSelect(item);
    }

    private int getColor(ViewHolder holder, int attr) {
        TypedValue value = new TypedValue();
        holder.itemView.getContext().getTheme().resolveAttribute(attr, value, true);
        return value.resourceId != 0 ? ContextCompat.getColor(holder.itemView.getContext(), value.resourceId) : value.data;
    }

    private String getString(ViewHolder holder, int resId, Object... args) {
        return holder.itemView.getContext().getString(resId, args);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSubscribeBinding binding;

        ViewHolder(@NonNull ItemSubscribeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
