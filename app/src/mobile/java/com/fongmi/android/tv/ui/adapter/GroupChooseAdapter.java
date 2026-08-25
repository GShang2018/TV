package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterGroupChooseBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupChooseAdapter extends RecyclerView.Adapter<GroupChooseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Group> mItems;

    public GroupChooseAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Group item);
    }

    public void addAll(List<Group> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setActivated(Group group) {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelected(mItems.get(i).equals(group));
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterGroupChooseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.count.setText(String.valueOf(item.getChannel().size()));
        holder.binding.getRoot().setSelected(item.isSelected());
        // 使用 bindingAdapterPosition，避免刷新后位置错位
        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) mListener.onItemClick(mItems.get(pos));
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterGroupChooseBinding binding;

        ViewHolder(@NonNull AdapterGroupChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
