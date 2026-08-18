package com.fongmi.android.tv.ui.adapter;

import android.graphics.Color;
import android.text.SpannableString;
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
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.databinding.ItemCustomSiteListBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomSiteListAdapter extends RecyclerView.Adapter<CustomSiteListAdapter.ViewHolder> {

    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_AVAILABLE = 1;
    public static final int STATUS_UNAVAILABLE = 2;

    private final OnClickListener mListener;
    private List<CustomSite> mItems;
    private final Map<String, Integer> mStatus = new HashMap<>();

    public CustomSiteListAdapter(OnClickListener listener) {
        this.mListener = listener;
        setHasStableIds(true);
    }

    public interface OnClickListener {

        void onToggle(CustomSite item, boolean enabled);

        void onEdit(CustomSite item);

        void onCopy(CustomSite item);

        void onDelete(CustomSite item);
    }

    public CustomSiteListAdapter addAll(List<CustomSite> items) {
        mItems = new ArrayList<>(items);
        mStatus.clear();
        notifyDataSetChanged();
        return this;
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

    public int remove(CustomSite item) {
        mItems.remove(item);
        notifyDataSetChanged();
        return getItemCount();
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getKey().hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCustomSiteListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomSite item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.api.setText(item.getApi());
        holder.binding.more.setOnClickListener(v -> showMoreMenu(holder, item));
        holder.binding.switchBtn.setOnCheckedChangeListener(null);
        holder.binding.switchBtn.setChecked(item.getEnabled());
        holder.binding.switchBtn.setOnCheckedChangeListener((button, isChecked) -> mListener.onToggle(item, isChecked));
        updateDot(holder, item.getKey());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        if (payloads.contains("status")) {
            updateDot(holder, mItems.get(position).getKey());
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

    private void showMoreMenu(ViewHolder holder, CustomSite item) {
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

        private final ItemCustomSiteListBinding binding;

        ViewHolder(@NonNull ItemCustomSiteListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
