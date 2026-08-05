package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.text.TextPaint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.animation.DecelerateInterpolator;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.bean.CastVideo;
import com.fongmi.android.tv.databinding.ActivityVideoBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.IjkUtil;
import com.fongmi.android.tv.ui.dialog.PlayerDialog;
import com.fongmi.android.tv.ui.dialog.SubtitleDialog;
import com.fongmi.android.tv.utils.Downloader;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.player.danmu.Parser;
import com.fongmi.android.tv.utils.Timer;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.adapter.FlagAdapter;
import com.fongmi.android.tv.ui.adapter.GalleryAdapter;
import com.fongmi.android.tv.ui.adapter.ParseAdapter;
import com.fongmi.android.tv.ui.adapter.QualityAdapter;
import com.fongmi.android.tv.ui.adapter.QuickAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.CustomMovement;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.CastDialog;
import com.fongmi.android.tv.ui.dialog.ControlDialog;
import com.fongmi.android.tv.ui.dialog.DanmuDialog;
import com.fongmi.android.tv.ui.dialog.EpisodeGridDialog;
import com.fongmi.android.tv.ui.dialog.EpisodeListDialog;
import com.fongmi.android.tv.ui.dialog.ImageDialog;
import com.fongmi.android.tv.ui.dialog.InfoDialog;
import com.fongmi.android.tv.ui.dialog.ReceiveDialog;
import com.fongmi.android.tv.ui.dialog.SourceChooseDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.IDMUtil;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PiP;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.bassaer.library.MDColor;
import com.github.catvod.utils.Trans;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class VideoActivity extends BaseActivity implements Clock.Callback, CustomKeyDownVod.Listener, TrackDialog.Listener, PlayerDialog.Listener, ControlDialog.Listener, FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener, QualityAdapter.OnClickListener, QuickAdapter.OnClickListener, ParseAdapter.OnClickListener, CastDialog.Listener, InfoDialog.Listener {


    private ActivityVideoBinding mBinding;
    private ViewGroup.LayoutParams mFrameParams;
    private ValueAnimator mAnimator;
    private Observer<Result> mObserveDetail;
    private Observer<Result> mObservePlayer;
    private Observer<Result> mObserveSearch;
    private Observer<Result> mObserveDownload;
    private DanmakuContext mDanmakuContext;
    private EpisodeAdapter mEpisodeAdapter;
    private QualityAdapter mQualityAdapter;
    private ControlDialog mControlDialog;
    private QuickAdapter mQuickAdapter;
    private ParseAdapter mParseAdapter;
    private CustomKeyDownVod mKeyDown;
    private ExecutorService mExecutor;
    private SiteViewModel mViewModel;
    private FlagAdapter mFlagAdapter;
    private GalleryAdapter mGalleryAdapter;
    private List<Dialog> mDialogs;
    private List<String> mBroken;
    private History mHistory;
    private Players mPlayers;
    private boolean foreground;
    private boolean fullscreen;
    private boolean initTrack;
    private boolean initAuto;
    private boolean autoMode;
    private boolean sourcePending;
    private boolean useParse;
    private boolean redirect;
    private boolean rotate;
    private boolean stop;
    private boolean lock;
    private int toggleCount;
    private int errorCount;
    private long mSavedPosition;
    // 旋转重建前保存的全屏状态：平板竖屏点全屏会旋转重建为横屏分栏布局，需据此自动恢复全屏
    private boolean mSavedFullscreen;
    private Runnable mR0;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private SourceChooseDialog mSourceDialog;
    private Runnable mSourceFinishRunnable;
    private Clock mClock;
    private PiP mPiP;

    public static void push(FragmentActivity activity, String text) {
        if (FileChooser.isValid(activity, Uri.parse(text))) file(activity, FileChooser.getPathFromUri(activity, Uri.parse(text)));
        else start(activity, Sniffer.getUrl(text));
    }

    public static void file(FragmentActivity activity, String path) {
        if (TextUtils.isEmpty(path)) return;
        String name = new File(path).getName();
        PermissionX.init(activity).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> start(activity, "push_agent", "file://" + path, name));
    }

    public static void cast(Activity activity, History history) {
        start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic());
    }

    public static void collect(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, true);
    }

    public static void download(Activity activity, String id, String name, String pic) {
        start(activity, "push_agent", id, name, pic, null, false, true);
    }

    public static void start(Activity activity, String url) {
        start(activity, "push_agent", url, url, null);
    }

    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, null, null, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean collect) {
        start(activity, key, id, name, pic, mark, collect, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean collect, boolean download) {
        Intent intent = new Intent(activity, VideoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("download", download);
        intent.putExtra("collect", collect);
        intent.putExtra("mark", mark);
        intent.putExtra("name", name);
        intent.putExtra("pic", pic);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    private String getName() {
        return Objects.toString(getIntent().getStringExtra("name"), "");
    }

    private String getPic() {
        return Objects.toString(getIntent().getStringExtra("pic"), "");
    }

    private String getMark() {
        return Objects.toString(getIntent().getStringExtra("mark"), "");
    }

    private String getKey() {
        return Objects.toString(getIntent().getStringExtra("key"), "");
    }

    private String getId() {
        return Objects.toString(getIntent().getStringExtra("id"), "");
    }

    private String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId()).concat(AppDatabase.SYMBOL) + VodConfig.getCid();
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private Flag getFlag() {
        return mFlagAdapter.getActivated();
    }

    private Episode getEpisode() {
        return mEpisodeAdapter.getActivated();
    }

    private int getPlayer() {
        return mHistory != null && mHistory.getPlayer() != -1 ? mHistory.getPlayer() : getSite().getPlayerType() != -1 ? getSite().getPlayerType() : Setting.getPlayer();
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : Setting.getScale();
    }

    private PlayerView getExo() {
        return mBinding.exo;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    private Drawable getDefaultArtwork() {
        if (mPlayers.isExo()) return getExo().getDefaultArtwork();
        return getIjk().getDefaultArtwork();
    }

    private boolean isReplay() {
        return Setting.getReset() == 1;
    }

    private boolean isFromCollect() {
        return getIntent().getBooleanExtra("collect", false);
    }

    private boolean isFromDownload() {
        return getIntent().getBooleanExtra("download", false);
    }

    private boolean isAutoRotate() {
        return Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    private boolean isLand() {
        return mBinding.getRoot().getTag().equals("land");
    }

    private boolean isPort() {
        return mBinding.getRoot().getTag().equals("port");
    }

    @Override
    protected boolean transparent() {
        return true;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVideoBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String id = Objects.toString(intent.getStringExtra("id"), "");
        if (TextUtils.isEmpty(id) || id.equals(getId())) return;
        mBinding.swipeLayout.setRefreshing(true);
        getIntent().putExtras(intent);
        stopSearch();
        sourcePending = false;
        setOrient();
        checkId();
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private void setPosterOutline() {
        // 利用 Android 原生阴影机制：elevation + ViewOutlineProvider。
        // 系统根据 outline 自动绘制柔和的弥散阴影，无需手动叠加渐变或半透明 View。
        // setClipToOutline(true) 仅将海报内容裁剪为圆角，不影响阴影绘制。
        mBinding.poster.setClipToOutline(true);
        mBinding.poster.setElevation(ResUtil.dp2px(8));
        mBinding.poster.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int radius = ResUtil.dp2px(8);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        // API 28+ 可自定义阴影颜色，加深阴影（默认阴影偏浅）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mBinding.poster.setOutlineAmbientShadowColor(Color.BLACK);
            mBinding.poster.setOutlineSpotShadowColor(Color.BLACK);
        }
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        // 旋转重建后，恢复销毁前同步保存的播放位置（onTimeChanged 落库是异步的，销毁瞬间可能尚未写入）
        mSavedPosition = savedInstanceState == null ? 0 : savedInstanceState.getLong("video_position", 0);
        mSavedFullscreen = savedInstanceState != null && savedInstanceState.getBoolean("video_fullscreen", false);
        mKeyDown = CustomKeyDownVod.create(this, mBinding.video);
        mFrameParams = mBinding.video.getLayoutParams();
        getWindow().setStatusBarColor(Color.BLACK);
        mBinding.getRoot().setPadding(0, getStatusBarHeight(), 0, 0);
        setPosterOutline();
        if (isPort()) {
            mBinding.video.post(() -> {
                int width = mBinding.video.getWidth();
                if (width > 0) {
                    mFrameParams.height = width * 9 / 16;
                    mBinding.video.setLayoutParams(mFrameParams);
                }
            });
        }
        setAnimator();
        mDanmakuContext = DanmakuContext.create();
        mBinding.progressLayout.showProgress();
        mBinding.swipeLayout.setEnabled(false);
        mObserveDetail = this::setDetail;
        mObservePlayer = this::setPlayer;
        mObserveDownload = this::setDownload;
        mObserveSearch = this::setSearch;
        mPlayers = Players.create(this);
        mDialogs = new ArrayList<>();
        mBroken = new ArrayList<>();
        mClock = Clock.create(Arrays.asList(mBinding.display.clock, mBinding.control.time));
        mR0 = this::stopService;
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mR3 = this::setOrient;
        mR4 = this::showEmpty;
        // 播放源弹窗检索结束：关闭加载动画（结果已全部回填，或兜底超时）
        mSourceDialog = null;
        mSourceFinishRunnable = () -> {
            sourcePending = false;
            if (mSourceDialog != null) mSourceDialog.finish();
        };
        mPiP = new PiP();
        setForeground(true);
        setRecyclerView();
        setVideoView();
        setDisplayView();
        setDanmuView();
        setViewModel();
        showProgress();
        checkId();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.poster.setOnClickListener(view -> showPoster());
        mBinding.more.setOnClickListener(view -> onMore());
        mBinding.actor.setOnClickListener(view -> onActor());
        mBinding.content.setOnClickListener(view -> onContent());
        mBinding.reverse.setOnClickListener(view -> onReverse());
        mBinding.download.setOnClickListener(view -> onDownload());
        mBinding.currentSite.setOnClickListener(view -> onSource());
        mBinding.name.setOnLongClickListener(view -> onChange());
        mBinding.content.setOnLongClickListener(view -> onCopy());
        mBinding.cast.setOnClickListener(view -> onCast());
        mBinding.control.info.setOnClickListener(view -> onInfo());
        mBinding.control.refresh.setOnClickListener(view -> onRefresh());
        mBinding.control.pip.setOnClickListener(view -> onPiP());
        mBinding.share.setOnClickListener(view -> onShareClick());
        mBinding.control.full.setOnClickListener(view -> onFull());
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.control.danmu.setOnClickListener(view -> onDanmu());
        mBinding.control.danmuSetting.setOnClickListener(view -> onDanmuSetting());
        mBinding.control.play.setOnClickListener(view -> checkPlay());
        mBinding.control.next.setOnClickListener(view -> checkNext());
        mBinding.control.prev.setOnClickListener(view -> checkPrev());
        mBinding.setting.setOnClickListener(view -> onSetting());
        mBinding.control.title.setOnLongClickListener(view -> onChange());
        mBinding.control.backTop.setOnClickListener(view -> onBack());
        mBinding.control.right.lock.setOnClickListener(view -> onLock());
        mBinding.control.right.rotate.setOnClickListener(view -> onRotate());
        mBinding.control.action.text.setOnClickListener(this::onTrack);
        mBinding.control.action.audio.setOnClickListener(this::onTrack);
        mBinding.control.action.video.setOnClickListener(this::onTrack);
        mBinding.control.action.loop.setOnClickListener(view -> onLoop());
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.action.reset.setOnClickListener(view -> onReset());
        mBinding.control.action.player.setOnClickListener(view -> onPlayer());
        mBinding.control.action.decode.setOnClickListener(view -> onDecode());
        mBinding.control.action.ending.setOnClickListener(view -> onEnding());
        mBinding.control.action.opening.setOnClickListener(view -> onOpening());
        mBinding.control.action.episodes.setOnClickListener(view -> onEpisodes());
        mBinding.control.action.text.setOnLongClickListener(view -> onTextLong());
        mBinding.control.action.player.setOnLongClickListener(view -> onChoose());
        mBinding.control.action.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.action.reset.setOnLongClickListener(view -> onResetToggle());
        mBinding.control.action.ending.setOnLongClickListener(view -> onEndingReset());
        mBinding.control.action.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        mBinding.control.action.getRoot().setOnTouchListener(this::onActionTouch);
        mBinding.swipeLayout.setOnRefreshListener(this::onSwipeRefresh);
        mBinding.control.seek.setListener(mPlayers);
    }

    private void setRecyclerView() {
        mBinding.flag.setHasFixedSize(true);
        mBinding.flag.setItemAnimator(null);
        mBinding.flag.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.flag.setAdapter(mFlagAdapter = new FlagAdapter(this));
        mQuickAdapter = new QuickAdapter(this);
        mBinding.episode.setHasFixedSize(true);
        mBinding.episode.setItemAnimator(null);
        mBinding.episode.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.episode.setAdapter(mEpisodeAdapter = new EpisodeAdapter(this, ViewType.HORI));
        mBinding.quality.setHasFixedSize(true);
        mBinding.quality.setItemAnimator(null);
        mBinding.quality.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.quality.setAdapter(mQualityAdapter = new QualityAdapter(this));
        mBinding.gallery.setHasFixedSize(true);
        mBinding.gallery.setItemAnimator(null);
        mBinding.gallery.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.gallery.setAdapter(mGalleryAdapter = new GalleryAdapter(position -> openGallery(mGalleryAdapter.getItems(), position)));
        mBinding.control.parse.setHasFixedSize(true);
        mBinding.control.parse.setItemAnimator(null);
        mBinding.control.parse.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.control.parse.setAdapter(mParseAdapter = new ParseAdapter(this, ViewType.DARK));
    }

    private void setPlayerView() {
        getIjk().setPlayer(mPlayers.getPlayer());
        mBinding.control.action.player.setText(mPlayers.getPlayerText());
        mBinding.control.action.speed.setEnabled(mPlayers.canAdjustSpeed());
        mBinding.control.action.speed.setText(mPlayers.setSpeed(mHistory.getSpeed()));
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
        if (mControlDialog != null && mControlDialog.isVisible()) mControlDialog.updatePlayer();
    }

    private void setDecodeView() {
        mBinding.control.action.decode.setText(mPlayers.getDecodeText());
        if (mControlDialog != null && mControlDialog.isVisible()) mControlDialog.updateDecode();
    }

    private void setVideoView() {
        mPlayers.init(getExo(), getIjk());
        ExoUtil.setSubtitleView(mBinding.exo);
        IjkUtil.setSubtitleView(mBinding.ijk);
        if (isPort() && ResUtil.isLand(this)) {
            enterFullscreen();
        } else if (mSavedFullscreen && !isFullscreen()) {
            // 平板竖屏点全屏旋转重建后，自动进入全屏（横屏全屏时隐藏右侧分栏，video 铺满），避免停留在横屏分栏布局
            enterFullscreen();
        }
        mBinding.control.action.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
        mBinding.video.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> mPiP.update(getActivity(), view));
    }

    private void setVideoView(boolean isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            if (mAnimator != null) mAnimator.cancel();
            mBinding.video.setLayoutParams(getFullscreenParams());
        } else {
            // 退出 PiP：恢复正确的 16:9 播放框高度，避免 mAnimator 中途取消导致的高度残留
            if (isPort() && mBinding.video.getWidth() > 0) {
                mFrameParams.height = mBinding.video.getWidth() * 9 / 16;
            }
            mBinding.video.setLayoutParams(mFrameParams);
        }
    }

    // 生成铺满屏幕的 LayoutParams：根据 video 父容器类型（竖屏 RelativeLayout / 横屏 LinearLayout）生成匹配参数，避免 ClassCastException
    private ViewGroup.LayoutParams getFullscreenParams() {
        ViewGroup parent = (ViewGroup) mBinding.video.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.weight = 0;
            return params;
        } else if (parent instanceof RelativeLayout) {
            return new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    public void setDanmuViewSettings() {
        float[] range = {2.4f, 1.8f, 1.2f, 0.8f};
        float speed = range[Setting.getDanmuSpeed()];
        float alpha = Setting.getDanmuAlpha() / 100.0f;
        float sizeScale = Setting.getDanmuSize();
        int maxLine = Setting.getDanmuLine(2);
        HashMap<Integer, Integer> maxLines = new HashMap<>();
        maxLines.put(BaseDanmaku.TYPE_FIX_TOP, maxLine);
        maxLines.put(BaseDanmaku.TYPE_SCROLL_RL, maxLine);
        maxLines.put(BaseDanmaku.TYPE_SCROLL_LR, maxLine);
        maxLines.put(BaseDanmaku.TYPE_FIX_BOTTOM, maxLine);
        mDanmakuContext.setMaximumLines(maxLines).setScrollSpeedFactor(speed).setDanmakuTransparency(alpha).setScaleTextSize(sizeScale);
    }

    private void setDanmuView() {
        mPlayers.setDanmuView(mBinding.danmaku);
        setDanmuViewSettings();
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setDanmakuMargin(8);
        checkDanmuImg();
    }

    private void setDisplayView() {
        mBinding.display.getRoot().setVisibility(View.VISIBLE);
        showDisplayInfo();
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.action.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observeForever(mObserveDetail);
        mViewModel.player.observeForever(mObservePlayer);
        mViewModel.search.observeForever(mObserveSearch);
        mViewModel.download.observeForever(mObserveDownload);
        mViewModel.episode.observe(this, episode -> {
            onItemClick(episode);
            hideSheet();
        });
        mViewModel.ep.observe(this, episode -> {
            Notify.progress(this);
            Downloader.get().title(mBinding.name.getText() + "-" + episode.getName());
            mViewModel.download(getKey(), getFlag().getFlag(), episode.getUrl());
        });
    }

    private void checkId() {
        if (getId().startsWith("push://")) getIntent().putExtra("key", "push_agent").putExtra("id", getId().substring(7));
        if (getId().isEmpty() || getId().startsWith("msearch:")) setEmpty(false);
        else getDetail();
    }

    private void getDetail() {
        mViewModel.detailContent(getKey(), getId());
    }

    private void getDetail(Vod item) {
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("pic", item.getVodPic());
        getIntent().putExtra("id", item.getVodId());
        mBinding.swipeLayout.setRefreshing(true);
        mBinding.swipeLayout.setEnabled(false);
        mBinding.scroll.scrollTo(0, 0);
        mClock.setCallback(null);
        mPlayers.reset();
        mPlayers.stop();
        getDetail();
    }

    private void setDetail(Result result) {
        mBinding.swipeLayout.setRefreshing(false);
        if (result.getList().isEmpty()) setEmpty(result.hasMsg());
        else setDetail(result.getList().get(0));
        Notify.show(result.getMsg());
    }

    private void setEmpty(boolean finish) {
        if (isFromCollect() || finish) {
            finish();
        } else if (getName().isEmpty()) {
            showEmpty();
        } else {
            mBinding.name.setText(getName());
            App.post(mR4, 10000);
            checkSearch(false);
        }
    }

    private void showEmpty() {
        showError(getString(R.string.error_detail));
        mBinding.swipeLayout.setEnabled(true);
        mBinding.progressLayout.showEmpty();
        stopSearch();
        sourcePending = false;
    }

    private void setDetail(Vod item) {
        mBinding.progressLayout.showContent();
        if (isFromDownload()) item.setVodName("");
        if (isFromDownload()) item.setVodPic("");
        mBinding.video.setTag(item.getVodPic(getPic()));
        mBinding.name.setText(item.getVodName(getName()));
        Downloader.get().image(item.getVodPic());
        setPoster(item.getVodPic());
        setText(mBinding.remark, 0, item.getVodRemarks());
        mBinding.currentSite.setText(getSite().getName());
        setText(mBinding.content, 0, Html.fromHtml(removeImg(item.getVodContent())));
        setText(mBinding.actor, R.string.detail_actor, Html.fromHtml(removeImg(item.getVodActor())));
        setText(mBinding.director, R.string.detail_director, Html.fromHtml(removeImg(item.getVodDirector())));
        mBinding.contentLayout.setVisibility(mBinding.content.getVisibility());
        mFlagAdapter.addAll(item.getVodFlags());
        setOther(item);
        setArtwork(item.getVodPic());
        setGallery(item);
        App.removeCallbacks(mR4);
        checkHistory(item);
        checkFlag(item);
        checkKeepImg();
        checkActionBar();
    }

    private void checkActionBar() {
        mBinding.setting.setVisibility(mHistory == null || isFullscreen() ? View.GONE : View.VISIBLE);
        mBinding.keep.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.control.info.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.share.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.cast.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
    }

    private void setText(TextView view, int resId, CharSequence text) {
        if (TextUtils.isEmpty(text)) text = getString(R.string.detail_na);
        view.setText(getSpan(resId, text), TextView.BufferType.SPANNABLE);
        view.setVisibility(View.VISIBLE);
        view.setLinkTextColor(Color.WHITE);
        CustomMovement.bind(view);
        view.setTag(text.toString());
    }

    // 移除简介中的 <img> 标签，避免 Html.fromHtml 将其渲染成占位小方块
    private String removeImg(String text) {
        if (TextUtils.isEmpty(text)) return text;
        return text.replaceAll("(?i)<img[^>]*>", "");
    }

    private SpannableStringBuilder getSpan(int resId, CharSequence text) {
        if (resId > 0) text = getString(resId, text);
        SpannableStringBuilder span = new SpannableStringBuilder(text);
        String str = text.toString();
        Map<String, String> map = new HashMap<>();
        Matcher m = Sniffer.CLICKER.matcher(str);
        while (m.find()) {
            String key = Trans.s2t(m.group(2)).trim();
            str = str.replace(m.group(), key);
            map.put(key, m.group(1));
        }
        if (!map.isEmpty()) {
            span = new SpannableStringBuilder(str);
            for (String s : map.keySet()) {
                int index = str.indexOf(s);
                Result result = Result.type(map.get(s));
                span.setSpan(getClickSpan(result), index, index + s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return span;
    }

    private ClickableSpan getClickSpan(Result result) {
        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                FolderActivity.start(getActivity(), getKey(), result);
                ((TextView) view).setMaxLines(Integer.MAX_VALUE);
                setRedirect(true);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setUnderlineText(true);
            }
        };
    }

    private void setOther(Vod item) {
        setText(mBinding.otherYear, R.string.detail_year, item.getVodYear());
        setText(mBinding.otherArea, R.string.detail_area, item.getVodArea());
        setText(mBinding.otherType, R.string.detail_type, item.getTypeName());
    }

    private void setGallery(Vod item) {
        List<String> items = item.getGallery();
        if (items.isEmpty()) {
            mBinding.galleryLayout.setVisibility(View.GONE);
        } else {
            mBinding.galleryLayout.setVisibility(View.VISIBLE);
            mBinding.galleryAll.setVisibility(View.VISIBLE);
            mBinding.galleryAll.setText(getString(R.string.detail_gallery_all, items.size()));
            ArrayList<String> urls = new ArrayList<>(items);
            mBinding.galleryAll.setOnClickListener(v -> GalleryGridActivity.start(this, urls));
            mGalleryAdapter.addAll(items);
        }
    }

    private void openGallery(List<String> items, int position) {
        GalleryActivity.start(this, new ArrayList<>(items), position);
    }

    private void setPoster(String url) {
        ImgUtil.loadPoster("", url, mBinding.poster);
    }

    private void showPoster() {
        String url = Objects.toString(mBinding.video.getTag(), "");
        if (!TextUtils.isEmpty(url)) {
            ArrayList<String> urls = new ArrayList<>();
            urls.add(url);
            GalleryActivity.start(this, urls, 0, mBinding.name.getText().toString());
        }
    }

    private void showImage(String url) {
        ImageDialog.create(this).url(url).show();
    }

    private void getPlayer(Flag flag, Episode episode, boolean replay) {
        mBinding.control.title.setText(getString(R.string.detail_title, mBinding.name.getText(), episode.getName()));
        mBinding.control.title.setSelected(true);
        mBinding.display.title.setText(mBinding.control.title.getText());
        mViewModel.playerContent(getKey(), flag.getFlag(), episode.getUrl());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateHistory(episode, replay);
        mPlayers.clear();
        mPlayers.stop();
        // 只有磁力链接才设置 originalUrl，非磁力链接使用 getUrl() 即 InfoDialog 中的链接
        // Episode.getOriginalUrl() 在 originalUrl 为 null 时会回退到 url，所以需要判断是否真的是磁力链接
        String epOriginalUrl = episode.getOriginalUrl();
        if (epOriginalUrl != null && epOriginalUrl.startsWith("magnet:")) {
            mPlayers.setOriginalUrl(epOriginalUrl);
        } else {
            mPlayers.setOriginalUrl(null);
        }
        showProgress();
        setMetadata();
        hidePreview();
    }

    private void setPlayer(Result result) {
        result.getUrl().set(mQualityAdapter.getPosition());
        setUseParse(VodConfig.hasParse() && ((result.getPlayUrl().isEmpty() && VodConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1));
        if (mControlDialog != null && mControlDialog.isVisible()) mControlDialog.setParseVisible(isUseParse());
        mBinding.control.parse.setVisibility(isFullscreen() && isUseParse() ? View.VISIBLE : View.GONE);
        mPlayers.start(result, isUseParse(), getSite().isChangeable() ? getSite().getTimeout() : -1);
        setQualityVisible(result.getUrl().isMulti());
        mBinding.swipeLayout.setRefreshing(false);
        checkDanmu(result.getDanmaku());
        mQualityAdapter.addAll(result);
    }

    private void setDownload(Result result) {
        Downloader.get().result(result).start(this);
    }

    private void checkDanmu(String danmu) {
        mBinding.danmaku.release();
        if (!Setting.isDanmuLoad() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode())) return;
        mBinding.danmaku.setVisibility(danmu.isEmpty() ? View.GONE : View.VISIBLE);
        if (danmu.length() > 0) App.execute(() -> mBinding.danmaku.prepare(new Parser(danmu), mDanmakuContext));
    }

    @Override
    public void onItemClick(Flag item) {
        if (item.isActivated()) return;
        mFlagAdapter.setActivated(item);
        mBinding.flag.scrollToPosition(mFlagAdapter.getPosition());
        setEpisodeAdapter(item.getEpisodes());
        setQualityVisible(false);
        seamless(item);
    }

    @Override
    public void onItemClick(Episode item) {
        if (shouldEnterFullscreen(item)) return;
        mFlagAdapter.toggle(item);
        notifyItemChanged(mEpisodeAdapter);
        mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
        onRefresh();
    }

    @Override
    public void onItemClick(Result result) {
        try {
            mPlayers.start(result, isUseParse(), getSite().isChangeable() ? getSite().getTimeout() : -1);
            mBinding.danmaku.hide();
        } catch (Exception e) {
            ErrorEvent.extract(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(Vod item) {
        // 切换播放源：停止后台检索避免继续回填/浪费流量，保留已检索到的候选结果
        mSourceDialog = null;
        stopSearch();
        sourcePending = false;
        setAutoMode(false);
        getDetail(item);
    }

    @Override
    public void onItemClick(Parse item) {
        setParse(item);
        onRefresh();
    }

    private void setParse(Parse item) {
        VodConfig.get().setParse(item);
        notifyItemChanged(mParseAdapter);
        if (mControlDialog != null && mControlDialog.isVisible()) mControlDialog.updateParse();
    }

    private void setEpisodeAdapter(List<Episode> items) {
        mBinding.control.action.episodes.setVisibility(items.size() < 2 ? View.GONE : View.VISIBLE);
        mBinding.control.nextRoot.setVisibility(items.size() < 2 ? View.GONE : View.VISIBLE);
        mBinding.control.prevRoot.setVisibility(items.size() < 2 ? View.GONE : View.VISIBLE);
        mBinding.episode.setVisibility(items.size() == 0 ? View.GONE : View.VISIBLE);
        mBinding.reverse.setVisibility(items.size() < 2 ? View.GONE : View.VISIBLE);
        mBinding.more.setVisibility(items.size() < 3 ? View.GONE : View.VISIBLE);
        mEpisodeAdapter.addAll(items);
    }

    private void seamless(Flag flag) {
        Episode episode = flag.find(mHistory.getVodRemarks(), getMark().isEmpty());
        setQualityVisible(episode != null && episode.isActivated() && mQualityAdapter.getItemCount() > 1);
        if (episode == null || episode.isActivated()) return;
        if (Setting.getFlag() == 1) {
            episode.setSelected(true);
            mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition(episode));
        } else {
            mHistory.setVodRemarks(episode.getName());
            onItemClick(episode);
            hidePreview();
        }
    }

    private void setQualityVisible(boolean visible) {
        mBinding.qualityText.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBinding.quality.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void reverseEpisode(boolean scroll) {
        mFlagAdapter.reverse();
        setEpisodeAdapter(getFlag().getEpisodes());
        if (scroll) mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
    }

    private void onMore() {
        EpisodeGridDialog.create().reverse(mHistory.isRevSort()).episodes(mEpisodeAdapter.getItems()).show(this);
    }

    private void onSource() {
        // 点击立即打开弹窗（无结果时显示加载动画），结果实时回填，避免静默等待让人以为无法弹窗
        openSourceDialog();
        if (mQuickAdapter.isEmpty() && !sourcePending) {
            // 尚无检索结果：触发播放源检索，检索结果实时回填弹窗
            sourcePending = true;
            initSearch(mBinding.name.getText().toString(), false);
        }
    }

    private void openSourceDialog() {
        if (mSourceDialog != null && mSourceDialog.isVisible()) {
            mSourceDialog.refresh(mQuickAdapter.getItems(), getSelected());
            return;
        }
        mSourceDialog = SourceChooseDialog.create().items(mQuickAdapter.getItems()).selected(getSelected()).listener(item -> onItemClick(item)).show(this);
    }

    private int getSelected() {
        int selected = -1;
        List<Vod> items = mQuickAdapter.getItems();
        String key = getSite().getKey();
        for (int i = 0; i < items.size(); i++) if (key.equals(items.get(i).getSiteKey())) selected = i;
        return selected;
    }

    private void onDownload() {
        EpisodeGridDialog.create().reverse(mHistory.isRevSort()).episodes(mEpisodeAdapter.getItems()).download(true).show(this);
    }

    private void onActor() {
        mBinding.actor.setMaxLines(mBinding.actor.getMaxLines() == 1 ? Integer.MAX_VALUE : 1);
    }

    private void onContent() {
        mBinding.content.setMaxLines(mBinding.content.getMaxLines() == 2 ? Integer.MAX_VALUE : 2);
    }

    private void onReverse() {
        mHistory.setRevSort(!mHistory.isRevSort());
        reverseEpisode(false);
    }

    private boolean onChange() {
        checkSearch(true);
        return true;
    }

    private boolean onCopy() {
        Util.copy(mBinding.content.getText().toString());
        return true;
    }

    private void onCast() {
        CastDialog.create().history(mHistory).video(CastVideo.get(mBinding.name.getText().toString(), mPlayers.getUrl())).fm(true).show(this);
    }

    private void onInfo() {
        InfoDialog.create(this).title(mBinding.control.title.getText()).headers(mPlayers.getHeaders()).url(mPlayers.getUrl()).show();
    }

    private void onShareClick() {
        // 分享格式化文本：我正在看[标题]，观看链接: [链接]
        try {
            if (mPlayers.isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String title = mBinding.control.title.getText().toString();
            String shareUrl = mPlayers.getOriginalUrl() != null ? mPlayers.getOriginalUrl() : mPlayers.getUrl();
            boolean isM3u8 = shareUrl != null && shareUrl.toLowerCase().endsWith(".m3u8");
            String prefix = isM3u8 ?  "https://m3u8-player.cc/player?url=" : "";
            String shareText = "我正在看" + title + "，观看链接: " + prefix+ shareUrl;
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            intent.putExtra("extra_headers", mPlayers.getHeaderBundle());
            intent.putExtra("title", title);
            intent.putExtra("name", title);
            intent.setType("text/plain");
            startActivity(Util.getChooser(intent));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onFull() {
        setR1Callback();
        toggleFullscreen();
    }

    private void onBack() {
        if (isFullscreen()) exitFullscreen();
        else finish();
    }

    private void onKeep() {
        Keep keep = Keep.find(getHistoryKey());
        Notify.show(keep != null ? R.string.keep_del : R.string.keep_add);
        if (keep != null) keep.delete();
        else createKeep();
        RefreshEvent.keep();
        checkKeepImg();
    }

    private void onDanmu() {
        Setting.putDanmu(!Setting.isDanmu());
        checkDanmuImg();
        showDanmu();
    }

    private void onDanmuSetting() {
        DanmuDialog.create().show(this);
    }

    private void showDanmu() {
        if (Setting.isDanmu()) mBinding.danmaku.show();
        else mBinding.danmaku.hide();
    }

    private void checkPlay() {
        setR1Callback();
        if (mPlayers.isPlaying()) onPaused();
        else if (mPlayers.isEmpty()) onRefresh();
        else onPlay();
    }

    private void checkNext() {
        setR1Callback();
        Episode item = mEpisodeAdapter.getNext();
        if (item.isActivated()) Notify.show(R.string.error_play_next);
        else onItemClick(item);
    }

    private void checkPrev() {
        setR1Callback();
        Episode item = mEpisodeAdapter.getPrev();
        if (item.isActivated()) Notify.show(R.string.error_play_prev);
        else onItemClick(item);
    }

    private void onSetting() {
        mControlDialog = ControlDialog.create().parent(mBinding).history(mHistory).player(mPlayers).parse(isUseParse()).show(this);
    }

    private void onLock() {
        setLock(!isLock());
        setRequestedOrientation(getLockOrient());
        mKeyDown.setLock(isLock());
        checkLockImg();
        showControl();
    }

    private void onRotate() {
        setR1Callback();
        setRotate(!isRotate());
        setRequestedOrientation(ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void onTrack(View view) {
        TrackDialog.create().player(mPlayers).vod(true).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
    }

    private void onLoop() {
        mBinding.control.action.loop.setActivated(!mBinding.control.action.loop.isActivated());
    }

    private void onScale() {
        int index = getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        mHistory.setScale(index = index == array.length - 1 ? 0 : ++index);
        setScale(index);
        setR1Callback();
    }

    private void onSpeed() {
        mBinding.control.action.speed.setText(mPlayers.addSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
        setR1Callback();
    }

    private boolean onSpeedLong() {
        mBinding.control.action.speed.setText(mPlayers.toggleSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
        setR1Callback();
        return true;
    }

    private void onRefresh() {
        onReset(false);
    }

    private void onReset() {
        onReset(isReplay());
    }

    private void onReset(boolean replay) {
        mClock.setCallback(null);
        if (mFlagAdapter.isEmpty()) return;
        if (mEpisodeAdapter.isEmpty()) return;
        getPlayer(getFlag(), getEpisode(), replay);
    }

    private boolean onResetToggle() {
        Setting.putReset(Math.abs(Setting.getReset() - 1));
        mBinding.control.action.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
        return true;
    }

    private void onPlayer() {
        PlayerDialog.create().select(mPlayers.getPlayer()).title(getName()).show(this);
        hideControl();
    }

    private void onDecode() {
        onDecode(true);
    }

    private void onDecode(boolean save) {
        mPlayers.toggleDecode(save);
        mPlayers.init(getExo(), getIjk());
        mPlayers.setMediaSource();
        setDecodeView();
        setR1Callback();
    }

    private void onEnding() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current < duration / 2) return;
        mHistory.setEnding(duration - current);
        mBinding.control.action.ending.setText(mPlayers.stringToTime(mHistory.getEnding()));
        setR1Callback();
    }

    private boolean onEndingReset() {
        mHistory.setEnding(0);
        mBinding.control.action.ending.setText(R.string.play_ed);
        setR1Callback();
        return true;
    }

    private void onOpening() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current > duration / 2) return;
        mHistory.setOpening(current);
        mBinding.control.action.opening.setText(mPlayers.stringToTime(mHistory.getOpening()));
        setR1Callback();
    }

    private boolean onOpeningReset() {
        mHistory.setOpening(0);
        mBinding.control.action.opening.setText(R.string.play_op);
        setR1Callback();
        return true;
    }

    private void onEpisodes() {
        mDialogs.add(EpisodeListDialog.create(this).episodes(mEpisodeAdapter.getItems()).show());
    }

    private boolean onChoose() {
        if (mPlayers.isEmpty()) return false;
        mPlayers.choose(this, mBinding.control.title.getText());
        setRedirect(true);
        return true;
    }

    private boolean onTextLong() {
        onSubtitleClick();
        return true;
    }

    private boolean onActionTouch(View v, MotionEvent e) {
        setR1Callback();
        return false;
    }

    private void onSwipeRefresh() {
        if (mBinding.progressLayout.isEmpty()) getDetail();
        else onRefresh();
    }

    private void showDisplayInfo() {
        boolean pictureMode = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) pictureMode = true;
        boolean hasDialog = false;
        for (Fragment f : getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) hasDialog = true;
        boolean controlVisible = isVisible(mBinding.control.getRoot());
        boolean visible = (!controlVisible || isLock()) && !pictureMode && !hasDialog;
        mBinding.display.clock.setVisibility(Setting.isDisplayTime() && visible  ? View.VISIBLE : View.GONE);
        mBinding.display.netspeed.setVisibility(Setting.isDisplaySpeed() && visible ? View.VISIBLE : View.GONE);
        mBinding.display.duration.setVisibility(Setting.isDisplayDuration() && visible && (mPlayers.isVod()) ? View.VISIBLE : View.GONE);
        mBinding.display.progress.setVisibility(Setting.isDisplayMiniProgress() && visible && (mPlayers.isVod()) ? View.VISIBLE : View.GONE);
        mBinding.display.titleLayout.setVisibility(Setting.isDisplayVideoTitle()&& visible ? View.VISIBLE : View.GONE);
    }

    private void onTimeChangeDisplaySpeed() {
        boolean controlVisible = isVisible(mBinding.control.getRoot());
        boolean visible = (!controlVisible || isLock());
        long position = mPlayers.getPosition();
        if (Setting.isDisplaySpeed() && visible) Traffic.setSpeed(mBinding.display.netspeed);
        if (Setting.isDisplayDuration() && visible && position > 0) mBinding.display.duration.setText(mPlayers.getPositionTime(0) + "/" + mPlayers.getDurationTime());
        if (Setting.isDisplayMiniProgress() && visible && position > 0 && (mPlayers.isVod())) mBinding.display.progress.setProgress((int)(position * 100 / mPlayers.getDuration()));
        showDisplayInfo();
    }

    private void toggleFullscreen() {
        if (isFullscreen()) exitFullscreen();
        else enterFullscreen();
    }

    private boolean shouldEnterFullscreen(Episode item) {
        boolean enter = !isFullscreen() && item.isActivated();
        if (enter) enterFullscreen();
        return enter;
    }

    private void setAnimator() {
        mAnimator = new ValueAnimator();
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(animation -> {
            if (isFullscreen() || isInPictureInPictureMode()) return;
            mFrameParams.height = (int) animation.getAnimatedValue();
            mBinding.video.setLayoutParams(mFrameParams);
        });
    }

    // 平滑过渡视频框高度：参考同源项目 webtv 的 ValueAnimator 动画，避免全屏切换时高度跳变
    private void animateFrame(int from, int to) {
        if (mAnimator == null) setAnimator();
        mAnimator.cancel();
        mAnimator.setIntValues(from, to);
        mAnimator.setDuration(200);
        mAnimator.start();
    }

    private void enterFullscreen() {
        if (isFullscreen()) return;
        mBinding.video.setClipToOutline(false);
        mBinding.getRoot().setPadding(0, 0, 0, 0);
        if (mBinding.infoContainer != null) mBinding.infoContainer.setVisibility(View.GONE);
        App.post(() -> {
            if (mAnimator != null) mAnimator.cancel();
            mBinding.video.setLayoutParams(getFullscreenParams());
        }, 50);
        setRequestedOrientation(mPlayers.isPortrait() ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        mBinding.control.full.setVisibility(View.GONE);
        mDanmakuContext.setScaleTextSize(1.0f * Setting.getDanmuSize());
        setRotate(mPlayers.isPortrait(), true);
        Util.hideSystemUI(this);
        App.post(mR3, 2000);
        hideControl();
    }

    private void exitFullscreen() {
        if (!isFullscreen()) return;
        mBinding.video.setClipToOutline(true);
        mBinding.getRoot().setPadding(0, getStatusBarHeight(), 0, 0);
        setRequestedOrientation(isPort() ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
        mBinding.control.full.setVisibility(View.VISIBLE);
        if (mBinding.infoContainer != null) mBinding.infoContainer.setVisibility(View.VISIBLE);
        if (isLand()) {
            // 横屏：video 高度本就铺满，直接恢复 weight 布局参数
            mBinding.video.setLayoutParams(mFrameParams);
        } else {
            // 竖屏：从当前高度平滑过渡回 16:9 播放框
            animateFrame(mBinding.video.getHeight(), mFrameParams.height);
        }
        mDanmakuContext.setScaleTextSize(0.8f * Setting.getDanmuSize());
        setRotate(false, false);
        Util.showSystemUI(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        App.post(mR3, 2000);
        hideControl();
    }

    private int getLockOrient() {
        if (isLock()) {
            return ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        } else if (isRotate()) {
            return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        } else if (isPort() && isAutoRotate()) {
            return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
        } else {
            return ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        }
    }

    private void showProgress() {
        mBinding.widget.progress.setVisibility(View.VISIBLE);
        App.post(mR2, 0);
        hideError();
    }

    private void hideProgress() {
        mBinding.widget.progress.setVisibility(View.GONE);
        App.removeCallbacks(mR2);
        Traffic.reset();
    }

    private void showError(String text) {
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
    }

    private void showControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        mBinding.control.danmu.setVisibility(isLock() || !mBinding.danmaku.isPrepared() ? View.GONE : View.VISIBLE);
        mBinding.control.danmuSetting.setVisibility(isLock() || !Setting.isDanmuLoad() || !isVisible(mBinding.danmaku) ? View.GONE : View.VISIBLE);
        mBinding.setting.setVisibility(mHistory == null || isFullscreen() ? View.GONE : View.VISIBLE);
        mBinding.control.batteryInfo.setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        mBinding.control.right.rotate.setVisibility(isFullscreen() && !isLock() ? View.VISIBLE : View.GONE);
        mBinding.keep.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.control.right.back.setVisibility(View.GONE);
        mBinding.control.backTop.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.parse.setVisibility(isFullscreen() && isUseParse() ? View.VISIBLE : View.GONE);
        mBinding.control.action.getRoot().setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        mBinding.control.right.lock.setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        mBinding.control.info.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.control.refresh.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.control.pip.setVisibility(mHistory == null || PiP.noPiP() ? View.GONE : View.VISIBLE);
        mBinding.share.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.cast.setVisibility(mHistory == null ? View.GONE : View.VISIBLE);
        mBinding.control.center.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.bottom.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.top.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        showDisplayInfo();
        checkPlayImg(mPlayers.isPlaying());
        checkBatteryImg();
        setR1Callback();
    }

    private void hideControl() {
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
        showDisplayInfo();
    }

    private void hideSheet() {
        for (Dialog dialog : mDialogs) dialog.dismiss();
        for (Fragment fragment : getSupportFragmentManager().getFragments()) if (fragment instanceof BottomSheetDialogFragment) ((BottomSheetDialogFragment) fragment).dismiss();
        mDialogs.clear();
    }

    private void showPreview(Drawable preview) {
        if (Setting.getFlag() == 0 || isGone(mBinding.widget.preview)) return;
        mBinding.widget.preview.setVisibility(View.VISIBLE);
        mBinding.widget.preview.setImageDrawable(preview);
    }

    private void hidePreview() {
        mBinding.widget.preview.setVisibility(View.GONE);
        mBinding.widget.preview.setImageDrawable(null);
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setOrient() {
        if (isPort() && isAutoRotate()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        if (isLand() && isAutoRotate()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void setArtwork(String url) {
        ImgUtil.load(url, R.drawable.radio, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                getExo().setDefaultArtwork(resource);
                getIjk().setDefaultArtwork(resource);
                showPreview(resource);
                setCoverBackground(url);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable error) {
                getExo().setDefaultArtwork(error);
                getIjk().setDefaultArtwork(error);
                hidePreview();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private void setCoverBackground(String url) {
        App.execute(() -> {
            try {
                Bitmap bitmap = Glide.with(VideoActivity.this)
                        .asBitmap()
                        .load(ImgUtil.getUrl(url))
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .submit(ResUtil.getScreenWidth(), ResUtil.getScreenHeight())
                        .get();
                if (bitmap == null) return;
                Palette.from(bitmap).generate(palette -> {
                    // 旋转/销毁后 Activity 已重建，Palette 的 AsyncTask 无法取消，直接忽略避免 NPE
                    if (isDestroyed() || isFinishing()) return;
                    int darkColor = 0xFF222222;
                    // 优先使用 Muted（柔和），其次其它色调
                    if (palette.getMutedSwatch() != null) {
                        darkColor = palette.getMutedSwatch().getRgb();
                    } else if (palette.getDarkVibrantSwatch() != null) {
                        darkColor = palette.getDarkVibrantSwatch().getRgb();
                    } else if (palette.getDarkMutedSwatch() != null) {
                        darkColor = palette.getDarkMutedSwatch().getRgb();
                    } else if (palette.getDominantSwatch() != null) {
                        darkColor = palette.getDominantSwatch().getRgb();
                    } else if (palette.getVibrantSwatch() != null) {
                        darkColor = palette.getVibrantSwatch().getRgb();
                    }
                    int r = Color.red(darkColor);
                    int g = Color.green(darkColor);
                    int b = Color.blue(darkColor);
                    int darkenAmount = 60;
                    int dr = Math.max(0, r - darkenAmount);
                    int dg = Math.max(0, g - darkenAmount);
                    int db = Math.max(0, b - darkenAmount);
                    // 多阶颜色插值，消除色带
                    int steps = 8;
                    int[] colors = new int[steps];
                    for (int i = 0; i < steps; i++) {
                        float t = (float) i / (steps - 1);
                        int cr = (int) (r * (1 - t) + dr * t);
                        int cg = (int) (g * (1 - t) + dg * t);
                        int cb = (int) (b * (1 - t) + db * t);
                        colors[i] = Color.rgb(cr, cg, cb);
                    }
                    GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                    gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    gradient.setDither(true);
                    runOnUiThread(() -> {
                        // 进入主线程后仍需再次校验，防止回调入队后 Activity 已被销毁
                        if (isDestroyed() || isFinishing()) {
                            bitmap.recycle();
                            return;
                        }
                        gradient.setAlpha(0);
                        getWindow().setBackgroundDrawable(gradient);
                        ValueAnimator animator = ValueAnimator.ofInt(0, 255);
                        animator.setDuration(300);
                        animator.addUpdateListener(animation -> gradient.setAlpha((int) animation.getAnimatedValue()));
                        animator.start();
                        bitmap.recycle();
                    });
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void checkFlag(Vod item) {
        boolean empty = item.getVodFlags().isEmpty();
        mBinding.flag.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            ErrorEvent.flag();
        } else {
            onItemClick(mHistory.getFlag());
            if (mHistory.isRevSort()) reverseEpisode(true);
        }
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        if (!TextUtils.isEmpty(getMark())) mHistory.setVodRemarks(getMark());
        // 用销毁瞬间同步保存的位置覆盖 DB 中异步落库的陈旧位置，实现精确断点续播
        if (mSavedPosition > 0) {
            mHistory.setPosition(mSavedPosition);
            mSavedPosition = 0;
        }
        if (Setting.isIncognito() && mHistory.getKey().equals(getHistoryKey())) mHistory.delete();
        mBinding.control.action.opening.setText(mHistory.getOpening() == 0 ? getString(R.string.play_op) : mPlayers.stringToTime(mHistory.getOpening()));
        mBinding.control.action.ending.setText(mHistory.getEnding() == 0 ? getString(R.string.play_ed) : mPlayers.stringToTime(mHistory.getEnding()));
        mHistory.setVodPic(item.getVodPic());
        mPlayers.setPlayer(getPlayer());
        setScale(getScale());
        setPlayerView();
        setDecodeView();
    }

    private History createHistory(Vod item) {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setCid(VodConfig.getCid());
        history.setVodName(item.getVodName());
        history.findEpisode(item.getVodFlags());
        history.setSpeed(Setting.getPlaySpeed());
        return history;
    }

    private void updateHistory(Episode item, boolean replay) {
        replay = replay || !item.equals(mHistory.getEpisode());
        long position = replay ? 0 : mHistory.getPosition();
        mHistory.setPosition(position);
        mHistory.setEpisodeUrl(item.getUrl());
        mHistory.setVodRemarks(item.getName());
        mHistory.setVodFlag(getFlag().getFlag());
        mHistory.setCreateTime(System.currentTimeMillis());
        mPlayers.setPosition(Math.max(mHistory.getOpening(), mHistory.getPosition()));
    }

    private void checkPlayImg(boolean playing) {
        mBinding.control.play.setImageResource(playing ? androidx.media3.ui.R.drawable.exo_icon_pause : androidx.media3.ui.R.drawable.exo_icon_play);
        mPiP.update(this, playing);
        ActionEvent.update();
    }

    private void checkKeepImg() {
        mBinding.keep.setImageResource(Keep.find(getHistoryKey()) == null ? R.drawable.ic_control_keep_off : R.drawable.ic_control_keep_on);
    }

    private void checkLockImg() {
        mBinding.control.right.lock.setImageResource(isLock() ? R.drawable.ic_control_lock_on : R.drawable.ic_control_lock_off);
    }

    private void checkDanmuImg() {
        mBinding.control.danmu.setImageResource(Setting.isDanmu() ? R.drawable.ic_control_danmu_on : R.drawable.ic_control_danmu_off);
    }

    private void checkBatteryImg() {
        int batteryLevel = Util.batteryLevel();
        int resId = R.drawable.ic_battery_00;
        if (batteryLevel >= 90) resId = R.drawable.ic_battery_full;
        else if (batteryLevel >= 60) resId = R.drawable.ic_battery_75;
        else if (batteryLevel >= 40) resId = R.drawable.ic_battery_50;
        else if (batteryLevel >= 10) resId = R.drawable.ic_battery_25;
        mBinding.control.battery.setImageResource(resId);
    }

    private void createKeep() {
        Keep keep = new Keep();
        keep.setKey(getHistoryKey());
        keep.setCid(VodConfig.getCid());
        keep.setSiteName(getSite().getName());
        keep.setVodPic(mBinding.video.getTag().toString());
        keep.setVodName(mBinding.name.getText().toString());
        keep.setCreateTime(System.currentTimeMillis());
        keep.save();
    }

    @Override
    public void onTrackClick(Track item) {
        item.setKey(getHistoryKey());
        item.save();
    }

    @Override
    public void onSubtitleClick() {
        App.post(this::hideControl, 200);
        SubtitleView subtitleView = mPlayers.isIjk() ? getIjk().getSubtitleView() : getExo().getSubtitleView();
        App.post(() -> SubtitleDialog.create().view(subtitleView).full(isFullscreen()).show(this), 200);
    }

    @Override
    public void onTimeChanged() {
        onTimeChangeDisplaySpeed();
        long position, duration;
        mHistory.setPosition(position = mPlayers.getPosition());
        mHistory.setDuration(duration = mPlayers.getDuration());
        if (position >= 0 && duration > 0 && !Setting.isIncognito()) App.execute(() -> mHistory.update());
        if (mHistory.getEnding() > 0 && duration > 0 && mHistory.getEnding() + position >= duration) {
            mClock.setCallback(null);
            checkNext();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (isRedirect()) return;
        ReceiveDialog.create().event(event).show(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (isRedirect()) return;
        if (ActionEvent.PLAY.equals(event.getAction()) || ActionEvent.PAUSE.equals(event.getAction())) {
            mBinding.control.play.performClick();
        } else if (ActionEvent.NEXT.equals(event.getAction())) {
            mBinding.control.next.performClick();
        } else if (ActionEvent.PREV.equals(event.getAction())) {
            mBinding.control.prev.performClick();
        } else if (ActionEvent.STOP.equals(event.getAction())) {
            finish();
        } else if (ActionEvent.RESTORE.equals(event.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) moveTaskToBack(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (isRedirect()) return;
        if (event.getType() == RefreshEvent.Type.DETAIL) getDetail();
        else if (event.getType() == RefreshEvent.Type.PLAYER) onRefresh();
        else if (event.getType() == RefreshEvent.Type.DANMAKU) checkDanmu(event.getPath());
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) mPlayers.setSub(Sub.from(event.getPath()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        if (isRedirect()) return;
        switch (event.getState()) {
            case 0:
                setInitTrack(true);
                setTrackVisible(false);
                mClock.setCallback(this);
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                stopSearch();
                sourcePending = false;
                checkRotate();
                setMetadata();
                resetToggle();
                resetError();
                hideProgress();
                mPlayers.reset();
                setDefaultTrack();
                setTrackVisible(true);
                checkPlayImg(mPlayers.isPlaying());
                mHistory.setPlayer(mPlayers.getPlayer());
                mBinding.control.size.setText(mPlayers.getSizeText());
                mBinding.display.size.setText(mPlayers.getSizeText());
                if (isVisible(mBinding.control.getRoot())) showControl();
                break;
            case Player.STATE_ENDED:
                checkEnded();
                break;
        }
    }

    private void checkRotate() {
        if (isFullscreen() && !isRotate() && mPlayers.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            setRotate(true);
        }
    }

    private void checkEnded() {
        if (mBinding.control.action.loop.isActivated()) {
            onReset(true);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            checkPlayImg(false);
            checkNext();
        }
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.action.text.setVisibility(visible && (mPlayers.haveTrack(C.TRACK_TYPE_TEXT) || mPlayers.isExo()) ? View.VISIBLE : View.GONE);
        mBinding.control.action.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.action.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
        if (mControlDialog != null && mControlDialog.isVisible()) mControlDialog.setTrackVisible();
    }

    private void setDefaultTrack() {
        if (isInitTrack()) {
            setInitTrack(false);
            mPlayers.prepared();
            mPlayers.setTrack(Track.find(getHistoryKey()));
        }
    }

    private void setMetadata() {
        String title = mHistory.getVodName();
        String episode = getEpisode().getName();
        String artist = title.equals(episode) ? "" : getString(R.string.play_now, episode);
        mPlayers.setMetadata(title, artist, mHistory.getVodPic(), getDefaultArtwork());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (isRedirect()) return;
        if (addErrorCount() > 20) onErrorEnd(event);
        else if (mPlayers.addRetry() > event.getRetry()) checkError(event);
        else if (event.isDecode() && mPlayers.canToggleDecode()) onDecode(false);
        else if (event.isExo() && mPlayers.isExo()) onExoCheck(event);
        else onRefresh();
    }

    private void onExoCheck(ErrorEvent event) {
        if (event.getCode() == PlaybackException.ERROR_CODE_IO_UNSPECIFIED || event.getCode() >= PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED && event.getCode() <= PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED) mPlayers.setFormat(ExoUtil.getMimeType(event.getCode()));
        mPlayers.setMediaSource();
    }

    private void checkError(ErrorEvent event) {
        if (getSite().getPlayerType() == -1 && event.isUrl() && event.getRetry() > 0 && getToggleCount() < 2 && mPlayers.getPlayer() != Players.SYS) {
            toggleCount++;
            nextPlayer();
        } else {
            resetToggle();
            onError(event);
        }
    }

    private void nextPlayer() {
        mPlayers.nextPlayer();
        setPlayerView();
        setDecodeView();
        onRefresh();
    }

    private void onErrorEnd(ErrorEvent event) {
        onErrorPlayer(event);
        resetError();
    }

    private void onErrorPlayer(ErrorEvent event) {
        mBinding.swipeLayout.setEnabled(true);
        Track.delete(getHistoryKey());
        showError(event.getMsg());
        mClock.setCallback(null);
        mPlayers.reset();
        mPlayers.stop();
    }

    private void onError(ErrorEvent event) {
        onErrorPlayer(event);
    }

    private void startFlow() {
        if (getToggleCount() < 2 && mPlayers.getPlayer() != Players.SYS) {
            toggleCount++;
            nextPlayer();
        }
    }

    private void checkParse() {
        int position = mParseAdapter.getPosition();
        boolean last = position == mParseAdapter.getItemCount() - 1;
        boolean pass = position == 0 || last;
        if (last) initParse();
        if (pass) checkFlag();
        else nextParse(position);
    }

    private void initParse() {
        if (mParseAdapter.isEmpty()) return;
        setParse(mParseAdapter.first());
    }

    private void checkFlag() {
        int position = isGone(mBinding.flag) ? -1 : mFlagAdapter.getPosition();
        if (position == mFlagAdapter.getItemCount() - 1) checkSearch(false);
        else nextFlag(position);
    }

    private void checkSearch(boolean force) {
        if (mQuickAdapter.isEmpty()) initSearch(mBinding.name.getText().toString(), true);
        else if (isAutoMode() || force) nextSite();
    }

    private void initSearch(String keyword, boolean auto) {
        stopSearch();
        setAutoMode(auto);
        setInitAuto(auto);
        startSearch(keyword);
    }

    private boolean isPass(Site item) {
        if (isAutoMode() && !item.isChangeable()) return false;
        return item.isSearchable();
    }

    private void startSearch(String keyword) {
        mQuickAdapter.clear();
        List<Site> sites = new ArrayList<>();
        mExecutor = Executors.newFixedThreadPool(Constant.THREAD_POOL * 2);
        for (Site item : VodConfig.get().getSites()) if (isPass(item)) sites.add(item);
        for (Site site : sites) mExecutor.execute(() -> search(site, keyword));
        // 兜底：即使所有站点均无结果，也在 3 秒后结束加载动画，避免弹窗一直转圈
        if (sourcePending) App.post(mSourceFinishRunnable, 3000);
    }

    private void stopSearch() {
        App.removeCallbacks(mSourceFinishRunnable);
        if (mExecutor == null) return;
        mExecutor.shutdownNow();
        mExecutor = null;
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword, true);
        } catch (Throwable ignored) {
        }
    }

    private void setSearch(Result result) {
        List<Vod> items = result.getList();
        Iterator<Vod> iterator = items.iterator();
        while (iterator.hasNext()) if (mismatch(iterator.next())) iterator.remove();
        mQuickAdapter.addAll(items);
        if (isInitAuto()) nextSite();
        // 弹窗打开时：结果实时回填（含空结果时更新加载态）
        if (mSourceDialog != null && mSourceDialog.isVisible()) mSourceDialog.refresh(mQuickAdapter.getItems(), getSelected());
        if (items.isEmpty()) return;
        App.removeCallbacks(mR4);
        // 重置结束计时：600ms 内无新结果到达才结束加载，期间持续回填多个播放源
        if (sourcePending) App.post(mSourceFinishRunnable, 600);
    }

    private boolean mismatch(Vod item) {
        if (getId().equals(item.getVodId())) return true;
        if (mBroken.contains(item.getVodId())) return true;
        String keyword = mBinding.name.getText().toString();
        if (isAutoMode()) return !item.getVodName().equals(keyword);
        else return !item.getVodName().contains(keyword);
    }

    private void nextParse(int position) {
        Parse parse = mParseAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_parse, parse.getName()));
        onItemClick(parse);
    }

    private void nextFlag(int position) {
        Flag flag = mFlagAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_flag, flag.getFlag()));
        onItemClick(flag);
    }

    private void nextSite() {
        if (mQuickAdapter.isEmpty()) return;
        Vod item = mQuickAdapter.get(0);
        Notify.show(getString(R.string.play_switch_site, item.getSiteName()));
        mQuickAdapter.remove(0);
        mBroken.add(getId());
        setInitAuto(false);
        getDetail(item);
    }

    private void onPaused() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkPlayImg(false);
        mPlayers.pause();
    }

    private void onPlay() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkPlayImg(true);
        mPlayers.play();
    }

    public boolean isForeground() {
        return foreground;
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

    private boolean isFullscreen() {
        return fullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        Util.toggleFullscreen(this, this.fullscreen = fullscreen);
    }

    private boolean isInitTrack() {
        return initTrack;
    }

    private void setInitTrack(boolean initTrack) {
        this.initTrack = initTrack;
    }

    private boolean isInitAuto() {
        return initAuto;
    }

    private void setInitAuto(boolean initAuto) {
        this.initAuto = initAuto;
    }

    private boolean isAutoMode() {
        return autoMode;
    }

    private void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public boolean isUseParse() {
        return useParse;
    }

    public void setUseParse(boolean useParse) {
        this.useParse = useParse;
    }

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public boolean isRotate() {
        return rotate;
    }

    public void setRotate(boolean rotate, boolean fullscreen) {
        this.rotate = rotate;
        setFullscreen(fullscreen);
        if (!fullscreen || rotate) noPadding(mBinding.control.getRoot());
        if (fullscreen && !rotate) setPadding(mBinding.control.getRoot());
    }

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
        if (fullscreen && rotate) noPadding(mBinding.control.getRoot());
        if (fullscreen && !rotate) setPadding(mBinding.control.getRoot());
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public int getToggleCount() {
        return toggleCount;
    }

    public void resetToggle() {
        this.toggleCount = 0;
    }

    public int addErrorCount() {
        return ++errorCount;
    }

    public void resetError() {
        this.errorCount = 0;
    }

    private void notifyItemChanged(RecyclerView.Adapter<?> adapter) {
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    private void stopService() {
        PlaybackService.stop();
    }

    @Override
    public void onCasted() {
        onPaused();
    }

    @Override
    public void onScale(int tag) {
        mHistory.setScale(tag);
        setScale(tag);
    }

    @Override
    public void onParse(Parse item) {
        onItemClick(item);
    }

    @Override
    public void onSpeedUp() {
        if (!mPlayers.isPlaying() || !mPlayers.canAdjustSpeed()) return;
        mBinding.control.action.speed.setText(mPlayers.setSpeed(mPlayers.getSpeed() < 3 ? 3 : 5));
        mBinding.widget.speed.startAnimation(ResUtil.getAnim(R.anim.forward));
        mBinding.widget.speed.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSpeedEnd() {
        mBinding.control.action.speed.setText(mPlayers.setSpeed(mHistory.getSpeed()));
        mBinding.widget.speed.setVisibility(View.GONE);
        mBinding.widget.speed.clearAnimation();
    }

    @Override
    public void onBright(int progress) {
        mBinding.widget.bright.setVisibility(View.VISIBLE);
        mBinding.widget.brightProgress.setProgress(progress);
        if (progress < 35) mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_low);
        else if (progress < 70) mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_medium);
        else mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_high);
    }

    @Override
    public void onBrightEnd() {
        mBinding.widget.bright.setVisibility(View.GONE);
    }

    @Override
    public void onVolume(int progress) {
        mBinding.widget.volume.setVisibility(View.VISIBLE);
        mBinding.widget.volumeProgress.setProgress(progress);
        if (progress < 35) mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_low);
        else if (progress < 70) mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_medium);
        else mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_high);
    }

    @Override
    public void onVolumeEnd() {
        mBinding.widget.volume.setVisibility(View.GONE);
    }

    @Override
    public void onSeek(int time) {
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        mBinding.widget.time.setText(mPlayers.getPositionTime(time));
        mBinding.widget.seek.setVisibility(View.VISIBLE);
        hideProgress();
    }

    @Override
    public void onSeekEnd(int time) {
        mBinding.widget.seek.setVisibility(View.GONE);
        mPlayers.seekTo(time);
        showProgress();
        onPlay();
    }

    @Override
    public void onSingleTap() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl();
    }

    @Override
    public void onDoubleTap() {
        if (!isFullscreen()) {
            App.post(this::enterFullscreen, 250);
        } else if (mPlayers.isPlaying()) {
            onPaused();
        } else {
            hideControl();
            onPlay();
        }
    }

    @Override
    public void onShare(CharSequence title) {
        boolean idm = IDMUtil.downloadFile(this, UrlUtil.fixDownloadUrl(mPlayers.getUrl()), title.toString(), mPlayers.getHeaders(), false, false);
        if (!idm) mPlayers.share(this, title);
        setRedirect(true);
    }

    @Override
    public void onPlayerClick(Integer item) {
        mPlayers.setPlayer(item);
        setPlayerView();
        setDecodeView();
        setR1Callback();
        onRefresh();
    }

    @Override
    public void onPlayerShare(String title) {
        this.onShare(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) mPlayers.checkData(data);
    }

    private void onPiP() {
        if (PiP.noPiP()) {
            Notify.show(R.string.error_pip);
            return;
        }
        if (mPlayers.haveTrack(C.TRACK_TYPE_VIDEO)) {
            mPiP.enterManually(this, mPlayers.getVideoWidth(), mPlayers.getVideoHeight(), getScale(), mPlayers.isPlaying());
        }
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
        if (!isFullscreen()) setVideoView(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            mBinding.getRoot().setPadding(0, 0, 0, 0);
            PlaybackService.start(mPlayers);
            mBinding.danmaku.hide();
            hideControl();
            hideSheet();
        } else {
            mBinding.getRoot().setPadding(0, getStatusBarHeight(), 0, 0);
            showDanmu();
            App.post(mR0, 1000);
            setForeground(true);
            if (isStop()) finish();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 全屏状态下旋转：保持不重建，保住播放器与视频流，仅维持沉浸式
        if (isFullscreen()) {
            Util.hideSystemUI(this);
            return;
        }
        // 非全屏：竖屏单列布局在自动旋转开启时转为横屏 → 直接进入全屏（不重建，保视频流）
        if (isAutoRotate() && isPort() && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullscreen();
            return;
        }
        // 非全屏：布局 tag 与实际方向不匹配（横屏分栏转竖屏等）→ 重建以切换 port 单列 / land 分栏布局
        boolean match = (isPort() && newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
                || (isLand() && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        if (!match) recreate();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 弹窗（BottomSheetDialogFragment / Dialog）关闭后 Activity 重新获得焦点，
        // 若此时直接 hideSystemUI 会把状态栏隐藏/透明，故需先判断是否有弹窗正在显示
        if (isFullscreen() && hasFocus && !hasDialog()) Util.hideSystemUI(this);
    }

    private boolean hasDialog() {
        for (Dialog dialog : mDialogs) if (dialog.isShowing()) return true;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BottomSheetDialogFragment && ((BottomSheetDialogFragment) fragment).isVisible()) return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClock.stop().start();
        setStop(false);
        onPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isForeground()) return;
        if (isRedirect()) onPlay();
        App.post(mR0, 1000);
        setForeground(true);
        setRedirect(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setForeground(false);
        App.removeCallbacks(mR0);
        if (isRedirect()) onPaused();
        else if (Setting.isBackgroundOn() && !isFinishing()) PlaybackService.start(mPlayers);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Setting.isBackgroundOff()) onPaused();
        if (Setting.isBackgroundOff()) mClock.stop();
        setStop(true);
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isFullscreen() && !isLock()) {
            exitFullscreen();
        } else if (!isLock()) {
            stopSearch();
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // 旋转/杀进程前同步保存当前播放位置，避免 onTimeChanged 的异步落库来不及写入
        long position = mPlayers != null ? mPlayers.getPosition() : 0L;
        long duration = mPlayers != null ? mPlayers.getDuration() : 0L;
        // 位置有效且未近片尾（近片尾不恢复，避免恢复后立即结束跳到下一集）才保存
        if (position > 0 && (duration <= 0 || position < duration - 10_000)) {
            outState.putLong("video_position", position);
        }
        if (isFullscreen()) outState.putBoolean("video_fullscreen", true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSearch();
        mClock.release();
        mPlayers.release();
        Timer.get().reset();
        App.post(mR0, 1000);
        Source.get().stop();
        RefreshEvent.history();
        App.removeCallbacks(mR1, mR2, mR3, mR4, mSourceFinishRunnable);
        mSourceDialog = null;
        mViewModel.result.removeObserver(mObserveDetail);
        mViewModel.player.removeObserver(mObservePlayer);
        mViewModel.search.removeObserver(mObserveSearch);
    }
}
