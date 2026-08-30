package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CustomVod;
import com.fongmi.android.tv.databinding.ItemMineBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class MineAdapter extends RecyclerView.Adapter<MineAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<CustomVod> mItems;
    private boolean delete;

    public MineAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(CustomVod item);

        void onEdit(CustomVod item);

        boolean onLongClick();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<CustomVod> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void remove(CustomVod item) {
        int index = mItems.indexOf(item);
        if (index == -1) return;
        mItems.remove(index);
        notifyItemRemoved(index);
    }

    public void clear() {
        mItems.clear();
        setDelete(false);
        notifyDataSetChanged();
        CustomVod.deleteAll();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomVod item = mItems.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.year.setText(item.getVodYear());
        holder.binding.year.setVisibility(TextUtils.isEmpty(item.getVodYear()) ? View.GONE : View.VISIBLE);
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getVodRemarks()) ? View.GONE : View.VISIBLE);
        holder.binding.site.setText(getLineText(holder, item));
        holder.binding.edit.setVisibility(delete ? View.GONE : View.VISIBLE);
        ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
        holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(view -> mListener.onLongClick());
        holder.binding.edit.setOnClickListener(view -> {
            if (!isDelete()) mListener.onEdit(item);
        });
    }

    private String getLineText(ViewHolder holder, CustomVod item) {
        if (!item.hasPlayUrl()) return holder.itemView.getContext().getString(R.string.mine_play_url_empty);
        String from = item.getVodPlayFrom();
        if (!TextUtils.isEmpty(from)) return from.split("\\$\\$\\$")[0].trim();
        return item.getVodName();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemMineBinding binding;

        ViewHolder(@NonNull ItemMineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
