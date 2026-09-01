package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogWebdavBinding;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.WebDav;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WebDavDialog extends BaseDialog {

    private DialogWebdavBinding binding;

    public static WebDavDialog create() {
        return new WebDavDialog();
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return;
        show(manager, tag);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogWebdavBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.url.setText(WebDav.getUrl());
        binding.username.setText(WebDav.getUsername());
        binding.password.setText(WebDav.getPassword());
        binding.path.setText(WebDav.getPath());
        updateStatus();
    }

    @Override
    protected void initEvent() {
        binding.test.setOnClickListener(this::onTest);
        binding.upload.setOnClickListener(this::onUpload);
        binding.download.setOnClickListener(this::onDownload);
        binding.clear.setOnClickListener(this::onClear);
    }

    private void updateStatus() {
        if (WebDav.isConfigured()) {
            binding.status.setVisibility(View.VISIBLE);
            binding.status.setText(R.string.webdav_connected);
        } else {
            binding.status.setVisibility(View.GONE);
        }
    }

    private boolean checkInput() {
        if (TextUtils.isEmpty(binding.url.getText().toString().trim())) {
            Notify.show(R.string.webdav_url_empty);
            return false;
        }
        if (TextUtils.isEmpty(binding.username.getText().toString().trim())) {
            Notify.show(R.string.webdav_username_empty);
            return false;
        }
        if (TextUtils.isEmpty(binding.password.getText().toString().trim())) {
            Notify.show(R.string.webdav_password_empty);
            return false;
        }
        return true;
    }

    private String getPath() {
        String path = binding.path.getText().toString().trim();
        if (TextUtils.isEmpty(path)) path = "/tv";
        if (!path.startsWith("/")) path = "/" + path;
        return path;
    }

    private void onTest(View view) {
        if (!checkInput()) return;
        Notify.progress(getActivity());
        App.execute(() -> {
            WebDav.Result result = WebDav.testConnection(
                    binding.url.getText().toString().trim(),
                    binding.username.getText().toString().trim(),
                    binding.password.getText().toString().trim(),
                    getPath());
            App.post(() -> {
                Notify.dismiss();
                if (result.success) {
                    WebDav.saveConfig(
                            binding.url.getText().toString().trim(),
                            binding.username.getText().toString().trim(),
                            binding.password.getText().toString().trim(),
                            getPath());
                    updateStatus();
                }
                Notify.show(result.message);
            });
        });
    }

    private void onUpload(View view) {
        if (!checkInput()) return;
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.webdav_upload_confirm_title)
                .setMessage(R.string.webdav_upload_confirm_msg)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> doUpload()).show();
    }

    private void doUpload() {
        Notify.progress(getActivity());
        App.execute(() -> {
            WebDav.Result result = WebDav.upload();
            App.post(() -> {
                Notify.dismiss();
                Notify.show(result.message);
            });
        });
    }

    private void onDownload(View view) {
        if (!checkInput()) return;
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.webdav_download_confirm_title)
                .setMessage(R.string.webdav_download_confirm_msg)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> doDownload()).show();
    }

    private void doDownload() {
        Notify.progress(getActivity());
        App.execute(() -> {
            WebDav.Result result = WebDav.download();
            App.post(() -> {
                Notify.dismiss();
                Notify.show(result.message);
            });
        });
    }

    private void onClear(View view) {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.webdav_clear_confirm_title)
                .setMessage(R.string.webdav_clear_confirm_msg)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    WebDav.clearConfig();
                    binding.url.setText("");
                    binding.username.setText("");
                    binding.password.setText("");
                    binding.path.setText("/tv");
                    updateStatus();
                    Notify.show(R.string.webdav_clear_success);
                }).show();
    }
}
