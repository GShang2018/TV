package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.databinding.AdapterKeepFolderBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class KeepFolderAdapter extends RecyclerView.Adapter<KeepFolderAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<KeepFolder> mItems;
    private boolean delete;

    public KeepFolderAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(KeepFolder item);

        void onItemDelete(KeepFolder item);
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<KeepFolder> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void remove(KeepFolder item) {
        int index = mItems.indexOf(item);
        if (index == -1) return;
        mItems.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterKeepFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeepFolder item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.count.setText(holder.itemView.getContext().getString(R.string.keep_folder_count, item.getCount()));
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        bindCovers(holder, item);
        holder.binding.getRoot().setOnClickListener(view -> {
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        });
        holder.binding.delete.setOnClickListener(view -> mListener.onItemDelete(item));
    }

    private void bindCovers(ViewHolder holder, KeepFolder item) {
        List<Keep> keeps = Keep.getVod(item.getId());
        if (keeps.isEmpty()) {
            // 无视频：三个封面都显示收藏夹首字 + 随机背景色，保持堆叠
            ImgUtil.loadPoster(item.getName(), "", holder.binding.cover1);
            ImgUtil.loadPoster(item.getName(), "", holder.binding.cover2);
            ImgUtil.loadPoster(item.getName(), "", holder.binding.cover3);
            holder.binding.cover2.setVisibility(View.VISIBLE);
            holder.binding.cover3.setVisibility(View.VISIBLE);
        } else {
            ImgUtil.loadPoster(keeps.get(0).getVodName(), keeps.get(0).getVodPic(), holder.binding.cover1);
            holder.binding.cover2.setVisibility(keeps.size() > 1 ? View.VISIBLE : View.GONE);
            holder.binding.cover3.setVisibility(keeps.size() > 2 ? View.VISIBLE : View.GONE);
            if (keeps.size() > 1) ImgUtil.loadPoster(keeps.get(1).getVodName(), keeps.get(1).getVodPic(), holder.binding.cover2);
            if (keeps.size() > 2) ImgUtil.loadPoster(keeps.get(2).getVodName(), keeps.get(2).getVodPic(), holder.binding.cover3);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterKeepFolderBinding binding;

        ViewHolder(@NonNull AdapterKeepFolderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
