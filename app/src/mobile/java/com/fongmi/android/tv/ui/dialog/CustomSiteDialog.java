package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.DialogCustomSiteBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.UUID;

public class CustomSiteDialog {

    private final DialogCustomSiteBinding binding;
    private final Fragment fragment;
    private AlertDialog dialog;

    public static CustomSiteDialog create(Fragment fragment) {
        return new CustomSiteDialog(fragment);
    }

    public CustomSiteDialog(Fragment fragment) {
        this.fragment = fragment;
        this.binding = DialogCustomSiteBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.setting_custom_vod).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, null).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initEvent() {
        binding.api.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void onPositive(DialogInterface dialog, int which) {
        String name = binding.name.getText().toString().trim();
        String api = binding.api.getText().toString().trim();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(api)) {
            Notify.show(R.string.error_custom_site_empty);
            return;
        }
        CustomSite custom = new CustomSite();
        custom.setKey("custom_" + UUID.randomUUID().toString().substring(0, 8));
        custom.setName(name);
        custom.setApi(api);
        custom.setType(1);
        custom.setSearchable(1);
        custom.setQuickSearch(1);
        custom.setFilterable(1);
        custom.setStyle(Style.rect());
        custom.save();
        Notify.show(R.string.custom_site_added);
        RefreshEvent.video();
        RefreshEvent.config();
        Util.hideKeyboard(binding.api);
        dialog.dismiss();
    }
}
