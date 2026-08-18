package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.SubtitleSearch;
import com.fongmi.android.tv.databinding.AdapterSubtitleSearchBinding;

import java.util.ArrayList;
import java.util.List;

public class SubtitleSearchAdapter extends RecyclerView.Adapter<SubtitleSearchAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<SubtitleSearch.Data> mItems;

    public SubtitleSearchAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(SubtitleSearch.Data item);
    }

    public void setItems(List<SubtitleSearch.Data> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSubtitleSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubtitleSearch.Data item = mItems.get(position);
        holder.binding.text.setText(item.toString());
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterSubtitleSearchBinding binding;

        public ViewHolder(@NonNull AdapterSubtitleSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }
}
