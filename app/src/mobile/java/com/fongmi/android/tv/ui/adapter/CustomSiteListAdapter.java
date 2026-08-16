package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.databinding.ItemCustomSiteListBinding;

import java.util.ArrayList;
import java.util.List;

public class CustomSiteListAdapter extends RecyclerView.Adapter<CustomSiteListAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private List<CustomSite> mItems;

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
        notifyDataSetChanged();
        return this;
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
    }

    private void showMoreMenu(ViewHolder holder, CustomSite item) {
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

        private final ItemCustomSiteListBinding binding;

        ViewHolder(@NonNull ItemCustomSiteListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
