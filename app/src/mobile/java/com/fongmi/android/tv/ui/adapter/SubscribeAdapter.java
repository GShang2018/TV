package com.fongmi.android.tv.ui.adapter;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.ItemSubscribeBinding;

import java.util.ArrayList;
import java.util.List;

public class SubscribeAdapter extends RecyclerView.Adapter<SubscribeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private List<Config> mItems;
    private String active;

    public SubscribeAdapter(OnClickListener listener) {
        this.mListener = listener;
        setHasStableIds(true);
    }

    public interface OnClickListener {

        void onSelect(Config item);

        void onView(Config item);

        void onCustom(Config item);

        void onEdit(Config item);

        void onCopy(Config item);

        void onDelete(Config item);
    }

    public SubscribeAdapter addAll(List<Config> items, String active) {
        mItems = new ArrayList<>(items);
        this.active = active;
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getId();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSubscribeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = mItems.get(position);
        boolean checked = TextUtils.equals(active, item.getUrl());
        holder.binding.name.setText(item.getDesc());
        if (item.isCustom()) {
            holder.binding.url.setText(getString(holder, R.string.custom_site_list));
            holder.binding.source.setVisibility(View.GONE);
            holder.binding.more.setVisibility(View.GONE);
            holder.binding.switchBtn.setVisibility(View.VISIBLE);
            holder.binding.name.setOnClickListener(v -> mListener.onCustom(item));
            holder.binding.url.setOnClickListener(v -> mListener.onCustom(item));
        } else {
            if (item.isDepot()) {
                holder.binding.url.setText(getString(holder, R.string.subscribe_selected_line, item.getLineName()));
            } else {
                holder.binding.url.setText(item.getUrl());
            }
            if (!TextUtils.isEmpty(item.getSource())) {
                holder.binding.source.setText(getString(holder, R.string.subscribe_from, item.getSource()));
                holder.binding.source.setVisibility(View.VISIBLE);
            } else {
                holder.binding.source.setVisibility(View.GONE);
            }
            holder.binding.more.setVisibility(View.VISIBLE);
            holder.binding.switchBtn.setVisibility(View.VISIBLE);
            holder.binding.name.setOnClickListener(v -> onItemClick(item));
            holder.binding.url.setOnClickListener(v -> onItemClick(item));
            holder.binding.more.setOnClickListener(v -> showMoreMenu(holder, item));
        }
        holder.binding.switchBtn.setOnCheckedChangeListener(null);
        holder.binding.switchBtn.setChecked(checked);
        holder.binding.switchBtn.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked && !checked) mListener.onSelect(item);
        });
    }

    private void onItemClick(Config item) {
        mListener.onView(item);
    }

    private void showMoreMenu(ViewHolder holder, Config item) {
        PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.binding.more);
        popup.inflate(R.menu.menu_subscribe_item);
        forceShowIcons(popup);
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete);
        SpannableString redText = new SpannableString(deleteItem.getTitle());
        redText.setSpan(new ForegroundColorSpan(Color.parseColor("#C0392B")), 0, redText.length(), 0);
        deleteItem.setTitle(redText);
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

    private String getString(ViewHolder holder, int resId, Object... args) {
        return holder.itemView.getContext().getString(resId, args);
    }

    private void forceShowIcons(PopupMenu popup) {
        try {
            Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popup);
            Method setForceShowIcon = menuPopupHelper.getClass().getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSubscribeBinding binding;

        ViewHolder(@NonNull ItemSubscribeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
