package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogSubscribeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.fragment.SubscribeFragment;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SubscribeDialog {

    private final DialogSubscribeBinding binding;
    private final Fragment fragment;
    private AlertDialog dialog;
    private boolean append;
    private boolean init;
    private int type;

    public static SubscribeDialog create(Fragment fragment, int type) {
        return new SubscribeDialog(fragment, type);
    }

    public SubscribeDialog(Fragment fragment, int type) {
        this.fragment = fragment;
        this.type = type;
        this.binding = DialogSubscribeBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.append = true;
    }

    public SubscribeDialog setUrl(String url) {
        binding.url.setText(url);
        return this;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(type == 0 ? R.string.subscribe_add_vod : R.string.subscribe_add_live).setView(binding.getRoot()).setPositiveButton(R.string.subscribe_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.epgInput.setVisibility(type == 1 ? View.VISIBLE : View.GONE);
    }

    private void initEvent() {
        if (init) return;
        init = true;
        binding.choose.setEndIconOnClickListener(this::onChoose);
        binding.epgInput.setEndIconOnClickListener(this::onChoose);
        binding.url.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.url.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void onChoose(View view) {
        FileChooser.from(fragment).show();
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ile://");
        } else if (append && "a".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ssets://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        String url = UrlUtil.fixUrl(binding.url.getText().toString().trim());
        String name = binding.name.getText().toString().trim();
        if (url.isEmpty()) {
            dialog.dismiss();
            return;
        }
        Config config = Config.find(url, name, type);
        if (type == 1) config.epg(binding.epg.getText().toString().trim());
        boolean use = binding.use.isChecked();
        if (use) config.update();
        else config.save();
        Notify.progress(fragment.getContext());
        Util.hideKeyboard(binding.url);
        dialog.dismiss();
        if (type == 0) VodConfig.probe(config, getProbe(use, config));
        else LiveConfig.probe(config, getProbe(use, config));
    }

    private Callback getProbe(boolean use, Config config) {
        return new Callback() {
            @Override
            public void success(String result) {
                afterProbe(use, config);
            }

            @Override
            public void success() {
                afterProbe(use, config);
            }

            @Override
            public void error(String msg) {
                afterProbe(use, config);
            }
        };
    }

    private void afterProbe(boolean use, Config config) {
        Notify.dismiss();
        Notify.show(R.string.subscribe_added);
        if (use) load(config);
        else refresh();
    }

    private void load(Config config) {
        if (type == 0) VodConfig.load(config, getCallback());
        else LiveConfig.load(config, getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success(String result) {
                refresh();
            }

            @Override
            public void success() {
                refresh();
            }

            @Override
            public void error(String msg) {
                refresh();
            }
        };
    }

    private void refresh() {
        Notify.dismiss();
        ((SubscribeFragment) fragment).refresh();
        RefreshEvent.video();
        RefreshEvent.config();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
