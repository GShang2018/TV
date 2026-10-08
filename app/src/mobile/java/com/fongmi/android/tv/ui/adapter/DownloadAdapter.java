package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.databinding.AdapterDownloadBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Download> mItems;

    public DownloadAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Download item);

        void onAction(Download item);

        void onDelete(Download item);
    }

    public void addAll(List<Download> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterDownloadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Download item = mItems.get(position);
        holder.binding.name.setText(item.getFileName());
        ImgUtil.loadPoster(item.getVodName(), item.getVodPic(), holder.binding.image);
        holder.binding.status.setText(getStatusText(item));
        bindProgress(holder, item);
        bindAction(holder, item);
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
        holder.binding.action.setOnClickListener(v -> mListener.onAction(item));
        holder.binding.delete.setOnClickListener(v -> mListener.onDelete(item));
    }

    private void bindProgress(ViewHolder holder, Download item) {
        boolean active = item.getStatus() == Download.STATUS_QUEUED || item.getStatus() == Download.STATUS_DOWNLOADING || item.getStatus() == Download.STATUS_PAUSED;
        holder.binding.progress.setVisibility(active ? android.view.View.VISIBLE : android.view.View.GONE);
        if (active) holder.binding.progress.setProgress(item.getPercent());
    }

    private void bindAction(ViewHolder holder, Download item) {
        int res;
        switch (item.getStatus()) {
            case Download.STATUS_DOWNLOADING:
            case Download.STATUS_QUEUED:
                res = R.string.download_pause;
                break;
            case Download.STATUS_PAUSED:
                res = R.string.download_resume;
                break;
            case Download.STATUS_FAILED:
                res = R.string.download_retry;
                break;
            default:
                res = R.string.download_play;
                break;
        }
        holder.binding.action.setText(res);
    }

    private String getStatusText(Download item) {
        switch (item.getStatus()) {
            case Download.STATUS_QUEUED:
                return ResUtil.getString(R.string.download_status_queued);
            case Download.STATUS_DOWNLOADING:
                return item.getTotal() > 0 ? item.getPercent() + "% · " + formatSize(item.getProgress()) + " / " + formatSize(item.getTotal()) : item.getPercent() + "%";
            case Download.STATUS_PAUSED:
                return ResUtil.getString(R.string.download_status_paused) + " · " + item.getPercent() + "%";
            case Download.STATUS_FINISHED:
                return ResUtil.getString(R.string.download_status_finished) + (item.getProgress() > 0 ? " · " + formatSize(item.getProgress()) : "");
            default:
                return ResUtil.getString(R.string.download_status_failed) + (item.getErrorMsg().isEmpty() ? "" : " · " + item.getErrorMsg());
        }
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0B";
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        if (bytes < 1024L * 1024 * 1024) return (bytes / (1024 * 1024)) + "MB";
        return String.format(Locale.getDefault(), "%.2fGB", bytes / (1024.0 * 1024 * 1024));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterDownloadBinding binding;

        ViewHolder(@NonNull AdapterDownloadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
