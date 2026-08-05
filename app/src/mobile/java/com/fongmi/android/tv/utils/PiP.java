package com.fongmi.android.tv.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Rational;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.media3.ui.R;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.receiver.ActionReceiver;

import java.util.ArrayList;
import java.util.List;

public class PiP {

    private PictureInPictureParams.Builder builder;

    public static boolean noPiP() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !App.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private RemoteAction buildRemoteAction(Activity activity, @DrawableRes int icon, @StringRes int title, String action) {
        return new RemoteAction(Icon.createWithResource(activity, icon), activity.getString(title), "", ActionReceiver.getPendingIntent(activity, action));
    }

    private RemoteAction getPlayPauseAction(Activity activity, boolean play) {
        if (play) return buildRemoteAction(activity, R.drawable.exo_icon_pause, R.string.exo_controls_pause_description, ActionEvent.PAUSE);
        return buildRemoteAction(activity, R.drawable.exo_icon_play, R.string.exo_controls_play_description, ActionEvent.PLAY);
    }

    public PiP() {
        if (noPiP()) return;
        this.builder = new PictureInPictureParams.Builder();
    }

    public void update(Activity activity, View view) {
        if (noPiP()) return;
        Rect sourceRectHint = new Rect();
        view.getGlobalVisibleRect(sourceRectHint);
        builder.setSourceRectHint(sourceRectHint);
        try {
            activity.setPictureInPictureParams(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(Activity activity, boolean play) {
        if (noPiP()) return;
        try {
            activity.setPictureInPictureParams(builder.setActions(getActions(activity, play)).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private List<RemoteAction> getActions(Activity activity, boolean play) {
        List<RemoteAction> actions = new ArrayList<>();
        // 还原按钮放在最前面，确保在控件栏中优先显示
        actions.add(buildRemoteAction(activity, com.fongmi.android.tv.R.drawable.ic_control_full, com.fongmi.android.tv.R.string.pip_restore, ActionEvent.RESTORE));
        actions.add(getPlayPauseAction(activity, play));
        actions.add(buildRemoteAction(activity, R.drawable.exo_icon_previous, R.string.exo_controls_previous_description, ActionEvent.PREV));
        actions.add(buildRemoteAction(activity, R.drawable.exo_icon_next, R.string.exo_controls_next_description, ActionEvent.NEXT));
        return actions;
    }

    public void enter(Activity activity, int width, int height, int scale) {
        try {
            if (noPiP() || activity.isInPictureInPictureMode() || !Setting.isBackgroundPiP()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setSeamlessResizeEnabled(true);
            builder.setAspectRatio(getFixedRational(scale));
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 手动进入画中画（不检查后台画中画开关）
     */
    public void enterManually(Activity activity, int width, int height, int scale, boolean play) {
        try {
            if (noPiP() || activity.isInPictureInPictureMode()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setSeamlessResizeEnabled(true);
            builder.setAspectRatio(getFixedRational(scale));
            builder.setActions(getActions(activity, play));
            activity.setPictureInPictureParams(builder.build());
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 小窗保持固定比例，不随视频内容变化
     */
    private Rational getFixedRational(int scale) {
        if (scale == 2) return new Rational(4, 3);
        return new Rational(16, 9);
    }
}
