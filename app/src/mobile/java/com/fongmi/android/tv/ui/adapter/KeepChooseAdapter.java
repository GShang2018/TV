package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.databinding.AdapterKeepChooseBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeepChooseAdapter extends RecyclerView.Adapter<KeepChooseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<KeepFolder> mItems;
    private final Set<Integer> mSelected;

    public KeepChooseAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mSelected = new HashSet<>();
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(int position);
    }

    public void addAll(List<KeepFolder> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setSelected(int position, boolean selected) {
        if (selected) mSelected.add(position);
        else mSelected.remove(position);
        notifyItemChanged(position);
    }

    public void setSelected(List<Integer> folderIds) {
        mSelected.clear();
        for (int i = 0; i < mItems.size(); i++) {
            if (folderIds.contains(mItems.get(i).getId())) mSelected.add(i);
        }
        notifyDataSetChanged();
    }

    public boolean isSelected(int position) {
        return mSelected.contains(position);
    }

    public List<Integer> getSelectedFolderIds() {
        List<Integer> ids = new ArrayList<>();
        for (int position : mSelected) {
            if (position >= 0 && position < mItems.size()) ids.add(mItems.get(position).getId());
        }
        return ids;
    }

    public List<KeepFolder> getSelectedItems() {
        List<KeepFolder> list = new ArrayList<>();
        for (int position : mSelected) {
            if (position >= 0 && position < mItems.size()) list.add(mItems.get(position));
        }
        return list;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterKeepChooseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeepFolder item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.count.setText(holder.itemView.getContext().getString(R.string.keep_folder_count, item.getCount()));
        holder.binding.check.setChecked(mSelected.contains(position));
        holder.binding.check.setClickable(false);
        holder.binding.check.setFocusable(false);
        holder.binding.check.setFocusableInTouchMode(false);
        holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterKeepChooseBinding binding;

        ViewHolder(@NonNull AdapterKeepChooseBinding binding, OnClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
