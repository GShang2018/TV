package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogTrackerBinding;
import com.fongmi.android.tv.impl.TrackerCallback;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TrackerDialog {

    private final DialogTrackerBinding binding;
    private final AlertDialog dialog;
    private final TrackerCallback callback;

    public static TrackerDialog create(Activity activity) {
        return new TrackerDialog(activity);
    }

    public TrackerDialog(Activity activity) {
        this.callback = (TrackerCallback) activity;
        this.binding = DialogTrackerBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.setting_tracker)
                .setMessage(R.string.setting_tracker_hint)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> save())
                .create();
    }

    public void show() {
        initView();
        setDialog();
    }

    private void initView() {
        String trackerList = Setting.getTrackerList();
        binding.editText.setText(TextUtils.isEmpty(trackerList) ? getDefaultTrackers() : trackerList);
    }

    private String getDefaultTrackers() {
        return "udp://tracker.opentrackr.org:1337/announce\n" +
                "udp://tracker.openbittorrent.com:6969/announce\n" +
                "udp://tracker.torrent.eu.org:451/announce\n" +
                "udp://tracker.moeking.me:6969/announce\n" +
                "udp://exodus.desync.com:6969/announce\n" +
                "udp://open.demonii.com:1337/announce\n" +
                "udp://tracker.cyberia.is:6969/announce\n" +
                "udp://tracker.dler.org:6969/announce\n" +
                "https://tracker.nanoha.org:443/announce\n" +
                "https://tracker.lilithraws.org:443/announce\n" +
                "http://tracker.bt4g.com:2095/announce\n" +
                "http://tracker.files.fm:6969/announce";
    }

    private void save() {
        String text = binding.editText.getText().toString().trim();
        Setting.putTrackerList(text);
        callback.setTracker(text);
    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.8);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }
}
