package com.fongmi.android.tv.ui.activity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.palette.graphics.Palette;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.CastVideo;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.player.IjkUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.ChannelLiveAdapter;
import com.fongmi.android.tv.ui.adapter.EpgProgramAdapter;
import com.fongmi.android.tv.ui.adapter.GroupLiveAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.dialog.CastDialog;
import com.fongmi.android.tv.ui.dialog.EpgAllDialog;
import com.fongmi.android.tv.ui.dialog.InfoDialog;
import com.fongmi.android.tv.ui.dialog.LineChooseDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.ui.dialog.PlayerDialog;
import com.fongmi.android.tv.ui.dialog.SubtitleDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.IDMUtil;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PiP;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class LiveActivity extends BaseActivity implements Clock.Callback, CustomKeyDownLive.Listener, TrackDialog.Listener, PlayerDialog.Listener, LiveCallback, EpgProgramAdapter.OnClickListener, GroupLiveAdapter.OnClickListener, ChannelLiveAdapter.OnClickListener, CastDialog.Listener, InfoDialog.Listener {

    private ActivityLiveBinding mBinding;
    private View mShadow;
    private RelativeLayout.LayoutParams mVideoParams;
    private RelativeLayout.LayoutParams mShadowParams;
    private RelativeLayout.LayoutParams mContentParams;
    private boolean fullscreen;
    private EpgProgramAdapter mEpgProgramAdapter;
    private GroupLiveAdapter mGroupTabAdapter;
    private ChannelLiveAdapter mChannelTabAdapter;
    private Observer<Channel> mObserveUrl;
    private CustomKeyDownLive mKeyDown;
    private Observer<Epg> mObserveEpg;
    private LiveViewModel mViewModel;
    private List<Group> mHides;
    private List<Group> mGroups;
    private Players mPlayers;
    private Channel mChannel;
    private Group mGroup;
    private Runnable mR0;
    private Runnable mR1;
    private boolean mControlHiding;
    private Runnable mR2;
    private Runnable mR3;
    private Clock mClock;
    private boolean foreground;
    private boolean redirect;
    private boolean rotate;
    private boolean stop;
    private boolean lock;
    private int toggleCount;
    private int errorCount;
    private PiP mPiP;

    public static void start(Context context) {
        start(context, "", "");
    }

    public static void start(Context context, String group, String channel) {
        if (LiveConfig.isEmpty()) return;
        context.startActivity(new Intent(context, LiveActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("group", group).putExtra("channel", channel).putExtra("empty", false));
    }

    private boolean isEmpty() {
        return getIntent().getBooleanExtra("empty", true);
    }

    private String getGroupName() {
        return getIntent().getStringExtra("group") == null ? "" : getIntent().getStringExtra("group");
    }

    private String getChannelName() {
        return getIntent().getStringExtra("channel") == null ? "" : getIntent().getStringExtra("channel");
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

    private Group getKeep() {
        return mGroups.isEmpty() ? Group.create() : mGroups.get(0);
    }

    private Live getHome() {
        return LiveConfig.get().getHome();
    }

    private int getPlayerType(int playerType) {
        return playerType != -1 ? playerType : Setting.getLivePlayer();
    }

    private int getTimeout() {
        return getHome().isEmpty() ? Constant.TIMEOUT_PLAY : getHome().getTimeout();
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mKeyDown = CustomKeyDownLive.create(this, mBinding.video);
        mClock = Clock.create(Arrays.asList(mBinding.widget.clock, mBinding.display.clock, mBinding.control.time));
        getWindow().setStatusBarColor(Color.BLACK);
        // 渐变背景加载完成前的默认兜底背景，避免窗口无背景导致渲染异常（点播播放页默认有壁纸兜底）
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        mBinding.getRoot().setPadding(0, getStatusBarHeight(), 0, 0);
        mShadow = findViewById(R.id.shadow);
        mVideoParams = (RelativeLayout.LayoutParams) mBinding.video.getLayoutParams();
        mShadowParams = (RelativeLayout.LayoutParams) mShadow.getLayoutParams();
        mContentParams = (RelativeLayout.LayoutParams) mBinding.content.getLayoutParams();
        resizeVideo();
        mPlayers = Players.create(this);
        mObserveEpg = this::setEpg;
        mObserveUrl = this::start;
        mHides = new ArrayList<>();
        mGroups = new ArrayList<>();
        mR0 = this::stopService;
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mR3 = this::hideInfo;
        mPiP = new PiP();
        Server.get().start();
        setForeground(true);
        setRecyclerView();
        setVideoView();
        setDisplayView();
        setViewModel();
        checkLive();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.control.cast.setOnClickListener(view -> onCast());
        mBinding.control.info.setOnClickListener(view -> onInfo());
        mBinding.control.backTop.setOnClickListener(view -> onBack());
        mBinding.control.full.setOnClickListener(view -> onFull());
        mBinding.control.right.lock.setOnClickListener(view -> onLock());
        mBinding.control.right.rotate.setOnClickListener(view -> onRotate());
        mBinding.control.action.text.setOnClickListener(this::onTrack);
        mBinding.control.action.audio.setOnClickListener(this::onTrack);
        mBinding.control.action.video.setOnClickListener(this::onTrack);
        mBinding.control.action.home.setOnClickListener(view -> onHome());
        mBinding.control.action.line.setOnClickListener(view -> onLine());
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.action.invert.setOnClickListener(view -> onInvert());
        mBinding.control.action.across.setOnClickListener(view -> onAcross());
        mBinding.control.action.change.setOnClickListener(view -> onChange());
        mBinding.control.action.player.setOnClickListener(view -> onPlayer());
        mBinding.control.action.decode.setOnClickListener(view -> onDecode());
        mBinding.control.action.text.setOnLongClickListener(view -> onTextLong());
        mBinding.control.action.player.setOnLongClickListener(view -> onChoose());
        mBinding.control.action.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.action.getRoot().setOnTouchListener(this::onActionTouch);
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        mBinding.control.play.setOnClickListener(view -> checkPlay());
        mBinding.control.prev.setOnClickListener(view -> prevChannel());
        mBinding.control.next.setOnClickListener(view -> nextChannel());
        // 提前把两个页面从 FrameLayout 摘除，避免 ViewPager 在测量阶段 instantiateItem 时 removeView 改坏 FrameLayout 子节点数组
        if (mBinding.swipeLayout.getParent() != null) ((ViewGroup) mBinding.swipeLayout.getParent()).removeView(mBinding.swipeLayout);
        if (mBinding.channelBox.getParent() != null) ((ViewGroup) mBinding.channelBox.getParent()).removeView(mBinding.channelBox);
        // 禁用手势滑动翻页，避免播放/换台 tab 内容被轻易误切，仅保留 TabLayout 点击切换
        mBinding.tabPager.setSwipeEnabled(false);
        mBinding.tabPager.setAdapter(new TabPagerAdapter());
        mBinding.tabLayout.setupWithViewPager(mBinding.tabPager);
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.playCast.setOnClickListener(view -> onCast());
        mBinding.share.setOnClickListener(view -> onShareClick());
        mBinding.allEpg.setOnClickListener(view -> onAllEpg());
        mBinding.currentLine.setOnClickListener(view -> onMoreLine());
        mBinding.swipeLayout.setOnRefreshListener(this::onSwipeRefresh);
    }

    private void resizeVideo() {
        mBinding.video.post(this::setLayout);
    }

    private void setLayout() {
        // 画中画小窗中保持视频铺满的布局：进入小窗会触发 onConfigurationChanged → setLayout，
        // 若不拦截会重新显示 tab/频道列表，导致小窗内出现 tab 页
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            mBinding.getRoot().setPadding(0, 0, 0, 0);
            setVideoFullscreen();
            mShadow.setVisibility(View.GONE);
            mBinding.content.setVisibility(View.GONE);
            return;
        }
        boolean land = ResUtil.isLand(this);
        if (isFullscreen()) {
            mBinding.getRoot().setPadding(0, 0, 0, 0);
            setVideoFullscreen();
            mShadow.setVisibility(View.GONE);
            mBinding.content.setVisibility(View.GONE);
        } else {
            mBinding.getRoot().setPadding(0, getStatusBarHeight(), 0, 0);
            mShadow.setVisibility(View.VISIBLE);
            mBinding.content.setVisibility(View.VISIBLE);
            setVideoLayout(land);
            setShadowLayout(land);
            setContentLayout(land);
        }
    }

    private void setVideoFullscreen() {
        mVideoParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mVideoParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mBinding.video.setLayoutParams(mVideoParams);
    }

    private void setVideoLayout(boolean land) {
        mVideoParams.width = land ? ResUtil.getScreenWidth(this) * 3 / 5 : ViewGroup.LayoutParams.MATCH_PARENT;
        mVideoParams.height = land ? ViewGroup.LayoutParams.MATCH_PARENT : ResUtil.getScreenWidth(this) * 9 / 16;
        mBinding.video.setLayoutParams(mVideoParams);
    }

    private void setShadowLayout(boolean land) {
        mShadow.setBackgroundResource(land ? R.drawable.shadow_land : R.drawable.shadow);
        mShadowParams.width = land ? ResUtil.dp2px(4) : ViewGroup.LayoutParams.MATCH_PARENT;
        mShadowParams.height = land ? ViewGroup.LayoutParams.MATCH_PARENT : ResUtil.dp2px(4);
        if (land) {
            mShadowParams.removeRule(RelativeLayout.BELOW);
            mShadowParams.addRule(RelativeLayout.END_OF, R.id.video);
        } else {
            mShadowParams.removeRule(RelativeLayout.END_OF);
            mShadowParams.addRule(RelativeLayout.BELOW, R.id.video);
        }
        mShadow.setLayoutParams(mShadowParams);
    }

    private void setContentLayout(boolean land) {
        if (land) {
            mContentParams.removeRule(RelativeLayout.BELOW);
            mContentParams.addRule(RelativeLayout.END_OF, R.id.shadow);
        } else {
            mContentParams.removeRule(RelativeLayout.END_OF);
            mContentParams.addRule(RelativeLayout.BELOW, R.id.video);
        }
        mBinding.content.setLayoutParams(mContentParams);
    }

    private void setRecyclerView() {
        // 播放 tab 节目单横向卡片列表
        mBinding.epgProgram.setHasFixedSize(true);
        mBinding.epgProgram.setItemAnimator(null);
        mBinding.epgProgram.setAdapter(mEpgProgramAdapter = new EpgProgramAdapter(this));
        // 横向列表按下时声明自己消费手势，避免外层 CustomViewPager 拦截横向滑动导致列表无法滚动
        mBinding.epgProgram.setOnTouchListener(this::onHorizontalTouch);
        // 换台 tab 左侧频道分类 + 右侧频道列表
        mBinding.groupList.setHasFixedSize(true);
        mBinding.groupList.setItemAnimator(null);
        mBinding.groupList.setAdapter(mGroupTabAdapter = new GroupLiveAdapter(this));
        mBinding.channelList.setHasFixedSize(true);
        mBinding.channelList.setItemAnimator(null);
        mBinding.channelList.setAdapter(mChannelTabAdapter = new ChannelLiveAdapter(this));
    }

    private boolean onHorizontalTouch(View v, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            v.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    private void setPlayerView() {
        getIjk().setPlayer(mPlayers.getPlayer());
        mBinding.control.action.speed.setText(mPlayers.getSpeedText());
        mBinding.control.action.player.setText(mPlayers.getPlayerText());
        mBinding.control.action.speed.setEnabled(mPlayers.canAdjustSpeed());
        if (mPlayers.isMpv()) {
            getExo().setPlayer(mPlayers.mpv());
            getExo().setVisibility(View.VISIBLE);
            getIjk().setVisibility(View.GONE);
        } else {
            getExo().setPlayer(mPlayers.exo());
            getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
            getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
        }
    }

    private void setDecodeView() {
        mBinding.control.action.decode.setText(mPlayers.getDecodeText());
    }

    private void setVideoView() {
        mPlayers.init(getExo(), getIjk());
        setScale(Setting.getLiveScale());
        ExoUtil.setSubtitleView(mBinding.exo);
        IjkUtil.setSubtitleView(mBinding.ijk);
        mBinding.control.action.invert.setActivated(Setting.isInvert());
        mBinding.control.action.across.setActivated(Setting.isAcross());
        mBinding.control.action.change.setActivated(Setting.isChange());
        mBinding.control.action.home.setVisibility(LiveConfig.isOnly() ? View.GONE : View.VISIBLE);
        mBinding.video.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> mPiP.update(getActivity(), view));
    }

    private void setDisplayView() {
        mBinding.display.getRoot().setVisibility(View.VISIBLE);
        mBinding.display.progress.setVisibility(View.GONE);
        showDisplayInfo();
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.action.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mViewModel.url.observeForever(mObserveUrl);
        mViewModel.xml.observe(this, this::setEpg);
        mViewModel.epg.observeForever(mObserveEpg);
        mViewModel.live.observe(this, live -> {
            mViewModel.getXml(live);
            hideProgress();
            setGroup(live);
        });
    }

    private void checkLive() {
        if (isEmpty()) {
            LiveConfig.get().init().load(getCallback());
        } else {
            getLive();
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                getLive();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    private void getLive() {
        mBinding.control.action.home.setText(getHome().getName());
        mPlayers.setPlayer(Setting.getLivePlayer());
        mViewModel.getLive(getHome());
        setPlayerView();
        setDecodeView();
        showProgress();
    }

    private void setGroup(Live live) {
        mHides.clear();
        mGroups.clear();
        for (Group group : live.getGroups()) (group.isHidden() ? mHides : mGroups).add(group);
        // 换台 tab 左侧分类列表
        mGroupTabAdapter.addAll(mGroups);
        if (!getGroupName().isEmpty()) {
            for (Group group : mGroups) {
                if (group.getName().equals(getGroupName())) {
                    int index = group.find(getChannelName());
                    if (index == -1) return;
                    setGroup(group, index);
                    return;
                }
            }
        }
        setPosition(LiveConfig.get().find(mGroups));
    }

    private void setPosition(int[] position) {
        if (position[0] == -1 || mGroups.isEmpty()) return;
        int size = mGroups.size();
        if (position[0] >= size) position[0] = size - 1;
        setGroup(mGroups.get(position[0]), position[1]);
    }

    private void setGroup(Group item, int position) {
        mGroup = item;
        mGroup.setPosition(position);
        if (mGroup.isEmpty()) return;
        onItemClick(mGroup.current());
        // 批量拉取当前分组频道节目单，供换台 tab 展示
        loadEpgList();
    }

    private void onCast() {
        CastDialog.create().video(CastVideo.get(mBinding.control.title.getText().toString(), mPlayers.getUrl())).fm(false).show(this);
    }

    private void onInfo() {
        InfoDialog.create(this).title(mBinding.control.title.getText()).headers(mPlayers.getHeaders()).url(mPlayers.getUrl()).show();
    }

    private void onBack() {
        if (isFullscreen()) {
            setFullscreen(false);
            setLayout();
            showControl();
        } else {
            finish();
        }
    }

    private void onFull() {
        setFullscreen(!isFullscreen());
        setLayout();
        showControl();
    }

    private void onKeep() {
        if (mChannel == null) return;
        boolean exist = Keep.exist(mChannel.getName());
        Notify.show(exist ? R.string.keep_del : R.string.keep_add);
        if (exist) delKeep(mChannel);
        else addKeep(mChannel);
        checkKeepImg();
    }

    private void checkKeepImg() {
        if (mChannel == null) return;
        mBinding.keep.setImageResource(Keep.exist(mChannel.getName()) ? R.drawable.ic_control_keep_on : R.drawable.ic_control_keep_off);
    }

    private void onShareClick() {
        onShare(mBinding.control.title.getText());
    }

    private void onSwipeRefresh() {
        mBinding.swipeLayout.setRefreshing(false);
        fetch();
    }

    private void onLock() {
        setLock(!isLock());
        mKeyDown.setLock(isLock());
        checkLockImg();
        showControl();
    }

    private void onRotate() {
        setR1Callback();
        setRotate(!isRotate());
        setRequestedOrientation(ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void checkPlay() {
        if (mPlayers.isPlaying()) onPaused();
        else onPlay();
    }

    private void onTrack(View view) {
        TrackDialog.create().player(mPlayers).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
    }

    private void onHome() {
        LiveDialog.create().show(this);
        hideControl();
    }

    private void onLine() {
        nextLine(false);
    }

    private void onScale() {
        int index = Setting.getLiveScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        Setting.putLiveScale(index = index == array.length - 1 ? 0 : ++index);
        setScale(index);
        setR1Callback();
    }

    private void onSpeed() {
        mBinding.control.action.speed.setText(mPlayers.addSpeed());
        setR1Callback();
    }

    private boolean onSpeedLong() {
        mBinding.control.action.speed.setText(mPlayers.toggleSpeed());
        setR1Callback();
        return true;
    }

    private void onInvert() {
        setR1Callback();
        Setting.putInvert(!Setting.isInvert());
        mBinding.control.action.invert.setActivated(Setting.isInvert());
    }

    private void onAcross() {
        setR1Callback();
        Setting.putAcross(!Setting.isAcross());
        mBinding.control.action.across.setActivated(Setting.isAcross());
    }

    private void onChange() {
        setR1Callback();
        Setting.putChange(!Setting.isChange());
        mBinding.control.action.change.setActivated(Setting.isChange());
    }

    private void onPlayer() {
        PlayerDialog.create().select(mPlayers.getPlayer()).title(mBinding.control.title.getText().toString()).show(this);
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
        if (e.getAction() == MotionEvent.ACTION_UP) setR1Callback();
        return false;
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
        // 未全屏时隐藏底部文本按钮（播放器/解码/倍速/线路等），全屏时才显示
        mBinding.control.action.getRoot().setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        mBinding.control.info.setVisibility(mPlayers.isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.control.cast.setVisibility(mPlayers.isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.control.right.rotate.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.right.back.setVisibility(View.GONE);
        mBinding.control.bottom.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.top.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.backTop.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.full.setVisibility(isFullscreen() ? View.GONE : View.VISIBLE);
        mBinding.control.batteryInfo.setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        checkBatteryImg();
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        mControlHiding = false;
        mBinding.control.top.animate().cancel();
        mBinding.control.bottom.animate().cancel();
        mBinding.control.top.setTranslationY(-mBinding.control.top.getHeight());
        mBinding.control.bottom.setTranslationY(mBinding.control.bottom.getHeight());
        mBinding.control.top.animate().translationY(0).setDuration(300).setInterpolator(new DecelerateInterpolator());
        mBinding.control.bottom.animate().translationY(0).setDuration(300).setInterpolator(new DecelerateInterpolator());
        setR1Callback();
        hideInfo();
    }

    private void hideControl() {
        App.removeCallbacks(mR1);
        mControlHiding = false;
        mBinding.control.top.animate().cancel();
        mBinding.control.bottom.animate().cancel();
        mControlHiding = true;
        mBinding.control.top.animate().translationY(-mBinding.control.top.getHeight()).setDuration(300).setInterpolator(new AccelerateInterpolator());
        mBinding.control.bottom.animate().translationY(mBinding.control.bottom.getHeight()).setDuration(300).setInterpolator(new AccelerateInterpolator()).withEndAction(() -> {
            if (!mControlHiding) return;
            mControlHiding = false;
            mBinding.control.getRoot().setVisibility(View.GONE);
            mBinding.control.top.setTranslationY(0);
            mBinding.control.bottom.setTranslationY(0);
        });
    }

    private void showDisplayInfo() {
        // 小窗模式下隐藏时间/网速/标题等悬浮信息，保持小窗画面干净（与点播一致）
        boolean pip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
        boolean controlVisible = isVisible(mBinding.control.getRoot()) || isVisible(mBinding.widget.info);
        boolean visible = (!controlVisible && !isLock()) && !pip;
        mBinding.display.clock.setVisibility(Setting.isDisplayTime() && visible ? View.VISIBLE : View.GONE);
        mBinding.display.netspeed.setVisibility(Setting.isDisplaySpeed() && visible ? View.VISIBLE : View.GONE);
        mBinding.display.duration.setVisibility(View.GONE);
        mBinding.display.titleLayout.setVisibility(Setting.isDisplayVideoTitle() && visible ? View.VISIBLE : View.GONE);
    }

    private void onTimeChangeDisplaySpeed() {
        boolean controlVisible = isVisible(mBinding.control.getRoot()) || isVisible(mBinding.widget.info);
        boolean visible = (!controlVisible && !isLock());
        if (Setting.isDisplaySpeed() && visible) Traffic.setSpeed(mBinding.display.netspeed);
        showDisplayInfo();
    }

    private void showInfo() {
        boolean pip = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && isInPictureInPictureMode();
        mBinding.widget.infoPip.setVisibility(pip ? View.VISIBLE : View.GONE);
        mBinding.widget.info.setVisibility(pip ? View.GONE : View.VISIBLE);
        setR3Callback();
        hideControl();
        setInfo();
    }

    private void hideInfo() {
        mBinding.widget.infoPip.setVisibility(View.GONE);
        mBinding.widget.info.setVisibility(View.GONE);
        App.removeCallbacks(mR3);
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void setR3Callback() {
        App.post(mR3, Constant.INTERVAL_HIDE);
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl();
        hideInfo();
    }

    private void setArtwork(String url) {
        ImgUtil.load(url, R.drawable.radio, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                getExo().setDefaultArtwork(resource);
                getIjk().setDefaultArtwork(resource);
                // 直播播放页默认使用兜底黑色背景，不再使用封面动态取色（setCoverBackground 代码保留备用）
                // setCoverBackground(url);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable error) {
                getExo().setDefaultArtwork(error);
                getIjk().setDefaultArtwork(error);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private void setCoverBackground(String url) {
        App.execute(() -> {
            try {
                Bitmap bitmap = Glide.with(LiveActivity.this)
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

    @Override
    public void onItemClick(Channel item) {
        if (item.getUrls().isEmpty()) return;
        item.group(mGroup);
        mGroup.setPosition(mGroup.getChannel().indexOf(item));
        mPlayers.setPlayer(getPlayerType(item.getPlayerType()));
        setArtwork(item.getLogo());
        mChannel = item;
        setPlayerView();
        setChannelAdapter();
        setInfo();
        fetch();
    }

    // 点击全部节目：弹出日期 + 时间线弹窗，已结束且支持回放的节目点击可播放
    private void onAllEpg() {
        if (mChannel == null) return;
        EpgAllDialog.create().channel(mChannel).viewModel(mViewModel).listener(this::onItemClick).show(this);
    }

    // 换台 tab 点击分类：仅切换分类并加载频道列表，不自动播放
    @Override
    public void onItemClick(Group item) {
        if (mGroup != null && item.equals(mGroup)) return;
        mGroup = item;
        setChannelTab();
        loadEpgList();
    }

    // 批量拉取当前分组频道节目单，完成后整体刷新换台列表，确保所有频道节目都显示
    private void loadEpgList() {
        mViewModel.getEpgList(mGroup.getChannel(), this::refreshChannelTab);
    }

    private void refreshChannelTab() {
        if (mGroup == null) return;
        mChannelTabAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(EpgData item) {
        if (item.isFuture() || !mChannel.hasCatchup()) return;
        Notify.show(getString(R.string.play_ready, item.getTitle()));
        // 回看/直播点击后高亮该节目卡片，并滚动到当前节目（非当天节目不滚动）
        mEpgProgramAdapter.setSelected(item);
        int index = mEpgProgramAdapter.indexOf(item);
        if (index >= 0) mBinding.epgProgram.scrollToPosition(index);
        // 同步更新播放信息中的当前节目/下一节目
        setPlayInfo();
        mViewModel.getUrl(mChannel, item);
        mPlayers.clear();
        mPlayers.stop();
        showProgress();
    }

    // 未来节目：预约调起系统日历新建事件提醒，与弹窗预约保持一致
    @Override
    public void onReserve(EpgData item) {
        if (mChannel == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.Events.TITLE, item.getTitle());
            intent.putExtra(CalendarContract.Events.DESCRIPTION, mChannel.getName());
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.getStartTime());
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.getEndTime());
            startActivity(intent);
            Notify.show(R.string.live_epg_reserve_toast);
        } catch (Exception e) {
            Notify.show(R.string.live_epg_reserve_fail);
        }
    }

    private void addKeep(Channel item) {
        getKeep().add(item);
        Keep keep = new Keep();
        keep.setKey(item.getName());
        keep.setType(1);
        keep.save();
    }

    private void delKeep(Channel item) {
        if (mGroup != null && mGroup.isKeep()) mChannelTabAdapter.remove(item);
        if (mGroup != null) mGroup.getChannel().remove(item);
        Keep.delete(item.getName());
    }

    // 换台 tab 频道收藏按钮
    @Override
    public void onKeepClick(Channel item) {
        if (item.getUrls().isEmpty()) return;
        boolean exist = Keep.exist(item.getName());
        Notify.show(exist ? R.string.keep_del : R.string.keep_add);
        if (exist) delKeep(item);
        else addKeep(item);
        mChannelTabAdapter.changed(item);
        if (mChannel != null && mChannel.equals(item)) checkKeepImg();
    }

    private void setInfo() {
        mViewModel.getEpg(mChannel);
        // 切换频道后先清空节目单并显示占位，EPG 加载完成后由 setEpg 填充
        String none = getString(R.string.live_epg_none);
        mBinding.widget.play.setText(none);
        mBinding.playEpg.setText(none);
        mBinding.playNext.setText("");
        mBinding.playNext.setVisibility(View.GONE);
        mEpgProgramAdapter.clear();
        mBinding.epgProgramEmpty.setVisibility(View.VISIBLE);
        mChannel.loadLogo(mBinding.widget.logo);
        mChannel.loadLogo(mBinding.playLogo);
        mBinding.widget.name.setText(mChannel.getName());
        mBinding.playName.setText(mChannel.getName());
        mBinding.control.title.setText(mChannel.getName());
        mBinding.display.title.setText(mChannel.getName());
        mBinding.widget.namePip.setText(mChannel.getName());
        mBinding.widget.line.setText(mChannel.getLineText());
        mBinding.widget.number.setText(mChannel.getNumber());
        mBinding.widget.numberPip.setText(mChannel.getNumber());
        mBinding.playNumber.setText(mChannel.getNumber());
        mBinding.playLine.setText(mChannel.getLineText());
        setLineView();
        mBinding.widget.name.setMaxEms(mChannel.getName().length());
        mBinding.widget.line.setVisibility(mChannel.getLineVisible());
        mBinding.control.action.line.setText(mBinding.widget.line.getText());
        mBinding.control.action.line.setVisibility(mBinding.widget.line.getVisibility());
        setChannelAdapter();
        checkKeepImg();
    }

    // 播放 tab 选中频道时同步换台 tab 选中状态
    private void setChannelAdapter() {
        if (mGroup == null) return;
        setChannelTab();
    }

    // 换台 tab：左侧分类选中 + 右侧频道列表同步
    private void setChannelTab() {
        if (mGroup == null) return;
        mGroupTabAdapter.setSelected(mGroup);
        mBinding.groupList.scrollToPosition(Math.max(mGroups.indexOf(mGroup), 0));
        mChannelTabAdapter.addAll(mGroup.getChannel());
        mChannelTabAdapter.setSelected(mGroup.getPosition());
        mBinding.channelEmpty.setVisibility(mGroup.getChannel().isEmpty() ? View.VISIBLE : View.GONE);
        mBinding.channelList.scrollToPosition(Math.max(mGroup.getPosition(), 0));
    }

    private void setEpg() {
        String epg = mChannel.getData().getEpg();
        // 节目单横向卡片展示频道当天数据
        List<EpgData> data = mChannel.getData().getList();
        if (epg.length() > 0) mBinding.widget.name.setMaxEms(12);
        // 无当前节目时显示占位提示
        String text = epg.isEmpty() ? getString(R.string.live_epg_none) : epg;
        mBinding.widget.play.setText(text);
        mBinding.playEpg.setText(text);
        setPlayInfo();
        mChannelTabAdapter.changed(mChannel);
        // 传当前频道供节目卡片判断回看/预约状态
        mEpgProgramAdapter.setChannel(mChannel);
        mEpgProgramAdapter.addAll(data);
        // 节目单为空时显示占位提示
        mBinding.epgProgramEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        mBinding.epgProgram.scrollToPosition(Math.max(mChannel.getData().getSelected(), 0));
        setMetadata();
    }

    // 频道信息区两行节目展示：当前节目 / 下一个节目
    private void setPlayInfo() {
        if (mChannel == null) return;
        List<EpgData> list = mChannel.getData().getList();
        int index = mChannel.getData().getSelected();
        String now = getString(R.string.live_epg_none);
        String next = "";
        if (index >= 0 && index < list.size()) {
            EpgData current = list.get(index);
            if (!current.getTitle().isEmpty()) {
                now = getString(R.string.live_epg_now, getTimeText(current), current.getTitle());
                if (index + 1 < list.size()) {
                    EpgData after = list.get(index + 1);
                    if (!after.getTitle().isEmpty()) next = getString(R.string.live_epg_next, getTimeText(after), after.getTitle());
                }
            }
        }
        mBinding.playEpg.setText(now);
        mBinding.playNext.setText(next);
        mBinding.playNext.setVisibility(next.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String getTimeText(EpgData item) {
        if (item.getStart().isEmpty() && item.getEnd().isEmpty()) return "";
        return item.getStart() + "~" + item.getEnd();
    }

    // 线路分类区域：单线路时隐藏，右侧显示当前线路
    private void setLineView() {
        if (mChannel == null) return;
        boolean only = mChannel.isOnly();
        mBinding.lineBox.setVisibility(only ? View.GONE : View.VISIBLE);
        if (only) return;
        mBinding.currentLine.setText(mChannel.getLineText());
    }

    // 打开线路选择弹窗（参考频道分类弹窗 GroupChooseDialog）
    private void onMoreLine() {
        if (mChannel == null || mChannel.isOnly()) return;
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < mChannel.getUrls().size(); i++) {
            String url = mChannel.getUrls().get(i);
            lines.add(url.contains("$") ? url.split("\\$")[1] : ResUtil.getString(R.string.live_line, i + 1));
        }
        LineChooseDialog.create().items(lines).selected(mChannel.getLine()).listener(this::onLineClick).show(this);
    }

    private void onLineClick(int position) {
        if (mChannel == null || mChannel.getLine() == position) return;
        mChannel.setLine(position);
        setInfo();
        fetch();
    }

    private void setEpg(boolean success) {
        if (mChannel != null && success) mViewModel.getEpg(mChannel);
        // XML 节目单为异步解析，完成后整体刷新换台列表，确保每个频道显示各自当前节目
        if (success) refreshChannelTab();
    }

    // EPG 结果回调：刷新播放页节目单与换台 tab 对应频道（弹窗内其它日期结果不影响当天展示）
    private void setEpg(Epg epg) {
        if (mChannel == null || epg == null || mGroup == null) return;
        for (Channel item : mGroup.getChannel()) {
            if (item.getTvgName().equals(epg.getKey())) {
                if (item.equals(mChannel)) setEpg();
                else mChannelTabAdapter.changed(item);
                return;
            }
        }
    }

    private void fetch() {
        if (mChannel == null) return;
        LiveConfig.get().setKeep(mChannel);
        mViewModel.getUrl(mChannel);
        mPlayers.clear();
        mPlayers.stop();
        showProgress();
    }

    private void start(Channel result) {
        mPlayers.start(result, getTimeout());
    }

    private void checkPlayImg(boolean playing) {
        mBinding.control.play.setImageResource(playing ? androidx.media3.ui.R.drawable.exo_icon_pause : androidx.media3.ui.R.drawable.exo_icon_play);
        mPiP.update(this, playing);
        ActionEvent.update();
    }

    private void checkLockImg() {
        mBinding.control.right.lock.setImageResource(isLock() ? R.drawable.ic_control_lock_on : R.drawable.ic_control_lock_off);
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

    private void resetAdapter() {
        mEpgProgramAdapter.clear();
        mGroupTabAdapter.clear();
        mChannelTabAdapter.clear();
        mHides.clear();
        mGroups.clear();
        mChannel = null;
        mGroup = null;
    }

    @Override
    public void onTrackClick(Track item) {
    }

    @Override
    public void onSubtitleClick() {
        App.post(this::hideControl, 200);
        SubtitleView subtitleView = mPlayers.isIjk() ? getIjk().getSubtitleView() : getExo().getSubtitleView();
        App.post(() -> SubtitleDialog.create().view(subtitleView).full(true).show(this), 200);
    }

    @Override
    public void onTimeChanged() {
        onTimeChangeDisplaySpeed();
    }

    @Override
    public void setLive(Live item) {
        if (item.isActivated()) item.getGroups().clear();
        LiveConfig.get().setHome(item);
        mPlayers.reset();
        mPlayers.stop();
        resetAdapter();
        hideControl();
        getLive();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (ActionEvent.PLAY.equals(event.getAction()) || ActionEvent.PAUSE.equals(event.getAction())) {
            checkPlay();
        } else if (ActionEvent.NEXT.equals(event.getAction())) {
            nextChannel();
        } else if (ActionEvent.PREV.equals(event.getAction())) {
            prevChannel();
        } else if (ActionEvent.STOP.equals(event.getAction())) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case LIVE:
                setLive(getHome());
                break;
            case PLAYER:
                fetch();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                setTrackVisible(false);
                mClock.setCallback(this);
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                setMetadata();
                resetToggle();
                resetError();
                hideProgress();
                mPlayers.reset();
                setTrackVisible(true);
                checkPlayImg(mPlayers.isPlaying());
                mBinding.control.size.setText(mPlayers.getSizeText());
                mBinding.display.size.setText(mPlayers.getSizeText());
                if (isVisible(mBinding.control.getRoot())) showControl();
                break;
            case Player.STATE_ENDED:
                checkNext();
                break;
        }
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.action.text.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_TEXT) ? View.VISIBLE : View.GONE);
        mBinding.control.action.speed.setVisibility(visible && mPlayers.isVod() ? View.VISIBLE : View.GONE);
        mBinding.control.action.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.action.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setMetadata() {
        String title = mBinding.widget.name.getText().toString();
        String artist = mBinding.widget.play.getText().toString();
        mPlayers.setMetadata(title, artist, mChannel.getLogo(), getDefaultArtwork());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (addErrorCount() > 20) onErrorEnd(event);
        else if (mPlayers.addRetry() > event.getRetry()) checkError(event);
        else if (event.isDecode() && mPlayers.canToggleDecode()) onDecode(false);
        else if (event.isExo() && mPlayers.isExo()) onExoCheck(event);
        else fetch();
    }

    private void onExoCheck(ErrorEvent event) {
        if (event.getCode() == PlaybackException.ERROR_CODE_IO_UNSPECIFIED || event.getCode() >= PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED && event.getCode() <= PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED) mPlayers.setFormat(ExoUtil.getMimeType(event.getCode()));
        mPlayers.setMediaSource();
    }

    private void checkError(ErrorEvent event) {
        if (mChannel != null && mChannel.getPlayerType() == -1 && event.isUrl() && event.getRetry() > 0 && getToggleCount() < 2 && mPlayers.getPlayer() != Players.SYS) {
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
        fetch();
    }

    private void onErrorEnd(ErrorEvent event) {
        onErrorPlayer(event);
        resetError();
    }

    private void onErrorPlayer(ErrorEvent event) {
        showError(event.getMsg());
        mPlayers.reset();
        mPlayers.stop();
    }

    private void onError(ErrorEvent event) {
        onErrorPlayer(event);
        startFlow();
    }

    private void startFlow() {
        if (!Setting.isChange()) return;
        if (!mChannel.isLast()) {
            nextLine(true);
        } else {
            mChannel.setLine(0);
            nextChannel();
        }
    }

    private boolean prevGroup() {
        int position = mGroups.indexOf(mGroup) - 1;
        if (position < 0) position = mGroups.size() - 1;
        if (mGroup.equals(mGroups.get(position))) return false;
        mGroup = mGroups.get(position);
        if (mGroup.skip()) return prevGroup();
        mGroup.setPosition(mGroup.getChannel().size() - 1);
        return true;
    }

    private boolean nextGroup() {
        int position = mGroups.indexOf(mGroup) + 1;
        if (position > mGroups.size() - 1) position = 0;
        if (mGroup.equals(mGroups.get(position))) return false;
        mGroup = mGroups.get(position);
        if (mGroup.skip()) return nextGroup();
        mGroup.setPosition(0);
        return true;
    }

    private void prevChannel() {
        if (mGroup == null) return;
        int position = mGroup.getPosition() - 1;
        boolean limit = position < 0;
        if (Setting.isAcross() & limit) prevGroup();
        else mGroup.setPosition(limit ? mGroup.getChannel().size() - 1 : position);
        if (!mGroup.isEmpty()) onItemClick(mGroup.current());
    }

    private void nextChannel() {
        if (mGroup == null) return;
        int position = mGroup.getPosition() + 1;
        boolean limit = position > mGroup.getChannel().size() - 1;
        if (Setting.isAcross() && limit) nextGroup();
        else mGroup.setPosition(limit ? 0 : position);
        if (!mGroup.isEmpty()) onItemClick(mGroup.current());
    }

    private void checkNext() {
        int current = mChannel.getData().getInRange();
        int position = mChannel.getData().getSelected() + 1;
        boolean hasNext = position <= current && position > 0;
        if (hasNext) onItemClick(mChannel.getData().getList().get(position));
        else nextChannel();
    }

    private void prevLine() {
        if (mChannel == null || mChannel.isOnly()) return;
        mChannel.prevLine();
        showInfo();
        fetch();
    }

    private void nextLine(boolean show) {
        if (mChannel == null || mChannel.isOnly()) return;
        mChannel.nextLine();
        if (show) showInfo();
        else setInfo();
        fetch();
    }

    private void onPaused() {
        checkPlayImg(false);
        mPlayers.pause();
    }

    private void onPlay() {
        checkPlayImg(true);
        mPlayers.play();
    }

    public boolean isForeground() {
        return foreground;
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
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

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
        if (rotate) {
            noPadding(mBinding.control.getRoot());
        } else {
            setPadding(mBinding.control.getRoot());
        }
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

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
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

    private void stopService() {
        PlaybackService.stop();
    }

    @Override
    public void onCasted() {
    }

    @Override
    public void onSpeedUp() {
        if (!mPlayers.isVod() || !mPlayers.isPlaying() || !mPlayers.canAdjustSpeed()) return;
        mBinding.control.action.speed.setText(mPlayers.setSpeed(mPlayers.getSpeed() < 3 ? 3 : 5));
        mBinding.widget.speed.startAnimation(ResUtil.getAnim(R.anim.forward));
        mBinding.widget.speed.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSpeedEnd() {
        mBinding.control.action.speed.setText(mPlayers.setSpeed(1.0f));
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
    public void onFlingUp() {
        if (!mPlayers.isVod()) prevChannel();
    }

    @Override
    public void onFlingDown() {
        if (!mPlayers.isVod()) nextChannel();
    }

    @Override
    public void onFlingLeft() {
        if (!mPlayers.isVod()) prevLine();
    }

    @Override
    public void onFlingRight() {
        if (!mPlayers.isVod()) nextLine(true);
    }

    @Override
    public void onSeek(int time) {
        if (!mPlayers.isVod()) return;
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        mBinding.widget.time.setText(mPlayers.getPositionTime(time));
        mBinding.widget.seek.setVisibility(View.VISIBLE);
        hideProgress();
    }

    @Override
    public void onSeekEnd(int time) {
        if (!mPlayers.isVod()) return;
        mBinding.widget.seek.setVisibility(View.GONE);
        mPlayers.seekTo(time);
        showProgress();
        onPlay();
    }

    @Override
    public void onSingleTap() {
        onToggle();
    }

    @Override
    public void onDoubleTap() {
        onToggle();
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
        Setting.putLivePlayer(mPlayers.getPlayer());
        setPlayerView();
        setR1Callback();
        fetch();
    }

    @Override
    public void onPlayerShare(String title) {
        this.onShare(title);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isRedirect()) return;
        if (isLock()) App.post(this::onLock, 500);
        if (mPlayers.haveTrack(C.TRACK_TYPE_VIDEO)) mPiP.enter(this, mPlayers.getVideoWidth(), mPlayers.getVideoHeight(), Setting.getLiveScale());
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            PlaybackService.start(mPlayers);
            hideControl();
            hideInfo();
        } else {
            hideInfo();
            App.post(mR0, 1000);
            setForeground(true);
            if (isStop()) finish();
        }
        // 小窗进入/退出均按画中画状态切换布局（进入铺满视频隐藏 tab，退出按全屏状态恢复）
        setLayout();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 画中画小窗的窗口尺寸/方向变化不处理（setLayout 内已有画中画守卫，双保险）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        resizeVideo();
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
        if (Setting.isBackgroundOn() && !isFinishing()) PlaybackService.start(mPlayers);
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
        if (isFullscreen()) {
            setFullscreen(false);
            setLayout();
            showControl();
        } else if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.info)) {
            hideInfo();
        } else if (mBinding.tabLayout.getSelectedTabPosition() == 1) {
            mBinding.tabLayout.getTabAt(0).select();
        } else if (!isLock()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClock.release();
        mPlayers.release();
        App.post(mR0, 1000);
        App.removeCallbacks(mR1, mR2, mR3);
        mViewModel.url.removeObserver(mObserveUrl);
        mViewModel.epg.removeObserver(mObserveEpg);
    }
class TabPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = position == 0 ? mBinding.swipeLayout : mBinding.channelBox;
            view.setVisibility(View.VISIBLE);
            if (view.getParent() != null) ((ViewGroup) view.getParent()).removeView(view);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? getString(R.string.tab_play) : getString(R.string.tab_channel);
        }
    }
}
