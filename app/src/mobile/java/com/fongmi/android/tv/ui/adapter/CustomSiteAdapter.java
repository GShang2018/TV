package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.databinding.ItemCustomSiteBinding;

import java.util.ArrayList;
import java.util.List;

public class CustomSiteAdapter extends RecyclerView.Adapter<CustomSiteAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private List<CustomSite> mItems;

    public CustomSiteAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onEditClick(CustomSite item);

        void onDeleteClick(CustomSite item);
    }

    public CustomSiteAdapter addAll(List<CustomSite> items) {
        mItems = new ArrayList<>(items);
        return this;
    }

    public int remove(CustomSite item) {
        mItems.remove(item);
        notifyDataSetChanged();
        return getItemCount();
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCustomSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomSite item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.api.setText(item.getApi());
        holder.binding.edit.setOnClickListener(v -> mListener.onEditClick(item));
        holder.binding.delete.setOnClickListener(v -> mListener.onDeleteClick(item));
        holder.binding.divider.setVisibility(position == mItems.size() - 1 ? View.GONE : View.VISIBLE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCustomSiteBinding binding;

        ViewHolder(@NonNull ItemCustomSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
