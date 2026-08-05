package com.fongmi.android.tv.ui.holder;

import android.view.View;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodOneBinding;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodOneHolder extends BaseVodHolder {

    private final VodAdapter.OnClickListener listener;
    private final AdapterVodOneBinding binding;

    public VodOneHolder(@NonNull AdapterVodOneBinding binding, VodAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    @Override
    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.name.setVisibility(item.getNameVisible());
        binding.year.setText(item.getVodYear());
        binding.year.setVisibility(item.getVodYear().isEmpty() ? View.GONE : View.VISIBLE);
        binding.remark.setText(item.getVodRemarks());
        binding.remark.setVisibility(item.getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE);
        binding.site.setText(item.getSiteName());
        binding.site.setVisibility(item.getSiteVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        // 搜索结果列表竖版封面应用与播放页一致的裁切方式（左/中/右）
        ImgUtil.loadPoster(item.getVodName(), item.getVodPic(), binding.image);
    }
}
