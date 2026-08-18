package com.fongmi.android.tv.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityCrashBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;

import java.util.Objects;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class CrashActivity extends BaseActivity {

    private ActivityCrashBinding mBinding;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCrashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEvent() {
        mBinding.details.setOnClickListener(v -> showError());
        mBinding.restart.setOnClickListener(v -> CustomActivityOnCrash.restartApplication(this, Objects.requireNonNull(CustomActivityOnCrash.getConfigFromIntent(getIntent()))));
    }

    private void showError() {
        String errorDetails = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, getIntent());
        TextView textView = new TextView(this);
        textView.setText(errorDetails);
        textView.setTextIsSelectable(true);
        textView.setPadding(48, 24, 48, 24);
        textView.setTextSize(13);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        new AlertDialog.Builder(this)
                .setTitle(R.string.crash_details_title)
                .setView(scrollView)
                .setPositiveButton(R.string.crash_details_close, null)
                .setNeutralButton(R.string.crash_details_copy, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.crash_details_title), errorDetails));
                    }
                })
                .show();
    }
}
