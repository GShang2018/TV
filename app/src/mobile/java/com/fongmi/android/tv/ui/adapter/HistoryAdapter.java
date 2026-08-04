package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.databinding.AdapterVodListBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnClickListener mListener;
    private final List<History> mItems;
    private int width, height;
    private int viewType = ViewType.GRID;
    private boolean delete;

    public HistoryAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(History item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    public void setSize(int[] size) {
        this.width = size[0];
        this.height = size[1];
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<History> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        setDelete(false);
        notifyDataSetChanged();
        History.delete(VodConfig.getCid());
    }

    public void remove(History item) {
        int index = mItems.indexOf(item);
        if (index == -1) return;
        mItems.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.LIST) {
            return new ListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.image.getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        History item = mItems.get(position);
        if (holder instanceof ListHolder) {
            ListHolder listHolder = (ListHolder) holder;
            listHolder.binding.name.setText(item.getVodName());
            listHolder.binding.site.setText(item.getSiteName());
            listHolder.binding.site.setVisibility(item.getSiteVisible());
            listHolder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
            listHolder.binding.remark.setVisibility(View.VISIBLE);
            listHolder.binding.progress.setMax((int) item.getDuration());
            listHolder.binding.progress.setProgress((int) item.getPosition());
            listHolder.binding.progress.setVisibility(View.VISIBLE);
            ImgUtil.load(item.getVodName(), item.getVodPic(), listHolder.binding.image, ImageView.ScaleType.FIT_CENTER, false);
            setClickListener(listHolder.binding.getRoot(), item);
        } else {
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.binding.image.getLayoutParams().width = width;
            viewHolder.binding.image.getLayoutParams().height = height;
            viewHolder.binding.name.setText(item.getVodName());
            viewHolder.binding.site.setText(item.getSiteName());
            viewHolder.binding.site.setVisibility(item.getSiteVisible());
            viewHolder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
            viewHolder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
            viewHolder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
            viewHolder.binding.progress.setMax((int) item.getDuration());
            viewHolder.binding.progress.setProgress((int) item.getPosition());
            viewHolder.binding.progress.setVisibility(View.VISIBLE);
            ImgUtil.loadVod(item.getVodName(), item.getVodPic(), viewHolder.binding.image);
            BaseVodHolder.setTagMaxWidth(viewHolder.binding.image, 12, viewHolder.binding.site, viewHolder.binding.remark);
            setClickListener(viewHolder.binding.getRoot(), item);
        }
    }

    private void setClickListener(View root, History item) {
        root.setOnLongClickListener(view -> mListener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ListHolder extends RecyclerView.ViewHolder {

        private final AdapterVodListBinding binding;

        ListHolder(@NonNull AdapterVodListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
