package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.databinding.AdapterVodGalleryBinding;
import com.fongmi.android.tv.utils.ImgUtil;

public class GalleryPresenter extends Presenter {

    private final OnClickListener listener;

    public GalleryPresenter(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterVodGalleryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        String url = (String) item;
        ViewHolder holder = (ViewHolder) viewHolder;
        ImgUtil.loadVod("", url, holder.binding.image);
        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(holder.getAbsoluteAdapterPosition()));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public interface OnClickListener {
        void onItemClick(int position);
    }

    static class ViewHolder extends Presenter.ViewHolder {
        final AdapterVodGalleryBinding binding;

        ViewHolder(AdapterVodGalleryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
