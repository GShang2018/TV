package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterTypeBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupTabAdapter extends RecyclerView.Adapter<GroupTabAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Group> mItems;

    public GroupTabAdapter(OnClickListener listener) {
        this(listener, false);
    }

    public GroupTabAdapter(OnClickListener listener, boolean dialogMode) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Group item);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Group> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public Group get(int position) {
        return mItems.get(position);
    }

    public List<Group> getItems() {
        return mItems;
    }

    public int indexOf(Group group) {
        return mItems.indexOf(group);
    }

    public void setSelected(Group group) {
        setSelected(indexOf(group));
    }

    public void setSelected(int position) {
        if (position == -1) return;
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
        return new ViewHolder(AdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterTypeBinding binding;

        ViewHolder(@NonNull AdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
