package com.fongmi.android.tv.ui.adapter;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.AdapterSiteBinding;

import java.util.ArrayList;
import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<Site> mItems;
    private List<Site> allItems;
    private boolean search;
    private boolean change;

    public SiteAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
        this.allItems = new ArrayList<>();
        this.addAll();
    }

    public interface OnClickListener {

        void onTextClick(Site item);

        void onSearchClick(Site item);
    }

    public SiteAdapter search(boolean search) {
        this.search = search;
        return this;
    }

    public SiteAdapter change(boolean change) {
        this.change = change;
        return this;
    }

    private void addAll() {
        allItems.addAll(VodConfig.get().getSites());
        mItems.addAll(allItems);
    }

    public List<Site> getItems() {
        return mItems;
    }

    public void keyword(String keyword) {
        mItems.clear();
        if (TextUtils.isEmpty(keyword)) {
            mItems.addAll(allItems);
        } else {
            for (Site site : allItems) {
                if (site.getName().toLowerCase().contains(keyword.toLowerCase())) mItems.add(site);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Site item = mItems.get(position);
        boolean on = !search || change;
        holder.binding.text.setText(item.getName());
        holder.binding.text.setEnabled(true);
        holder.binding.text.setFocusable(true);
        if (search) {
            // 搜索模式：点击站源切换是否参与检索
            boolean searchable = item.isSearchable();
            holder.binding.text.setSelected(searchable);
            holder.binding.text.setActivated(searchable);
            if (searchable) {
                holder.binding.text.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.site_stroke_active)));
                holder.binding.text.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.site_text_active));
            } else {
                holder.binding.text.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.site_disabled_stroke)));
                holder.binding.text.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.site_disabled_text));
            }
            holder.binding.text.setOnClickListener(v -> listener.onSearchClick(item));
        } else {
            // 切换站源模式
            holder.binding.text.setSelected(on && item.isActivated());
            holder.binding.text.setActivated(on && item.isActivated());
            holder.binding.text.setOnClickListener(v -> listener.onTextClick(item));
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSiteBinding binding;

        ViewHolder(@NonNull AdapterSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
