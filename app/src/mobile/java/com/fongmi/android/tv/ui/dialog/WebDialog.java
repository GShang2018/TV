package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WebDialog {

    private final AlertDialog dialog;

    public static WebDialog create(View view) {
        return new WebDialog(view);
    }

    public WebDialog(View view) {
        // 深色圆角容器承载 WebView，避免 Material 对话框默认浅色底（白条）与内容区不撑满（黑块）
        FrameLayout container = new FrameLayout(App.activity());
        container.setBackgroundResource(R.drawable.shape_web_dialog);
        container.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        this.dialog = new MaterialAlertDialogBuilder(App.activity()).setView(container).create();
        this.dialog.setOnDismissListener((DialogInterface.OnDismissListener) view);
    }

    public WebDialog show() {
        initDialog();
        return this;
    }

    public void dismiss() {
        dialog.setOnDismissListener(null);
        dialog.dismiss();
    }

    private void initDialog() {
        // 固定窗口尺寸：避免内容高度小导致弹窗过小，WebView 撑满容器
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.height = (int) (ResUtil.getScreenHeight() * 0.8f);
        params.width = (int) (ResUtil.getScreenWidth() * 0.85f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }
}
