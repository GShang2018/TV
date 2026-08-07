package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.databinding.AdapterKeepFolderBinding;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

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

        void onItemClick(Keep item);
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
        return new ViewHolder(AdapterKeepFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeepFolder item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.all.setText(holder.itemView.getContext().getString(R.string.keep_folder_all, item.getCount()));
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        holder.binding.all.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.vodAdapter.setItems(Keep.getVod(item.getId()));
        holder.binding.all.setOnClickListener(view -> mListener.onItemClick(item));
        holder.binding.delete.setOnClickListener(view -> mListener.onItemDelete(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterKeepFolderBinding binding;
        private final VodAdapter vodAdapter;

        ViewHolder(@NonNull AdapterKeepFolderBinding binding, OnClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.vodAdapter = new VodAdapter(listener);
            this.binding.recycler.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
            this.binding.recycler.setAdapter(vodAdapter);
        }
    }

    private static class VodAdapter extends RecyclerView.Adapter<VodAdapter.VodHolder> {

        private final List<Keep> mItems;
        private final OnClickListener mListener;

        VodAdapter(OnClickListener listener) {
            this.mItems = new ArrayList<>();
            this.mListener = listener;
        }

        void setItems(List<Keep> items) {
            mItems.clear();
            if (items.size() > 10) items = items.subList(0, 10);
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            VodHolder holder = new VodHolder(AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            // 3:4 竖版封面尺寸
            int width = ResUtil.dp2px(90);
            int height = ResUtil.dp2px(120);
            holder.binding.image.getLayoutParams().width = width;
            holder.binding.image.getLayoutParams().height = height;
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull VodHolder holder, int position) {
            Keep item = mItems.get(position);
            holder.binding.name.setText(item.getVodName());
            holder.binding.site.setText(item.getSiteName());
            holder.binding.site.setVisibility(View.VISIBLE);
            holder.binding.year.setVisibility(View.GONE);
            holder.binding.remark.setVisibility(View.GONE);
            ImgUtil.loadPoster(item.getVodName(), item.getVodPic(), holder.binding.image);
            holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        static class VodHolder extends RecyclerView.ViewHolder {

            private final AdapterVodRectBinding binding;

            VodHolder(@NonNull AdapterVodRectBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
