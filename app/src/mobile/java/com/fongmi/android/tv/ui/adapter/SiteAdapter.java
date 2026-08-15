package com.fongmi.android.tv.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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
        if (keyword == null || keyword.isEmpty()) {
            mItems.addAll(allItems);
        } else {
            String lower = keyword.toLowerCase();
            for (Site site : allItems) {
                if (site.getName().toLowerCase().contains(lower)) mItems.add(site);
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

        if (search) {
            // 搜索模式：用开关表示是否参与检索
            int searchable = item.getSearchable();
            boolean canSearch = searchable != 0;
            boolean isActive = item.isSearchable();
            holder.binding.switchBtn.setVisibility(View.VISIBLE);
            holder.binding.switchBtn.setChecked(isActive);
            holder.binding.switchBtn.setEnabled(canSearch);
            holder.binding.switchBtn.setClickable(canSearch);
            holder.binding.switchBtn.setFocusable(canSearch);
            holder.binding.switchBtn.setThumbTintList(ColorStateList.valueOf(canSearch ? Color.WHITE : Color.argb(0x66, 0xFF, 0xFF, 0xFF)));
            holder.binding.switchBtn.setOnClickListener(v -> listener.onSearchClick(item));
            // 不支持搜索的站源：文字和开关处于禁用样式
            boolean dimmed = !canSearch;
            holder.binding.text.setEnabled(canSearch);
            holder.binding.text.setAlpha(dimmed ? 0.4f : 1f);
            holder.binding.text.setOnClickListener(canSearch ? v -> listener.onSearchClick(item) : null);
        } else {
            // 切换站源模式
            holder.binding.switchBtn.setVisibility(View.GONE);
            holder.binding.text.setEnabled(true);
            holder.binding.text.setAlpha(1f);
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
