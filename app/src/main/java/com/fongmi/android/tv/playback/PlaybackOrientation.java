package com.fongmi.android.tv.playback;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.provider.Settings;
import android.view.Surface;

import com.fongmi.android.tv.utils.ResUtil;

/**
 * 点播 / 直播播放页共用的方向决策：所有 screenOrientation 常量集中在此，
 * 避免在 VideoActivity / LiveActivity 各写一份导致两边行为漂移。
 */
public final class PlaybackOrientation {

    private PlaybackOrientation() {
    }

    /** 系统「自动旋转」（重力感应）是否开启 */
    public static boolean isAutoRotate(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    /** 当前屏幕实际朝向（锁定方向时使用，含反向横屏的判断） */
    public static int getScreenOrientation(Activity activity) {
        int rotation = ResUtil.getDisplay(activity).getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_90) return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    /** 旋转按钮：竖屏视频竖着播，否则交还重力感应横屏 */
    public static int getRotateOrientation(boolean portrait) {
        return portrait ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    }

    /** 进入全屏：竖屏视频保持竖屏全屏，其余转横屏全屏 */
    public static int getEnterFullscreenOrientation(boolean portraitVideo) {
        if (portraitVideo) return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    }

    /** 退出全屏：竖屏单列布局回竖屏，横屏分栏布局交还系统 */
    public static int getExitFullscreenOrientation(boolean portMode) {
        if (portMode) return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
    }

    /** 竖屏单列布局 + 自动旋转开启：自由旋转 */
    public static int getPortAutoRotateOrientation() {
        return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
    }

    /** 横屏分栏布局 + 自动旋转开启：只在横屏内旋转 */
    public static int getLandAutoRotateOrientation() {
        return ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
    }

    /** 竖屏视频尺寸 */
    public static int getPortraitVideoSizeOrientation() {
        return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
    }

    /** 横屏视频尺寸 */
    public static int getLandscapeVideoSizeOrientation() {
        return ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
    }

    /**
     * 锁定 / 解锁时的方向：锁定取当前实际朝向，未锁定按播放形态决定
     */
    public static int getLockOrientation(Activity activity, boolean lock, boolean rotate, boolean allowFullUser) {
        if (lock) return getScreenOrientation(activity);
        if (!rotate && allowFullUser) return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
        return getPlayerOrientation(activity, rotate);
    }

    public static int getPlayerOrientation(Context context, boolean rotate) {
        if (rotate || !ResUtil.isLand(context)) return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    }

    /**
     * 常规方向策略：开启自动旋转时交还重力感应（竖屏布局自由旋转、横屏布局仅在横屏内旋转），
     * 关闭时锁定当前形态，避免误触旋转
     */
    public static int getAutoRotateOrientation(Context context, boolean portMode) {
        if (!isAutoRotate(context)) return portMode ? getPortraitVideoSizeOrientation() : getLandscapeVideoSizeOrientation();
        return portMode ? getPortAutoRotateOrientation() : getLandAutoRotateOrientation();
    }
}
