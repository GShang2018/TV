package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogImageBinding;
import com.fongmi.android.tv.ui.custom.TouchImageView;
import com.fongmi.android.tv.utils.ImgUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ImageDialog {

    private final DialogImageBinding binding;
    private AlertDialog dialog;
    private String url;

    public static ImageDialog create(Activity activity) {
        return new ImageDialog(activity);
    }

    public ImageDialog(Activity activity) {
        this.binding = DialogImageBinding.inflate(LayoutInflater.from(activity));
    }

    public ImageDialog url(String url) {
        this.url = url;
        return this;
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        binding.getRoot().setOnClickListener(v -> dialog.dismiss());
    }

    private void initView() {
        TouchImageView image = binding.image;
        Glide.with(App.get()).asBitmap().load(ImgUtil.getUrl(url)).placeholder(R.drawable.ic_img_loading).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                image.setOrigSize(resource.getWidth(), resource.getHeight());
                image.setImageBitmap(resource);
            }

            @Override
            public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
            }
        });
    }
}
