package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.databinding.ItemLineSelectBinding;

import java.util.ArrayList;
import java.util.List;

public class LineSelectAdapter extends RecyclerView.Adapter<LineSelectAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private List<Depot> mItems;
    private String selected;

    public LineSelectAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onLineClick(Depot item);
    }

    public LineSelectAdapter addAll(List<Depot> items, String selected) {
        mItems = new ArrayList<>(items);
        this.selected = selected;
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
        return new ViewHolder(ItemLineSelectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Depot item = mItems.get(position);
        boolean checked = TextUtils.equals(selected, item.getUrl());
        holder.binding.root.setBackgroundResource(checked ? R.drawable.shape_item_border_selected : 0);
        holder.binding.check.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.binding.name.setText(item.getName());
        holder.binding.url.setText(item.getUrl());
        holder.binding.root.setOnClickListener(v -> mListener.onLineClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLineSelectBinding binding;

        ViewHolder(@NonNull ItemLineSelectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
