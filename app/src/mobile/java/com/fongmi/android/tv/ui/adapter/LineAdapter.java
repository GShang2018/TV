package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterLineBinding;

import java.util.ArrayList;
import java.util.List;

public class LineAdapter extends RecyclerView.Adapter<LineAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<String> mItems;
    private int mSelected;

    public LineAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        this.mSelected = -1;
    }

    public interface OnClickListener {

        void onItemClick(int position);
    }

    public void addAll(List<String> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setSelected(int position) {
        mSelected = position;
        notifyItemRangeChanged(0, getItemCount());
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterLineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = mItems.get(position);
        holder.binding.text.setText(item);
        holder.binding.text.setSelected(position == mSelected);
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterLineBinding binding;

        ViewHolder(@NonNull AdapterLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
