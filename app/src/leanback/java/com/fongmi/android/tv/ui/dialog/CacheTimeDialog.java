package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogCacheTimeBinding;
import com.fongmi.android.tv.impl.CacheTimeCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CacheTimeDialog {

    private final DialogCacheTimeBinding binding;
    private final CacheTimeCallback callback;
    private int value;

    public static CacheTimeDialog create(FragmentActivity activity) {
        return new CacheTimeDialog(activity);
    }

    public CacheTimeDialog(FragmentActivity activity) {
        this.callback = (CacheTimeCallback) activity;
        this.binding = DialogCacheTimeBinding.inflate(LayoutInflater.from(activity));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.player_cache_time).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getCacheTime());
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setCacheTime((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setCacheTime(value);
        dialog.dismiss();
    }
}
