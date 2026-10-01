package com.fongmi.android.tv.ui.base;

import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.media3.common.C;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.playback.PlaybackOrientation;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.utils.PiP;
import com.fongmi.android.tv.utils.Util;

/**
 * 点播与直播播放页的公共基类：统一方向策略、画中画（小窗）进出、横竖屏配置变更的处理流程。
 * 两页的布局渲染方式不同（点播多套 XML + tag，直播单套 XML + 动态 LayoutParams），
 * 这部分差异由子类通过 onAutoRotateToLand() / onOrientationMismatch() 等抽象方法实现，决策流程只此一份。
 */
public abstract class BasePlaybackActivity extends BaseActivity {

    protected Players mPlayers;
    protected PiP mPiP;
    protected Runnable mR0;

    protected abstract boolean isFullscreen();

    protected abstract boolean isLock();

    protected abstract boolean isRotate();

    protected abstract boolean isRedirect();

    protected abstract boolean isStop();

    protected abstract void setForeground(boolean foreground);

    protected abstract int getScale();

    protected abstract void onLock();

    protected abstract void hideControl();

    /** 当前是否为竖屏单列形态：点播按布局 tag 判断，直播按实际方向判断 */
    protected abstract boolean isPortMode();

    /** 画中画进入 / 退出时子类的额外处理（在公共处理之后调用） */
    protected abstract void onPiPChanged(boolean isInPictureInPictureMode);

    /** 非全屏、自动旋转开启、由竖屏转为横屏时的处理 */
    protected abstract void onAutoRotateToLand();

    /** 非全屏、布局形态与实际屏幕方向不匹配时的处理 */
    protected abstract void onOrientationMismatch(@NonNull Configuration newConfig);

    /** 全屏状态下发生旋转：点播维持沉浸式，直播重排分栏 */
    protected void onFullscreenRotate() {
        Util.hideSystemUI(this);
    }

    // 系统「自动旋转」（重力感应）是否开启
    protected boolean isAutoRotate() {
        return PlaybackOrientation.isAutoRotate(this);
    }

    // 方向策略：开启自动旋转时交还重力感应自由旋转，关闭时锁定当前形态，避免误触旋转
    protected void setOrient() {
        // 用户手动旋转过（竖屏全屏）时保留其选择，不覆盖
        if (isRotate()) return;
        setRequestedOrientation(PlaybackOrientation.getAutoRotateOrientation(this, isPortMode()));
    }

    protected int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) return getResources().getDimensionPixelSize(resourceId);
        return 0;
    }

    protected void stopService() {
        PlaybackService.stop();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isRedirect()) return;
        if (isLock()) App.post(this::onLock, 500);
        if (mPlayers.haveTrack(C.TRACK_TYPE_VIDEO)) mPiP.enter(this, mPlayers.getVideoWidth(), mPlayers.getVideoHeight(), getScale());
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            PlaybackService.start(mPlayers);
            hideControl();
        } else {
            App.post(mR0, 1000);
            setForeground(true);
            // 已停止则直接结束，避免 finish 后再去操作布局
            if (isStop()) {
                finish();
                return;
            }
        }
        // 子类各自的布局 / 弹幕 / 信息条处理放在公共处理之后，保证退出时能读到最新的全屏状态
        onPiPChanged(isInPictureInPictureMode);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 画中画小窗会触发窗口尺寸 / 方向变化（16:9 小窗被识别为横屏），
        // 不应据此进入全屏或重建，否则退出小窗后布局错乱
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        // 全屏状态下旋转：保持不重建，保住播放器与视频流
        if (isFullscreen()) {
            onFullscreenRotate();
            return;
        }
        // 非全屏：竖屏单列布局在自动旋转开启时转为横屏
        if (isAutoRotate() && isPortMode() && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onAutoRotateToLand();
            setOrient();
            return;
        }
        // 非全屏：布局形态与实际方向不匹配 → 子类决定如何切换（点播重建换布局，直播重排即可）
        onOrientationMismatch(newConfig);
        // 同步方向策略，使重力感应在旋转后继续生效
        setOrient();
    }
}
