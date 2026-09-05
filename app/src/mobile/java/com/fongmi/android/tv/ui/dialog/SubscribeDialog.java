package com.fongmi.android.tv.ui.dialog;

import android.Manifest;
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
import com.fongmi.android.tv.bean.CustomLine;
import com.fongmi.android.tv.databinding.DialogSubscribeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.fragment.SubscribeFragment;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Prefers;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.UUID;

public class SubscribeDialog {

    private final DialogSubscribeBinding binding;
    private final Fragment fragment;
    private AlertDialog dialog;
    private Config editing;
    private boolean append;
    private boolean init;
    // true = 编辑/新建的是“自定义线路”
    private boolean custom;
    private int type;

    public static SubscribeDialog create(Fragment fragment, int type) {
        return new SubscribeDialog(fragment, type, null);
    }

    public static SubscribeDialog edit(Fragment fragment, int type, Config config) {
        return new SubscribeDialog(fragment, type, config);
    }

    public SubscribeDialog(Fragment fragment, int type, Config config) {
        this.fragment = fragment;
        this.type = type;
        this.editing = config;
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

    private boolean isEdit() {
        return editing != null;
    }

    private boolean isLineEdit() {
        return isEdit() && editing.isCustom() && !editing.isCustomSites();
    }

    private void initDialog() {
        int titleRes = isEdit() ? R.string.subscribe_edit_title : (type == 0 ? R.string.subscribe_add_vod : R.string.subscribe_add_live);
        int positiveRes = isEdit() ? R.string.dialog_edit : R.string.subscribe_positive;
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(titleRes).setView(binding.getRoot()).setPositiveButton(positiveRes, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        // 点播新增：顶部提供“自定义线路 / 远程订阅”模式选择，默认自定义线路
        if (type == 0 && !isEdit()) {
            binding.group.setVisibility(View.VISIBLE);
            binding.modeCustom.setChecked(true);
            custom = true;
            applyCustomUi(true);
        } else if (isLineEdit()) {
            custom = true;
            applyCustomUi(true);
            binding.name.setText(editing.getName());
            CustomLine line = CustomLine.find(editing.getCustomLineId());
            binding.url.setText(line == null ? "" : line.getUrl());
        } else {
            custom = false;
            applyCustomUi(false);
        }
        if (!custom && isEdit()) {
            binding.name.setText(editing.getName());
            binding.url.setText(editing.getUrl());
            if (type == 1) binding.epg.setText(editing.getEpg());
        }
    }

    private void applyCustomUi(boolean custom) {
        binding.epgInput.setVisibility(View.GONE);
        binding.use.setVisibility(View.GONE);
        if (custom) {
            binding.choose.setHint(R.string.custom_line_addr);
            binding.choose.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_NONE);
        } else {
            binding.choose.setHint(R.string.subscribe_url);
            // setEndIconMode() 每次切换都会用 delegate 自带监听覆盖此前设置的监听，
            // 切回“远程订阅”后必须重新挂载文件夹图标与点击监听，否则无法点击选择本地文件
            binding.choose.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
            binding.choose.setEndIconDrawable(R.drawable.ic_action_choose);
            binding.choose.setEndIconOnClickListener(this::onChoose);
            binding.epgInput.setVisibility(type == 1 ? View.VISIBLE : View.GONE);
            binding.epgInput.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
            binding.epgInput.setEndIconDrawable(R.drawable.ic_action_choose);
            binding.epgInput.setEndIconOnClickListener(this::onChoose);
            binding.use.setChecked(!isEdit());
            binding.use.setVisibility(isEdit() ? View.GONE : View.VISIBLE);
        }
    }

    private void initEvent() {
        if (init) return;
        init = true;
        binding.choose.setEndIconOnClickListener(this::onChoose);
        binding.epgInput.setEndIconOnClickListener(this::onChoose);
        binding.group.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            custom = checkedId == binding.modeCustom.getId();
            applyCustomUi(custom);
        });
        binding.url.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 自定义线路的“地址”只是备注，不做协议自动补全
                if (!custom) detect(s.toString());
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
        if (custom) {
            onCustomPositive();
        } else {
            String url = UrlUtil.fixUrl(binding.url.getText().toString().trim());
            String name = binding.name.getText().toString().trim();
            String epg = binding.epg.getText().toString().trim();
            if (url.isEmpty()) {
                Notify.show(R.string.subscribe_edit_empty);
                return;
            }
            if (isEdit()) onEditPositive(url, name, epg);
            else onAddPositive(url, name, epg);
        }
    }

    // ==================== 自定义线路 ====================

    private void onCustomPositive() {
        String name = binding.name.getText().toString().trim();
        String address = binding.url.getText().toString().trim();
        if (name.isEmpty()) {
            Notify.show(R.string.custom_line_empty);
            return;
        }
        Util.hideKeyboard(binding.url);
        dialog.dismiss();
        if (isLineEdit()) updateCustomLine(name, address);
        else createCustomLine(name, address);
    }

    private void createCustomLine(String name, String address) {
        CustomLine line = new CustomLine();
        line.setId("line_" + UUID.randomUUID().toString().substring(0, 8));
        line.setName(name);
        line.setUrl(address);
        line.setSites(new ArrayList<>());
        line.save();
        Notify.show(R.string.custom_line_added);
        refresh();
    }

    private void updateCustomLine(String name, String address) {
        CustomLine line = CustomLine.find(editing.getCustomLineId());
        if (line == null) return;
        line.setName(name);
        line.setUrl(address);
        line.save();
        Notify.show(R.string.custom_line_updated);
        refresh();
    }

    // ==================== 远程订阅 ====================

    private void onAddPositive(String url, String name, String epg) {
        Config config = Config.find(url, name, type);
        if (type == 1) config.epg(epg);
        boolean use = binding.use.isChecked();
        if (use) config.update();
        else config.save();
        Notify.progress(fragment.getContext());
        Util.hideKeyboard(binding.url);
        dialog.dismiss();
        probe(use, config);
    }

    private void onEditPositive(String url, String name, String epg) {
        String oldUrl = editing.getUrl();
        boolean wasActive = oldUrl.equals(Prefers.getString("config_" + type, null));
        if (!url.equals(oldUrl)) {
            Config.delete(oldUrl, type);
            if (type == 0) Config.delete(oldUrl, 1);
        }
        Config config = Config.find(url, name, type);
        if (type == 1) config.epg(epg);
        if (wasActive) {
            config.update();
            Notify.progress(fragment.getContext());
            Util.hideKeyboard(binding.url);
            dialog.dismiss();
            probe(true, config);
        } else {
            config.save();
            Util.hideKeyboard(binding.url);
            dialog.dismiss();
            refresh();
        }
    }

    private void probe(boolean use, Config config) {
        ensurePermission(() -> {
            if (type == 0) VodConfig.probe(config, getProbe(use, config));
            else LiveConfig.probe(config, getProbe(use, config));
        }, config);
    }

    private void ensurePermission(Runnable action, Config config) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(fragment.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(fragment).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> action.run());
        } else {
            action.run();
        }
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
        if (use) load(config);
        else refresh();
    }

    private void load(Config config) {
        ensurePermission(() -> {
            if (type == 0) VodConfig.load(config, getCallback());
            else LiveConfig.load(config, getCallback());
        }, config);
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
