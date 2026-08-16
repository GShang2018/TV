package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.SiteItem;
import com.fongmi.android.tv.databinding.ItemSiteListBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SiteListAdapter extends RecyclerView.Adapter<SiteListAdapter.ViewHolder> {

    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_AVAILABLE = 1;
    public static final int STATUS_UNAVAILABLE = 2;

    private final OnClickListener mListener;
    private List<SiteItem> mItems;
    private final Map<String, Integer> mStatus = new HashMap<>();
    private final Set<String> mSelected = new HashSet<>();

    public interface OnClickListener {
        void onItemClick(SiteItem item);
        void onToggle(SiteItem item, boolean enabled);
    }

    public SiteListAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public SiteListAdapter addAll(List<SiteItem> items) {
        mItems = new ArrayList<>(items);
        mStatus.clear();
        mSelected.clear();
        notifyDataSetChanged();
        return this;
    }

    public SiteListAdapter addItems(List<SiteItem> items) {
        if (mItems == null) mItems = new ArrayList<>();
        int start = mItems.size();
        mItems.addAll(items);
        notifyItemRangeInserted(start, items.size());
        return this;
    }

    public void switchSelection(String key) {
        if (mItems == null) return;
        // 清除旧选中项，只通知变化的条目
        for (int i = 0; i < mItems.size(); i++) {
            String k = mItems.get(i).getKey();
            if (mSelected.contains(k) && !k.equals(key)) {
                mSelected.remove(k);
                notifyItemChanged(i, "status");
            }
        }
        // 设置新选中项
        if (!mSelected.contains(key)) {
            mSelected.add(key);
            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getKey().equals(key)) {
                    notifyItemChanged(i, "status");
                    break;
                }
            }
        }
    }

    public List<SiteItem> getItems() {
        return mItems == null ? new ArrayList<>() : mItems;
    }

    public void setStatus(String key, int status) {
        if (mItems == null) return;
        mStatus.put(key, status);
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getKey().equals(key)) {
                notifyItemChanged(i, "status");
                break;
            }
        }
    }

    public void clearStatus() {
        mStatus.clear();
        if (mItems != null) notifyDataSetChanged();
    }

    public void setSelected(String key, boolean selected) {
        if (selected) mSelected.add(key);
        else mSelected.remove(key);
        if (mItems == null) return;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getKey().equals(key)) {
                notifyItemChanged(i, "status");
                break;
            }
        }
    }

    public void clearSelected() {
        mSelected.clear();
        if (mItems != null) notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSiteListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SiteItem item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.api.setText(item.getUrl());
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) mListener.onItemClick(item);
        });
        holder.binding.switchBtn.setOnCheckedChangeListener(null);
        holder.binding.switchBtn.setChecked(mSelected.contains(item.getKey()));
        holder.binding.switchBtn.setOnCheckedChangeListener((button, isChecked) -> {
            if (mListener != null) mListener.onToggle(item, isChecked);
        });
        updateDot(holder, item.getKey());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        if (payloads.contains("status")) {
            SiteItem item = mItems.get(position);
            holder.binding.switchBtn.setOnCheckedChangeListener(null);
            holder.binding.switchBtn.setChecked(mSelected.contains(item.getKey()));
            holder.binding.switchBtn.setOnCheckedChangeListener((button, isChecked) -> {
                if (mListener != null) mListener.onToggle(item, isChecked);
            });
            updateDot(holder, item.getKey());
        }
    }

    private void updateDot(ViewHolder holder, String key) {
        Integer status = mStatus.get(key);
        if (status == null) status = STATUS_UNKNOWN;
        switch (status) {
            case STATUS_AVAILABLE:
                holder.binding.dot.setBackgroundResource(R.drawable.shape_dot_green);
                break;
            case STATUS_UNAVAILABLE:
                holder.binding.dot.setBackgroundResource(R.drawable.shape_dot_red);
                break;
            default:
                holder.binding.dot.setBackgroundResource(R.drawable.shape_dot_gray);
                break;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSiteListBinding binding;

        ViewHolder(@NonNull ItemSiteListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
