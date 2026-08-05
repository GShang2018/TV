package com.fongmi.android.tv.ui.holder;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodPortraitBinding;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodPortraitHolder extends BaseVodHolder {

    private final VodAdapter.OnClickListener listener;
    private final AdapterVodPortraitBinding binding;

    public VodPortraitHolder(@NonNull AdapterVodPortraitBinding binding, VodAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public VodPortraitHolder size(int[] size) {
        binding.image.getLayoutParams().width = size[0];
        binding.image.getLayoutParams().height = size[1];
        return this;
    }

    @Override
    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.year.setText(item.getVodYear());
        binding.site.setText(item.getSiteName());
        binding.remark.setText(item.getVodRemarks());
        binding.site.setVisibility(item.getSiteVisible());
        binding.name.setVisibility(item.getNameVisible());
        binding.year.setVisibility(item.getYearVisible());
        binding.remark.setVisibility(item.getRemarkVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        // 竖版封面应用与播放页一致的裁切方式（左/中/右）
        ImgUtil.loadPoster(item.getVodName(), item.getVodPic(), binding.image);
        setTagMaxWidth(binding.image, 12, binding.year, binding.site, binding.remark);
    }
}
