package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

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
        setHasStableIds(true);
    }

    public interface OnClickListener {

        void onLineClick(Depot item);

        void onEdit(Depot item);

        void onCopy(Depot item);

        void onDelete(Depot item);
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

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getUrl().hashCode();
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
        holder.binding.name.setText(item.getName());
        holder.binding.url.setText(item.getUrl());
        holder.binding.root.setOnClickListener(v -> mListener.onLineClick(item));
        holder.binding.more.setOnClickListener(v -> showMoreMenu(holder, item));
        holder.binding.switchBtn.setOnCheckedChangeListener(null);
        holder.binding.switchBtn.setChecked(checked);
        holder.binding.switchBtn.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked && !checked) mListener.onLineClick(item);
        });
    }

    private void showMoreMenu(ViewHolder holder, Depot item) {
        PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.binding.more);
        popup.inflate(R.menu.menu_subscribe_item);
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_edit) {
                mListener.onEdit(item);
                return true;
            } else if (id == R.id.action_copy) {
                mListener.onCopy(item);
                return true;
            } else if (id == R.id.action_delete) {
                mListener.onDelete(item);
                return true;
            }
            return false;
        });
        popup.show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLineSelectBinding binding;

        ViewHolder(@NonNull ItemLineSelectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
