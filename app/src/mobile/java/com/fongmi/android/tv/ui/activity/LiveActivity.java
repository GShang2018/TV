package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
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
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.PagerAdapter;

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
import com.fongmi.android.tv.ui.adapter.ChannelAdapter;
import com.fongmi.android.tv.ui.adapter.EpgDataAdapter;
import com.fongmi.android.tv.ui.adapter.LineAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.CastDialog;
import com.fongmi.android.tv.ui.dialog.ChannelChooseDialog;
import com.fongmi.android.tv.ui.dialog.InfoDialog;
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

public class LiveActivity extends BaseActivity implements Clock.Callback, CustomKeyDownLive.Listener, TrackDialog.Listener, PlayerDialog.Listener, LiveCallback, ChannelAdapter.OnClickListener, EpgDataAdapter.OnClickListener, LineAdapter.OnClickListener, CastDialog.Listener, InfoDialog.Listener {

    private ActivityLiveBinding mBinding;
    private View mShadow;
    private RelativeLayout.LayoutParams mVideoParams;
    private RelativeLayout.LayoutParams mShadowParams;
    private RelativeLayout.LayoutParams mContentParams;
    private boolean fullscreen;
    private ChannelAdapter mChannelAdapter;
    private EpgDataAdapter mEpgDataAdapter;
    private Observer<Channel> mObserveUrl;
    private CustomKeyDownLive mKeyDown;
    private Observer<Epg> mObserveEpg;
    private LiveViewModel mViewModel;
    private LineAdapter mLineAdapter;
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
        if (mBinding.epgData.getParent() != null) ((ViewGroup) mBinding.epgData.getParent()).removeView(mBinding.epgData);
        mBinding.tabPager.setAdapter(new TabPagerAdapter());
        mBinding.tabLayout.setupWithViewPager(mBinding.tabPager);
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.playCast.setOnClickListener(view -> onCast());
        mBinding.share.setOnClickListener(view -> onShareClick());
        mBinding.currentSite.setOnClickListener(view -> onHome());
        mBinding.allChannel.setOnClickListener(view -> onAllChannel());
        mBinding.swipeLayout.setOnRefreshListener(this::onSwipeRefresh);
        mBinding.currentSite.setOnClickListener(view -> onHome());
        mBinding.allChannel.setOnClickListener(view -> onAllChannel());
        mBinding.swipeLayout.setOnRefreshListener(this::onSwipeRefresh);
    }

    private void resizeVideo() {
        mBinding.video.post(this::setLayout);
    }

    private void setLayout() {
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
        mBinding.lineList.setHasFixedSize(true);
        mBinding.lineList.setItemAnimator(null);
        mBinding.lineList.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.lineList.setAdapter(mLineAdapter = new LineAdapter(this));
        mBinding.channel.setHasFixedSize(true);
        mBinding.channel.setItemAnimator(null);
        mBinding.channel.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.channel.setAdapter(mChannelAdapter = new ChannelAdapter(this));
        mBinding.epgData.setHasFixedSize(true);
        mBinding.epgData.setItemAnimator(null);
        mBinding.epgData.setAdapter(mEpgDataAdapter = new EpgDataAdapter(this));
    }

    private void setPlayerView() {
        getIjk().setPlayer(mPlayers.getPlayer());
        mBinding.control.action.speed.setText(mPlayers.getSpeedText());
        mBinding.control.action.player.setText(mPlayers.getPlayerText());
        mBinding.control.action.speed.setEnabled(mPlayers.canAdjustSpeed());
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
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
        mBinding.currentSite.setText(getHome().getName());
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
        if (!getGroupName().isEmpty()) {
            for (Group group : mGroups) {
                if (group.getName().equals(getGroupName())) {
                    mGroup = group;
                    int index = group.find(getChannelName());
                    if (index == -1) return;
                    mGroup.setPosition(index);
                    onItemClick(mGroup.current());
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
        mGroup = mGroups.get(position[0]);
        mGroup.setPosition(position[1]);
        if (mGroup.isEmpty()) return;
        onItemClick(mGroup.current());
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
        boolean controlVisible = isVisible(mBinding.control.getRoot()) || isVisible(mBinding.widget.info);
        boolean visible = (!controlVisible && !isLock());
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
        showInfo();
        fetch();
    }

    @Override
    public boolean onLongClick(Channel item) {
        if (mGroup == null || mGroup.isHidden()) return false;
        boolean exist = Keep.exist(item.getName());
        Notify.show(exist ? R.string.keep_del : R.string.keep_add);
        if (exist) delKeep(item);
        else addKeep(item);
        return true;
    }

    private void onAllChannel() {
        if (mGroup == null || mGroup.getChannel().isEmpty()) return;
        ChannelChooseDialog.create().items(mGroup.getChannel()).selected(mGroup.getPosition()).listener(this::onItemClick).show(this);
    }

    @Override
    public void onItemClick(EpgData item) {
        if (item.isFuture() || !mChannel.hasCatchup()) return;
        Notify.show(getString(R.string.play_ready, item.getTitle()));
        mEpgDataAdapter.setSelected(item);
        mViewModel.getUrl(mChannel, item);
        mPlayers.clear();
        mPlayers.stop();
        showProgress();
    }

    @Override
    public void onItemClick(int position) {
        if (mChannel == null || position == mChannel.getLine()) return;
        mChannel.setLine(position);
        setInfo();
        fetch();
    }

    private void addKeep(Channel item) {
        getKeep().add(item);
        Keep keep = new Keep();
        keep.setKey(item.getName());
        keep.setType(1);
        keep.save();
    }

    private void delKeep(Channel item) {
        if (mGroup != null && mGroup.isKeep()) mChannelAdapter.remove(item);
        if (mGroup != null) mGroup.getChannel().remove(item);
        Keep.delete(item.getName());
    }

    private void setInfo() {
        mViewModel.getEpg(mChannel);
        mBinding.widget.play.setText("");
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
        mBinding.widget.name.setMaxEms(mChannel.getName().length());
        mBinding.widget.line.setVisibility(mChannel.getLineVisible());
        mBinding.control.action.line.setText(mBinding.widget.line.getText());
        mBinding.control.action.line.setVisibility(mBinding.widget.line.getVisibility());
        setLineAdapter();
        setChannelAdapter();
        checkKeepImg();
    }

    private void setLineAdapter() {
        if (mChannel == null) return;
        List<String> lines = new ArrayList<>();
        for (String url : mChannel.getUrls()) lines.add(getLineName(url, lines.size()));
        mLineAdapter.addAll(lines);
        mLineAdapter.setSelected(mChannel.getLine());
    }

    private String getLineName(String url, int index) {
        if (url.contains("$")) return url.split("\\$")[1];
        return ResUtil.getString(R.string.live_line, index + 1);
    }

    private void setChannelAdapter() {
        if (mGroup == null) return;
        mChannelAdapter.addAll(mGroup.getChannel());
        mChannelAdapter.setSelected(mGroup.getPosition());
        mBinding.allChannel.setText("全部 " + mGroup.getChannel().size());
        mBinding.channel.scrollToPosition(Math.max(mGroup.getPosition(), 0));
    }

    private void setEpg() {
        String epg = mChannel.getData().getEpg();
        List<EpgData> data = mChannel.getData().getList();
        if (epg.length() > 0) mBinding.widget.name.setMaxEms(12);
        mBinding.widget.play.setText(epg);
        mBinding.playEpg.setText(epg);
        mChannelAdapter.changed(mChannel);
        mEpgDataAdapter.addAll(data);
        mBinding.epgData.scrollToPosition(Math.max(mChannel.getData().getSelected(), 0));
        setMetadata();
    }

    private void setEpg(boolean success) {
        if (mChannel != null && success) mViewModel.getEpg(mChannel);
    }

    private void setEpg(Epg epg) {
        if (mChannel != null && mChannel.getTvgName().equals(epg.getKey())) setEpg();
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
        mLineAdapter.addAll(new ArrayList<>());
        mChannelAdapter.clear();
        mEpgDataAdapter.clear();
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
        else mGroup.setPosition(limit ? mChannelAdapter.getItemCount() - 1 : position);
        if (!mGroup.isEmpty()) onItemClick(mGroup.current());
    }

    private void nextChannel() {
        if (mGroup == null) return;
        int position = mGroup.getPosition() + 1;
        boolean limit = position > mChannelAdapter.getItemCount() - 1;
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
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
            View view = position == 0 ? mBinding.swipeLayout : mBinding.epgData;
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
            return position == 0 ? getString(R.string.tab_play) : getString(R.string.tab_epg);
        }
    }
}
