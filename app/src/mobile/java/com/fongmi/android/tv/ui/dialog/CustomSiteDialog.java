package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.DialogCustomSiteBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.UUID;

public class CustomSiteDialog {

    private final DialogCustomSiteBinding binding;
    private final Fragment fragment;
    private final CustomSite custom;
    private AlertDialog dialog;
    private Runnable onSaved;

    public static CustomSiteDialog create(Fragment fragment) {
        return new CustomSiteDialog(fragment, null);
    }

    public static CustomSiteDialog create(Fragment fragment, CustomSite custom) {
        return new CustomSiteDialog(fragment, custom);
    }

    public CustomSiteDialog setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
        return this;
    }

    public CustomSiteDialog(Fragment fragment, CustomSite custom) {
        this.fragment = fragment;
        this.custom = custom;
        this.binding = DialogCustomSiteBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initView();
        initDialog();
        initEvent();
    }

    private void initView() {
        if (custom == null) return;
        binding.name.setText(custom.getName());
        binding.api.setText(custom.getApi());
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(custom == null ? R.string.setting_custom_vod : R.string.setting_edit_custom_site).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, null).create();
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
        boolean edit = custom != null;
        CustomSite target = custom;
        if (target == null) {
            target = new CustomSite();
            target.setKey("custom_" + UUID.randomUUID().toString().substring(0, 8));
            target.setType(1);
            target.setSearchable(1);
            target.setQuickSearch(1);
            target.setFilterable(1);
            target.setStyle(Style.rect());
        }
        target.setName(name);
        target.setApi(api);
        target.save();
        Notify.show(edit ? R.string.custom_site_updated : R.string.custom_site_added);
        refresh();
        Util.hideKeyboard(binding.api);
        if (onSaved != null) onSaved.run();
        dialog.dismiss();
    }

    private void refresh() {
        Config config = VodConfig.get().getConfig();
        if (config != null && config.isCustom()) {
            VodConfig.load(config, new Callback() {
                @Override
                public void success(String result) {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }

                @Override
                public void success() {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }

                @Override
                public void error(String msg) {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }
            });
        } else {
            RefreshEvent.video();
            RefreshEvent.config();
        }
    }
}
