package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterLineChooseBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LineChooseAdapter extends RecyclerView.Adapter<LineChooseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<String> mItems;
    private int selected = -1;

    public LineChooseAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(int position);
    }

    public void addAll(List<String> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setActivated(int position) {
        this.selected = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterLineChooseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.name.setText(mItems.get(position));
        holder.binding.number.setText(String.format(Locale.getDefault(), "%02d", position + 1));
        holder.binding.getRoot().setSelected(position == selected);
        // 使用 bindingAdapterPosition，避免刷新后位置错位
        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) mListener.onItemClick(pos);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterLineChooseBinding binding;

        ViewHolder(@NonNull AdapterLineChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
