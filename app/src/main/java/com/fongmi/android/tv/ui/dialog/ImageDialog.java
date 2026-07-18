package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogImageBinding;
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
        ImgUtil.loadVod("", url, binding.image);
    }
}
