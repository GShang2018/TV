package androidx.media3.mpvplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import is.xyz.mpv.MPVLib;

/**
 * 精简版 MPV 播放内核。
 * 参考 H:\webtv 项目的 MpvPlayer，按 h:\TV 现有架构精简：
 * 移除 webtv 特有的 HLS 代理 / ISO 会话 / LUT 滤镜 / 网络恢复策略 / 性能统计等高级特性，
 * 保留 SimpleBasePlayer 骨架 + MPVLib 核心播放能力（播放/暂停/seek/倍速/音量/音轨/字幕）。
 */
@UnstableApi
public final class MpvPlayer extends SimpleBasePlayer implements MPVLib.EventObserver, MPVLib.LogObserver {

	private static final String TAG = "TV-mpv";
	private static final long STATE_REFRESH_INTERVAL_MS = 1000;
	private static final long END_FILE_VALIDATION_DELAY_MS = 800;
	private static final double SECONDS_TO_MS = 1000.0;
	private static final String HLS_LOAD_OPTIONS = "demuxer=lavf,demuxer-lavf-format=hls,demuxer-lavf-probesize=10485760,demuxer-lavf-analyzeduration=5";
	private static final String DASH_LOAD_OPTIONS = "demuxer=lavf,demuxer-lavf-format=dash,demuxer-lavf-probesize=10485760,demuxer-lavf-analyzeduration=5";
	private static final String HEADER_ACCEPT = "Accept";
	private static final Object NATIVE_CONTEXT_LOCK = new Object();
	private static MpvPlayer nativeContextOwner;

	private static final Commands COMMANDS = new Commands.Builder()
			.add(COMMAND_PLAY_PAUSE)
			.add(COMMAND_PREPARE)
			.add(COMMAND_STOP)
			.add(COMMAND_RELEASE)
			.add(COMMAND_SET_REPEAT_MODE)
			.add(COMMAND_GET_CURRENT_MEDIA_ITEM)
			.add(COMMAND_GET_TIMELINE)
			.add(COMMAND_GET_METADATA)
			.add(COMMAND_SET_MEDIA_ITEM)
			.add(COMMAND_CHANGE_MEDIA_ITEMS)
			.add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
			.add(COMMAND_SEEK_TO_DEFAULT_POSITION)
			.add(COMMAND_GET_VOLUME)
			.add(COMMAND_SET_VOLUME)
			.add(COMMAND_SET_SPEED_AND_PITCH)
			.add(COMMAND_SET_VIDEO_SURFACE)
			.add(COMMAND_GET_TRACKS)
			.build();

	private final Context context;
	private final Handler mainHandler;
	private final Runnable stateRefreshRunnable;
	private final Runnable endFileValidationRunnable;
	private final Runnable trackRefreshRunnable;
	private final List<ParcelFileDescriptor> contentFds;

	private MediaItem mediaItem;
	private SurfaceHolder surfaceHolder;
	private Surface surface;
	private Surface attachedSurface;
	private Object videoOutput;
	private String currentPlayableUri;
	private PlaybackParameters playbackParameters;
	private PlaybackException playerError;
	private Tracks currentTracks;
	private VideoSize videoSize;
	private int playbackState;
	private long pendingSeekPositionMs;
	private long cachedPositionMs;
	private long cachedDurationMs;
	private long cachedCacheDurationMs;
	private boolean playWhenReady;
	private boolean loading;
	private boolean repeatOne;
	private boolean ownsSurface;
	private boolean initialized;
	private boolean released;
	private boolean surfaceAttached;
	private boolean fileLoaded;
	private boolean loadStarted;
	private boolean playbackRestarted;
	private boolean stopping;
	private boolean eofReached;
	private boolean idleActive;
	private boolean trackRefreshScheduled;
	private boolean sawNoAvData;
	private boolean sawInvalidData;
	private boolean sawNetworkError;
	private boolean sawDecodeError;
	private boolean sawDrmError;
	private int surfaceWidth;
	private int surfaceHeight;
	private float volume;
	/** surface 未就绪时挂起的加载：等待 surfaceCreated 后再 attach 并 loadfile */
	private boolean loadPendingOnSurface;

	/** 当前设备是否支持 MPV 内核（是否存在配套 native 库） */
	public static boolean isAvailable(Context context) {
		try {
			return MPVLib.isSupported(context);
		} catch (Throwable e) {
			return false;
		}
	}

	public MpvPlayer(Context context) {
		super(Looper.getMainLooper());
		this.context = context.getApplicationContext();
		this.mainHandler = new Handler(Looper.getMainLooper());
		this.stateRefreshRunnable = this::refreshPlaybackState;
		this.endFileValidationRunnable = this::validateEarlyEndFile;
		this.trackRefreshRunnable = this::runScheduledTrackRefresh;
		this.contentFds = new ArrayList<>();
		this.playbackParameters = PlaybackParameters.DEFAULT;
		this.currentTracks = Tracks.EMPTY;
		this.videoSize = VideoSize.UNKNOWN;
		this.playbackState = Player.STATE_IDLE;
		this.pendingSeekPositionMs = C.TIME_UNSET;
		this.cachedDurationMs = C.TIME_UNSET;
		this.playWhenReady = true;
		this.volume = 1f;
	}

	@Override
	protected State getState() {
		int state = playbackState;
		MediaItem currentItem = mediaItem;
		if (currentItem == null && state != Player.STATE_IDLE && state != Player.STATE_ENDED) {
			state = Player.STATE_IDLE;
		}
		State.Builder builder = new State.Builder()
				.setAvailableCommands(COMMANDS)
				.setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
				.setPlaybackState(state)
				.setIsLoading(currentItem != null && loading && state != Player.STATE_IDLE && state != Player.STATE_ENDED)
				.setPlayerError(playerError)
				.setRepeatMode(repeatOne ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF)
				.setPlaybackParameters(playbackParameters)
				.setVideoSize(videoSize)
				.setVolume(volume)
				.setPlaylist(currentItem == null ? ImmutableList.of() : ImmutableList.of(mediaItemData(currentItem)))
				.setCurrentMediaItemIndex(currentItem == null ? C.INDEX_UNSET : 0);
		if (currentItem != null) {
			long duration = durationMs();
			long position = positionMs();
			PositionSupplier positionSupplier = isPlayingInternal()
					? PositionSupplier.getExtrapolating(position, playbackParameters.speed)
					: PositionSupplier.getConstant(position);
			builder.setContentPositionMs(positionSupplier);
			builder.setContentBufferedPositionMs(PositionSupplier.getConstant(bufferedPositionMs(position, duration)));
			builder.setTotalBufferedDurationMs(PositionSupplier.getConstant(Math.max(0, bufferedPositionMs(position, duration) - position)));
		}
		return builder.build();
	}

	private MediaItemData mediaItemData(MediaItem item) {
		long duration = durationMs();
		return new MediaItemData.Builder(item.mediaId)
				.setMediaItem(item)
				.setMediaMetadata(item.mediaMetadata)
				.setDurationUs(duration == C.TIME_UNSET ? C.TIME_UNSET : duration * 1000)
				.setIsSeekable(duration > 0)
				.setIsDynamic(duration == C.TIME_UNSET)
				.setTracks(currentTracks)
				.build();
	}

	@Override
	protected ListenableFuture<?> handleSetMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
		cancelScheduledTrackRefresh();
		mediaItem = mediaItems.isEmpty() ? null : mediaItems.get(0);
		pendingSeekPositionMs = mediaItem != null && startPositionMs > 0 ? startPositionMs : C.TIME_UNSET;
		cachedPositionMs = Math.max(0, startPositionMs == C.TIME_UNSET ? 0 : startPositionMs);
		cachedDurationMs = C.TIME_UNSET;
		currentTracks = Tracks.EMPTY;
		playbackState = Player.STATE_IDLE;
		loading = false;
		fileLoaded = false;
		playbackRestarted = false;
		loadStarted = false;
		eofReached = false;
		idleActive = false;
		currentPlayableUri = null;
		loadPendingOnSurface = false;
		playerError = null;
		stopping = false;
		mainHandler.removeCallbacks(endFileValidationRunnable);
		closeContentFds();
		if (mediaItem != null && initialized) {
			releaseNativeContext("new media");
		}
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleAddMediaItems(int index, List<MediaItem> mediaItems) {
		if (mediaItem == null && !mediaItems.isEmpty()) mediaItem = mediaItems.get(0);
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleReplaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
		if (mediaItems.isEmpty()) {
			stopInternal(true);
			mediaItem = null;
			invalidateState();
		} else {
			mediaItem = mediaItems.get(0);
		}
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleRemoveMediaItems(int fromIndex, int toIndex) {
		stopInternal(true);
		mediaItem = null;
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handlePrepare() {
		openCurrent();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
		this.playWhenReady = playWhenReady;
		if (initialized && playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
			safeSetPropertyBoolean("pause", !playWhenReady);
		}
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleStop() {
		stopInternal(true);
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleRelease() {
		released = true;
		cancelScheduledTrackRefresh();
		stopInternal(false);
		clearVideoOutput();
		mainHandler.removeCallbacks(stateRefreshRunnable);
		mainHandler.removeCallbacks(endFileValidationRunnable);
		releaseNativeContext("release");
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleSetRepeatMode(int repeatMode) {
		this.repeatOne = repeatMode == Player.REPEAT_MODE_ONE;
		if (initialized) safeSetPropertyString("loop-file", repeatOne ? "inf" : "no");
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleSeek(int mediaItemIndex, long positionMs, int seekCommand) {
		if (mediaItemIndex != 0 || mediaItem == null) return Futures.immediateVoidFuture();
		if (fileLoaded) {
			seekMpv(positionMs);
		} else {
			pendingSeekPositionMs = positionMs;
		}
		cachedPositionMs = Math.max(0, positionMs);
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleSetPlaybackParameters(PlaybackParameters playbackParameters) {
		this.playbackParameters = playbackParameters;
		if (initialized && playbackState != Player.STATE_IDLE) {
			safeSetPropertyDouble("speed", playbackParameters.speed);
		}
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleSetVolume(float volume) {
		this.volume = volume;
		if (initialized) safeSetPropertyDouble("volume", volume * 100.0);
		invalidateState();
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleSetVideoOutput(Object videoOutput) {
		this.videoOutput = videoOutput;
		setVideoOutput(videoOutput);
		return Futures.immediateVoidFuture();
	}

	@Override
	protected ListenableFuture<?> handleClearVideoOutput(@Nullable Object videoOutput) {
		if (videoOutput == null || videoOutput == this.videoOutput) {
			this.videoOutput = null;
			clearVideoOutput();
		}
		return Futures.immediateVoidFuture();
	}

	@Override
	public void eventProperty(String property) {
		postToMain(() -> handleProperty(property, null));
	}

	@Override
	public void eventProperty(String property, long value) {
		postToMain(() -> handleProperty(property, value));
	}

	@Override
	public void eventProperty(String property, boolean value) {
		postToMain(() -> handleProperty(property, value));
	}

	@Override
	public void eventProperty(String property, String value) {
		postToMain(() -> handleProperty(property, value));
	}

	@Override
	public void eventProperty(String property, double value) {
		postToMain(() -> handleProperty(property, value));
	}

	@Override
	public void event(int eventId) {
		postToMain(() -> handleEvent(eventId));
	}

	@Override
	public void endFile(int reason, int error, String errorText) {
		postToMain(() -> handleEndFile(reason, error, errorText));
	}

	@Override
	public void logMessage(String prefix, int level, String text) {
		postToMain(() -> {
			if (released) return;
			markFailureSignal(prefix + ": " + text);
		});
	}

	private void openCurrent() {
		if (mediaItem == null || mediaItem.localConfiguration == null) return;
		try {
			ensureInitialized();
			playbackState = Player.STATE_BUFFERING;
			loading = true;
			playerError = null;
			fileLoaded = false;
			loadStarted = false;
			playbackRestarted = false;
			eofReached = false;
			idleActive = false;
			cachedDurationMs = C.TIME_UNSET;
			mainHandler.removeCallbacks(endFileValidationRunnable);
			closeContentFds();
			if (mediaItem.localConfiguration.drmConfiguration != null) {
				fail(new IOException("MPV does not support DRM media"), PlaybackException.ERROR_CODE_DRM_UNSPECIFIED);
				return;
			}
			applyMediaOptions(mediaItem);
			boolean attached = bindVideoOutput();
			safeSetPropertyBoolean("pause", !playWhenReady);
			safeSetPropertyString("loop-file", repeatOne ? "inf" : "no");
			safeSetPropertyDouble("speed", playbackParameters.speed);
			safeSetPropertyDouble("volume", volume * 100.0);
			currentPlayableUri = playableUri(mediaItem);
			if (attached) {
				loadCurrentUri(isLikelyHls(mediaItem, currentPlayableUri), isLikelyDash(mediaItem, currentPlayableUri));
			} else {
				// 渲染 surface 尚未就绪：挂起加载，等 surfaceCreated 后再 attach + loadfile，
				// 避免在无渲染表面时初始化视频输出导致崩溃/卡死
				loadPendingOnSurface = true;
			}
			invalidateState();
			startStateRefresh();
		} catch (Throwable e) {
			fail(e instanceof IOException ? (IOException) e : new IOException(e.getMessage(), e), PlaybackException.ERROR_CODE_IO_UNSPECIFIED);
		}
	}

	private void ensureInitialized() throws IOException {
		synchronized (NATIVE_CONTEXT_LOCK) {
			if (initialized) return;
			if (nativeContextOwner != null && nativeContextOwner != this) {
				nativeContextOwner.released = true;
				nativeContextOwner.releaseNativeContextLocked("takeover");
			}
			if (!MPVLib.ensureLoaded(context)) {
				Throwable e = MPVLib.getLoadError();
				if (e instanceof IOException) throw (IOException) e;
				if (e instanceof RuntimeException) throw (RuntimeException) e;
				throw new IOException(e == null ? "MPV native libraries are unavailable" : e.getMessage(), e);
			}
			nativeContextOwner = this;
			if (!MPVLib.tryCreate(context)) {
				// 上次创建未完成：清理残留标记，避免本次失败后 contextCreationAttempted 永久卡死
				MPVLib.destroyCreatedContext();
				nativeContextOwner = null;
				throw new IOException("MPV native context creation is already in progress");
			}
			try {
				applyPreInitOptions();
				MPVLib.init();
			} catch (Throwable e) {
				// init 失败：释放已创建的上下文并重置标记，确保下次播放可重新创建
				MPVLib.destroyCreatedContext();
				nativeContextOwner = null;
				throw e instanceof IOException ? (IOException) e : new IOException(e.getMessage(), e);
			}
			initialized = true;
			MPVLib.addObserver(this);
			MPVLib.addLogObserver(this);
			applyPostInitOptions();
			observeProperties();
		}
	}

	private void applyPreInitOptions() {
		setOption("config", "yes");
		setOption("config-dir", context.getFilesDir().getAbsolutePath());
		setOption("vo", "gpu");
		setOption("gpu-context", "android");
		setOption("opengl-es", "yes");
		setOption("hwdec", "mediacodec,mediacodec-copy");
		setOption("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1");
		setOption("ao", "audiotrack,opensles");
		setOption("audio-set-media-role", "yes");
		setOption("tls-verify", "no");
		setOption("input-default-bindings", "yes");
		setOption("cache", "yes");
		setOption("cache-secs", "20");
		setOption("demuxer-thread", "yes");
		setOption("demuxer-seekable-cache", "auto");
		setOption("demuxer-max-bytes", "67108864");
		setOption("demuxer-max-back-bytes", "67108864");
		setOption("demuxer-readahead-secs", "20");
		// 字幕渲染：libmpv 内置 libass 但不含 fontconfig，直接索引系统字体目录
		setOption("sub-ass", "yes");
		setOption("sub-ass-override", "yes");
		setOption("embeddedfonts", "yes");
		setOption("sub-fix-timing", "yes");
		setOption("sub-use-margins", "yes");
		setOption("sub-font-provider", "none");
		setOption("msg-level", "all=v");
	}

	private void applyPostInitOptions() {
		setRuntimeString("save-position-on-quit", "no");
		setRuntimeString("force-window", "no");
		setRuntimeString("idle", "yes");
	}

	private void observeProperties() {
		observe("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE);
		observe("time-pos/full", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE);
		observe("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE);
		observe("duration/full", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE);
		observe("demuxer-cache-duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE);
		observe("demuxer-cache-state/cache-duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE);
		observe("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG);
		observe("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG);
		observe("eof-reached", MPVLib.MpvFormat.MPV_FORMAT_FLAG);
		observe("idle-active", MPVLib.MpvFormat.MPV_FORMAT_FLAG);
		observe("width", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("height", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("video-params/w", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("video-params/h", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("video-params/dw", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("video-params/dh", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("track-list/count", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("vid", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("aid", MPVLib.MpvFormat.MPV_FORMAT_INT64);
		observe("sid", MPVLib.MpvFormat.MPV_FORMAT_INT64);
	}

	private void handleProperty(String property, @Nullable Object value) {
		if (released) return;
		if (mediaItem == null) {
			playbackState = Player.STATE_IDLE;
			loading = false;
			return;
		}
		switch (property) {
			case "time-pos":
			case "time-pos/full":
				cachedPositionMs = doubleSecondsToMs(value, cachedPositionMs);
				break;
			case "duration":
			case "duration/full":
				cachedDurationMs = doubleSecondsToMs(value, cachedDurationMs);
				break;
			case "demuxer-cache-duration":
			case "demuxer-cache-state/cache-duration":
				cachedCacheDurationMs = Math.max(0, doubleSecondsToMs(value, cachedCacheDurationMs));
				break;
			case "pause":
				if (value instanceof Boolean) playWhenReady = !((Boolean) value);
				break;
			case "paused-for-cache":
				loading = Boolean.TRUE.equals(value);
				if (loading) playbackState = Player.STATE_BUFFERING;
				else if (playbackState == Player.STATE_BUFFERING && fileLoaded && playbackRestarted) playbackState = Player.STATE_READY;
				break;
			case "eof-reached":
				eofReached = Boolean.TRUE.equals(value);
				// mpv 初始化（未加载媒体）时 eof-reached 也会回推 true，
				// 必须等 fileLoaded 后才算真正播放结束，否则会在播放启动瞬间误判 ENDED
				if (eofReached && fileLoaded && !stopping) markPlaybackEnded("property:eof-reached");
				break;
			case "idle-active":
				idleActive = Boolean.TRUE.equals(value);
				if (idleActive && fileLoaded && !stopping) markPlaybackEnded("property:idle-active");
				break;
			case "width":
			case "height":
			case "video-params/w":
			case "video-params/h":
			case "video-params/dw":
			case "video-params/dh":
				updateVideoSize("property:" + property);
				break;
			case "track-list/count":
				updateVideoSize("property:" + property);
				scheduleTrackRefresh();
				break;
			case "vid":
			case "aid":
			case "sid":
				scheduleTrackRefresh();
				break;
			default:
		}
		invalidateState();
	}

	private void handleEvent(int eventId) {
		if (released) return;
		if (mediaItem == null && eventId != MPVLib.MpvEvent.MPV_EVENT_SHUTDOWN) {
			playbackState = Player.STATE_IDLE;
			loading = false;
			invalidateState();
			return;
		}
		switch (eventId) {
			case MPVLib.MpvEvent.MPV_EVENT_START_FILE:
				loadStarted = true;
				playbackState = Player.STATE_BUFFERING;
				loading = true;
				fileLoaded = false;
				playbackRestarted = false;
				stopping = false;
				eofReached = false;
				idleActive = false;
				mainHandler.removeCallbacks(endFileValidationRunnable);
				startStateRefresh();
				break;
			case MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED:
				fileLoaded = true;
				mainHandler.removeCallbacks(endFileValidationRunnable);
				playbackState = Player.STATE_BUFFERING;
				loading = true;
				cachedDurationMs = durationMs();
				updateVideoSize("event=file-loaded");
				refreshTracks();
				if (pendingSeekPositionMs != C.TIME_UNSET) {
					seekMpv(pendingSeekPositionMs);
					pendingSeekPositionMs = C.TIME_UNSET;
				}
				safeSetPropertyBoolean("pause", !playWhenReady);
				addSubtitleConfigurations();
				startStateRefresh();
				break;
			case MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART:
				playbackRestarted = true;
				updateVideoSize("event=playback-restart");
				refreshTracks();
				if (playbackState != Player.STATE_ENDED) {
					playbackState = Player.STATE_READY;
					loading = false;
					startStateRefresh();
				}
				break;
			case MPVLib.MpvEvent.MPV_EVENT_VIDEO_RECONFIG:
				updateVideoSize("event=video-reconfig");
				break;
			case MPVLib.MpvEvent.MPV_EVENT_END_FILE:
				handleEndFile(MPVLib.MpvEndFileReason.MPV_END_FILE_REASON_UNKNOWN, MPVLib.MpvError.MPV_ERROR_SUCCESS, null);
				return;
			case MPVLib.MpvEvent.MPV_EVENT_IDLE:
				if (fileLoaded && !stopping) {
					markPlaybackEnded("event:idle");
				} else if (loading && !stopping) {
					playbackState = Player.STATE_BUFFERING;
					mainHandler.removeCallbacks(endFileValidationRunnable);
					mainHandler.postDelayed(endFileValidationRunnable, END_FILE_VALIDATION_DELAY_MS);
					startStateRefresh();
				}
				break;
			case MPVLib.MpvEvent.MPV_EVENT_SHUTDOWN:
				if (fileLoaded && !stopping) {
					markPlaybackEnded("event:shutdown");
				} else {
					playbackState = Player.STATE_IDLE;
					loading = false;
					stopStateRefresh();
				}
				break;
			default:
		}
		invalidateState();
	}

	private void handleEndFile(int reason, int error, @Nullable String errorText) {
		if (released) return;
		if (stopping) {
			stopping = false;
			return;
		}
		stopStateRefresh();
		loading = false;
		if (reason == MPVLib.MpvEndFileReason.MPV_END_FILE_REASON_ERROR) {
			fail(mpvError(nativeEndFileErrorCode(error), nativeEndFileDetail(reason, error, errorText)), nativeEndFilePlaybackExceptionCode(error));
			return;
		}
		if (isSuccessfulNaturalEof(reason, error)) {
			playbackState = Player.STATE_ENDED;
		} else if (fileLoaded || eofReached) {
			markPlaybackEnded("event:end-file");
		} else {
			loading = true;
			playbackState = Player.STATE_BUFFERING;
			mainHandler.removeCallbacks(endFileValidationRunnable);
			mainHandler.postDelayed(endFileValidationRunnable, END_FILE_VALIDATION_DELAY_MS);
			startStateRefresh();
		}
		invalidateState();
	}

	private void markPlaybackEnded(String reason) {
		if (playbackState == Player.STATE_ENDED) return;
		eofReached = true;
		loading = false;
		playbackState = Player.STATE_ENDED;
		stopStateRefresh();
		mainHandler.removeCallbacks(endFileValidationRunnable);
	}

	private boolean isSuccessfulNaturalEof(int reason, int error) {
		if (reason != MPVLib.MpvEndFileReason.MPV_END_FILE_REASON_EOF || error != MPVLib.MpvError.MPV_ERROR_SUCCESS || !fileLoaded) return false;
		if (eofReached) return true;
		long duration = cachedDurationMs;
		long position = cachedPositionMs;
		if (duration == C.TIME_UNSET || duration <= 0) return playbackRestarted && position > 0 && !sawNetworkError && !sawDecodeError;
		long tolerance = Math.max(3000L, Math.min(15000L, duration / 100L));
		return position >= Math.max(0, duration - tolerance);
	}

	private boolean isLikelyHls(MediaItem item, String uri) {
		if (uri == null || uri.startsWith("edl://")) return false;
		String lower = uri.toLowerCase(Locale.US);
		if (lower.contains(".m3u8")) return true;
		if (item.localConfiguration != null) {
			String mime = item.localConfiguration.mimeType;
			if (MimeTypes.APPLICATION_M3U8.equals(mime)) return true;
			if (mime != null && (mime.contains("m3u8") || mime.contains("mpegurl"))) return true;
		}
		return false;
	}

	private boolean isLikelyDash(MediaItem item, String uri) {
		if (uri == null || uri.startsWith("edl://")) return false;
		String lower = uri.toLowerCase(Locale.US);
		if (lower.contains(".mpd")) return true;
		if (item.localConfiguration != null) {
			String mime = item.localConfiguration.mimeType;
			if (MimeTypes.APPLICATION_MPD.equals(mime)) return true;
			if (mime != null && (mime.contains("mpd") || mime.contains("dash"))) return true;
		}
		return false;
	}

	private Map<String, String> applyMediaOptions(MediaItem item) {
		Map<String, String> headers = new LinkedHashMap<>(extractHeaders(item));
		String userAgent = findHeader(headers, HttpHeaders.USER_AGENT);
		String referer = findHeader(headers, HttpHeaders.REFERER);
		if (TextUtils.isEmpty(referer) && item.localConfiguration != null) referer = originOf(item.localConfiguration.uri);
		if (!TextUtils.isEmpty(userAgent)) putHeader(headers, HttpHeaders.USER_AGENT, userAgent);
		if (!TextUtils.isEmpty(referer)) putHeader(headers, HttpHeaders.REFERER, referer);
		if (TextUtils.isEmpty(findHeader(headers, HEADER_ACCEPT))) putHeader(headers, HEADER_ACCEPT, "*/*");
		setRuntimeString("user-agent", userAgent == null ? "" : userAgent);
		setRuntimeString("referrer", referer == null ? "" : referer);
		setRuntimeString("http-header-fields", buildHeaderFields(headers));
		if (item.mediaMetadata.title != null) setRuntimeString("force-media-title", item.mediaMetadata.title.toString());
		return headers;
	}

	private Map<String, String> extractHeaders(MediaItem item) {
		if (item.requestMetadata.extras == null) return Map.of();
		android.os.Bundle extras = item.requestMetadata.extras;
		LinkedHashMap<String, String> headers = new LinkedHashMap<>();
		for (String key : extras.keySet()) {
			String value = extras.getString(key);
			if (value != null) headers.put(key, value);
		}
		return headers;
	}

	private String buildHeaderFields(Map<String, String> headers) {
		if (headers.isEmpty()) return "";
		List<String> fields = new ArrayList<>();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String key = entry.getKey();
			if (equalsHeader(key, HttpHeaders.USER_AGENT) || equalsHeader(key, HttpHeaders.REFERER) || equalsHeader(key, HttpHeaders.RANGE)) continue;
			fields.add(key + ": " + escapeListValue(entry.getValue()));
		}
		return String.join(",", fields);
	}

	private void putHeader(Map<String, String> headers, String name, String value) {
		if (TextUtils.isEmpty(value)) return;
		String existing = null;
		for (String key : headers.keySet()) {
			if (equalsHeader(key, name)) {
				existing = key;
				break;
			}
		}
		headers.put(existing == null ? name : existing, value.trim());
	}

	private String escapeListValue(String value) {
		if (value == null) return "";
		return value.replace("\\", "\\\\").replace(",", "\\,");
	}

	@Nullable
	private String findHeader(Map<String, String> headers, String name) {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (equalsHeader(entry.getKey(), name)) return entry.getValue();
		}
		return null;
	}

	private boolean equalsHeader(String a, String b) {
		return a != null && a.equalsIgnoreCase(b);
	}

	@Nullable
	private String originOf(Uri uri) {
		if (uri == null || TextUtils.isEmpty(uri.getScheme()) || TextUtils.isEmpty(uri.getHost())) return null;
		String scheme = uri.getScheme();
		int port = uri.getPort();
		if (port > 0 && port != 80 && port != 443) return scheme + "://" + uri.getHost() + ":" + port;
		return scheme + "://" + uri.getHost();
	}

	private String playableUri(MediaItem item) throws IOException {
		Uri uri = item.localConfiguration.uri;
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, "r");
			if (fd == null) throw new IOException("Unable to open content uri: " + uri);
			contentFds.add(fd);
			return "fd://" + fd.getFd();
		}
		return uri.toString();
	}

	private void addSubtitleConfigurations() {
		if (mediaItem == null || mediaItem.localConfiguration == null || mediaItem.localConfiguration.subtitleConfigurations.isEmpty()) return;
		for (MediaItem.SubtitleConfiguration sub : mediaItem.localConfiguration.subtitleConfigurations) {
			try {
				MPVLib.command(new String[]{"sub-add", sub.uri.toString(), "auto"});
			} catch (Throwable ignored) {
			}
		}
	}

	private void setVideoOutput(Object output) {
		detachSurfaceHolder();
		surfaceWidth = 0;
		surfaceHeight = 0;
		if (output instanceof SurfaceView) {
			setSurfaceHolder(((SurfaceView) output).getHolder());
		} else if (output instanceof TextureView && ((TextureView) output).getSurfaceTexture() != null) {
			releaseOwnedSurface();
			surface = new Surface(((TextureView) output).getSurfaceTexture());
			ownsSurface = true;
		} else if (output instanceof SurfaceHolder) {
			setSurfaceHolder((SurfaceHolder) output);
		} else if (output instanceof Surface) {
			releaseOwnedSurface();
			surface = (Surface) output;
			ownsSurface = false;
		}
		bindVideoOutput();
	}

	private void setSurfaceHolder(SurfaceHolder holder) {
		surfaceHolder = holder;
		updateSurfaceSize(holder);
		surfaceHolder.addCallback(surfaceCallback);
		surface = surfaceHolder.getSurface();
		ownsSurface = false;
	}

	private boolean bindVideoOutput() {
		if (!initialized || surface == null || !surface.isValid()) return false;
		try {
			if (surfaceAttached && attachedSurface == surface) {
				setRuntimeString("force-window", "yes");
				applyAndroidSurfaceSize();
				return true;
			}
			if (surfaceAttached) detachMpvSurface();
			MPVLib.attachSurface(surface);
			surfaceAttached = true;
			attachedSurface = surface;
			setRuntimeString("force-window", "yes");
			applyAndroidSurfaceSize();
			return true;
		} catch (Throwable e) {
			fail(new IOException("video output failed: " + e.getMessage(), e), PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED);
			return false;
		}
	}

	private void clearVideoOutput() {
		detachSurfaceHolder();
		detachMpvSurface();
		releaseOwnedSurface();
		surface = null;
		surfaceWidth = 0;
		surfaceHeight = 0;
	}

	private void detachMpvSurface() {
		if (!initialized || !surfaceAttached) return;
		try {
			safeSetPropertyString("vo", "null");
			setRuntimeString("force-window", "no");
			MPVLib.detachSurface();
		} catch (Throwable ignored) {
		}
		surfaceAttached = false;
		attachedSurface = null;
	}

	private void detachSurfaceHolder() {
		if (surfaceHolder == null) return;
		try {
			surfaceHolder.removeCallback(surfaceCallback);
		} catch (Throwable ignored) {
		}
		surfaceHolder = null;
	}

	private void updateSurfaceSize(SurfaceHolder holder) {
		try {
			android.graphics.Rect frame = holder.getSurfaceFrame();
			if (frame != null) {
				surfaceWidth = frame.width();
				surfaceHeight = frame.height();
			}
		} catch (Throwable ignored) {
		}
	}

	private void applyAndroidSurfaceSize() {
		if (surfaceWidth <= 0 || surfaceHeight <= 0) return;
		try {
			MPVLib.setPropertyString("android-surface-size", surfaceWidth + "x" + surfaceHeight);
		} catch (Throwable ignored) {
		}
	}

	private void releaseOwnedSurface() {
		if (ownsSurface && surface != null) {
			try {
				surface.release();
			} catch (Throwable ignored) {
			}
			ownsSurface = false;
		}
	}

	private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			surface = holder.getSurface();
			onSurfaceReady();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			surfaceWidth = width;
			surfaceHeight = height;
			applyAndroidSurfaceSize();
			onSurfaceReady();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			surface = null;
			detachMpvSurface();
		}
	};

	/** surface 就绪回调：若存在挂起的加载则补 attach + loadfile */
	private void onSurfaceReady() {
		if (released || mediaItem == null) {
			bindVideoOutput();
			return;
		}
		if (!loadPendingOnSurface) {
			bindVideoOutput();
			return;
		}
		loadPendingOnSurface = false;
		if (bindVideoOutput()) {
			loadCurrentUri(isLikelyHls(mediaItem, currentPlayableUri), isLikelyDash(mediaItem, currentPlayableUri));
			invalidateState();
		}
	}

	private void stopInternal(boolean resetState) {
		cancelScheduledTrackRefresh();
		stopMpv(true);
		closeContentFds();
		loading = false;
		fileLoaded = false;
		loadStarted = false;
		playbackRestarted = false;
		eofReached = false;
		cachedPositionMs = 0;
		cachedDurationMs = C.TIME_UNSET;
		currentTracks = Tracks.EMPTY;
		videoSize = VideoSize.UNKNOWN;
		playerError = null;
		pendingSeekPositionMs = C.TIME_UNSET;
		idleActive = false;
		currentPlayableUri = null;
		loadPendingOnSurface = false;
		mainHandler.removeCallbacks(endFileValidationRunnable);
		if (resetState) playbackState = Player.STATE_IDLE;
		stopStateRefresh();
		invalidateState();
	}

	private void stopMpv(boolean markStopping) {
		if (!initialized) return;
		if (markStopping) stopping = true;
		try {
			MPVLib.command(new String[]{"stop"});
		} catch (Throwable ignored) {
			stopping = false;
		}
	}

	private void releaseNativeContext(String reason) {
		synchronized (NATIVE_CONTEXT_LOCK) {
			releaseNativeContextLocked(reason);
		}
	}

	private void releaseNativeContextLocked(String reason) {
		if (!initialized) return;
		boolean ownsNativeContext = nativeContextOwner == this;
		try {
			if (ownsNativeContext && surfaceAttached) MPVLib.detachSurface();
		} catch (Throwable ignored) {
		}
		try {
			MPVLib.removeObserver(this);
			MPVLib.removeLogObserver(this);
			if (ownsNativeContext) MPVLib.destroyCreatedContext();
		} catch (Throwable ignored) {
		} finally {
			if (ownsNativeContext) nativeContextOwner = null;
			initialized = false;
			surfaceAttached = false;
			attachedSurface = null;
			stopping = false;
			loadStarted = false;
		}
	}

	private void seekMpv(long positionMs) {
		try {
			MPVLib.command(new String[]{"seek", String.format(Locale.US, "%.3f", positionMs / SECONDS_TO_MS), "absolute+exact"});
		} catch (Throwable e) {
			fail(e, PlaybackException.ERROR_CODE_UNSPECIFIED);
		}
	}

	private void loadCurrentUri(boolean likelyHls, boolean likelyDash) {
		if (likelyHls) {
			MPVLib.command(new String[]{"loadfile", currentPlayableUri, "replace", "-1", HLS_LOAD_OPTIONS});
		} else if (likelyDash) {
			MPVLib.command(new String[]{"loadfile", currentPlayableUri, "replace", "-1", DASH_LOAD_OPTIONS});
		} else {
			MPVLib.command(new String[]{"loadfile", currentPlayableUri, "replace"});
		}
	}

	private void startStateRefresh() {
		mainHandler.removeCallbacks(stateRefreshRunnable);
		mainHandler.postDelayed(stateRefreshRunnable, STATE_REFRESH_INTERVAL_MS);
	}

	private void stopStateRefresh() {
		mainHandler.removeCallbacks(stateRefreshRunnable);
	}

	private void refreshPlaybackState() {
		if (released || mediaItem == null || playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || playerError != null) return;
		cachedPositionMs = positionMs();
		cachedDurationMs = durationMs();
		invalidateState();
		startStateRefresh();
	}

	private void validateEarlyEndFile() {
		if (released || stopping || fileLoaded || eofReached || playerError != null || playbackState != Player.STATE_BUFFERING) return;
		if (booleanProperty("idle-active", idleActive)) {
			fail(new IOException("MPV load failed: no playback started"), PlaybackException.ERROR_CODE_IO_UNSPECIFIED);
		} else {
			startStateRefresh();
		}
	}

	private long positionMs() {
		if (initialized) cachedPositionMs = Math.max(0, doublePropertyMs("time-pos/full", doublePropertyMs("time-pos", cachedPositionMs)));
		return cachedPositionMs;
	}

	private long durationMs() {
		if (initialized) {
			long duration = doublePropertyMs("duration/full", doublePropertyMs("duration", cachedDurationMs));
			cachedDurationMs = duration > 0 ? duration : C.TIME_UNSET;
		}
		return cachedDurationMs > 0 ? cachedDurationMs : C.TIME_UNSET;
	}

	private long bufferedPositionMs(long position, long duration) {
		if (duration == C.TIME_UNSET || duration <= 0) return position;
		if (cachedCacheDurationMs > 0) return Math.min(duration, position + cachedCacheDurationMs);
		return playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED ? duration : position;
	}

	private boolean isPlayingInternal() {
		return playbackState == Player.STATE_READY && playWhenReady && !loading;
	}

	private void scheduleTrackRefresh() {
		if (released || trackRefreshScheduled) return;
		trackRefreshScheduled = true;
		mainHandler.postDelayed(trackRefreshRunnable, 80);
	}

	private void runScheduledTrackRefresh() {
		trackRefreshScheduled = false;
		if (released) return;
		refreshTracks();
		invalidateState();
	}

	private void cancelScheduledTrackRefresh() {
		trackRefreshScheduled = false;
		mainHandler.removeCallbacks(trackRefreshRunnable);
	}

	private void refreshTracks() {
		if (trackRefreshScheduled) cancelScheduledTrackRefresh();
		if (!initialized) {
			currentTracks = Tracks.EMPTY;
			return;
		}
		int count = Math.max(0, intProperty("track-list/count", 0));
		if (count <= 0) {
			currentTracks = Tracks.EMPTY;
			return;
		}
		List<TrackInfo> infos = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			TrackInfo info = readTrackInfo(i);
			if (info != null) infos.add(info);
		}
		if (infos.isEmpty()) {
			currentTracks = Tracks.EMPTY;
			return;
		}
		String selectedVideo = selectedTrackId(C.TRACK_TYPE_VIDEO);
		String selectedAudio = selectedTrackId(C.TRACK_TYPE_AUDIO);
		String selectedText = selectedTrackId(C.TRACK_TYPE_TEXT);
		boolean hasSelectedVideo = hasSelectedTrack(infos, C.TRACK_TYPE_VIDEO, selectedVideo);
		boolean hasSelectedAudio = hasSelectedTrack(infos, C.TRACK_TYPE_AUDIO, selectedAudio);
		boolean hasSelectedText = hasSelectedTrack(infos, C.TRACK_TYPE_TEXT, selectedText);
		boolean autoVideoFallbackUsed = false;
		boolean autoAudioFallbackUsed = false;
		boolean autoTextFallbackUsed = false;
		List<Tracks.Group> groups = new ArrayList<>();
		for (TrackInfo info : infos) {
			boolean selected = isTrackSelected(info, trackIdForType(info.type, selectedVideo, selectedAudio, selectedText));
			if (!selected && info.type == C.TRACK_TYPE_VIDEO && !hasSelectedVideo && isAutoOrUnknownTrackChoice(selectedVideo) && !autoVideoFallbackUsed) {
				selected = true;
				autoVideoFallbackUsed = true;
			} else if (!selected && info.type == C.TRACK_TYPE_AUDIO && !hasSelectedAudio && isAutoOrUnknownTrackChoice(selectedAudio) && !autoAudioFallbackUsed) {
				selected = true;
				autoAudioFallbackUsed = true;
			} else if (!selected && info.type == C.TRACK_TYPE_TEXT && !hasSelectedText && isAutoTrackChoice(selectedText) && !autoTextFallbackUsed) {
				selected = true;
				autoTextFallbackUsed = true;
			}
			Format format = info.toFormat();
			TrackGroup mediaGroup = new TrackGroup("mpv:" + info.type + ":" + info.id, format);
			groups.add(new Tracks.Group(mediaGroup, false, new int[]{C.FORMAT_HANDLED}, new boolean[]{selected}));
		}
		currentTracks = groups.isEmpty() ? Tracks.EMPTY : new Tracks(groups);
	}

	private TrackInfo readTrackInfo(int index) {
		String prefix = "track-list/" + index + "/";
		String mpvType = stringProperty(prefix + "type", "");
		int type = mediaTrackType(mpvType);
		if (type == C.TRACK_TYPE_UNKNOWN) return null;
		if (type == C.TRACK_TYPE_VIDEO && booleanProperty(prefix + "albumart", false)) return null;
		String id = stringProperty(prefix + "id", "");
		if (TextUtils.isEmpty(id)) id = String.valueOf(intProperty(prefix + "id", index + 1));
		String title = stringProperty(prefix + "title", "");
		String lang = stringProperty(prefix + "lang", "");
		String codec = stringProperty(prefix + "codec", "");
		boolean selected = booleanProperty(prefix + "selected", false);
		int width = intProperty(prefix + "demux-w", C.LENGTH_UNSET);
		int height = intProperty(prefix + "demux-h", C.LENGTH_UNSET);
		float frameRate = type == C.TRACK_TYPE_VIDEO ? videoFrameRate() : C.RATE_UNSET;
		if (type == C.TRACK_TYPE_VIDEO) {
			if (width <= 0) width = videoSize.width > 0 ? videoSize.width : intProperty("width", C.LENGTH_UNSET);
			if (height <= 0) height = videoSize.height > 0 ? videoSize.height : intProperty("height", C.LENGTH_UNSET);
		}
		int sampleRate = intProperty(prefix + "demux-samplerate", C.RATE_UNSET_INT);
		int channels = intProperty(prefix + "demux-channel-count", C.LENGTH_UNSET);
		int bitrate = intProperty(prefix + "demux-bitrate", C.LENGTH_UNSET);
		if (bitrate <= 0 && type == C.TRACK_TYPE_VIDEO) bitrate = intProperty("video-bitrate", C.LENGTH_UNSET);
		if (bitrate <= 0 && type == C.TRACK_TYPE_AUDIO) bitrate = intProperty("audio-bitrate", C.LENGTH_UNSET);
		return new TrackInfo(type, id, title, lang, codec, selected, width, height, frameRate, sampleRate, channels, bitrate);
	}

	private float videoFrameRate() {
		double fps = doubleProperty("container-fps", 0);
		if (fps <= 0) fps = doubleProperty("estimated-vf-fps", 0);
		return fps > 0 ? (float) fps : C.RATE_UNSET;
	}

	private int mediaTrackType(String mpvType) {
		if ("video".equals(mpvType)) return C.TRACK_TYPE_VIDEO;
		if ("audio".equals(mpvType)) return C.TRACK_TYPE_AUDIO;
		if ("sub".equals(mpvType)) return C.TRACK_TYPE_TEXT;
		return C.TRACK_TYPE_UNKNOWN;
	}

	private void updateVideoSize(String reason) {
		int width = intProperty("current-tracks/video/demux-w", 0);
		int height = intProperty("current-tracks/video/demux-h", 0);
		if (width <= 0) width = intProperty("video-params/dw", intProperty("video-params/w", intProperty("width", 0)));
		if (height <= 0) height = intProperty("video-params/dh", intProperty("video-params/h", intProperty("height", 0)));
		if (width <= 0 || height <= 0) return;
		if (videoSize.width == width && videoSize.height == height) return;
		videoSize = new VideoSize(width, height);
	}

	private boolean hasSelectedTrack(List<TrackInfo> infos, int type, String selectedId) {
		for (TrackInfo info : infos) if (info.type == type && isTrackSelected(info, selectedId)) return true;
		return false;
	}

	private boolean isTrackSelected(TrackInfo info, String selectedId) {
		if (TextUtils.isEmpty(selectedId)) return info.selected;
		if (selectedId.equals(info.id)) return true;
		try {
			return Integer.parseInt(selectedId) == Integer.parseInt(info.id);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private String selectedTrackId(int type) {
		String property = type == C.TRACK_TYPE_VIDEO ? "vid" : type == C.TRACK_TYPE_AUDIO ? "aid" : "sid";
		String value = stringProperty(property, "");
		if (TextUtils.isEmpty(value)) {
			Integer num = intPropertyNullable(property);
			value = num == null ? "auto" : String.valueOf(num);
		}
		return value;
	}

	private String trackIdForType(int type, String selectedVideo, String selectedAudio, String selectedText) {
		if (type == C.TRACK_TYPE_VIDEO) return selectedVideo;
		if (type == C.TRACK_TYPE_AUDIO) return selectedAudio;
		if (type == C.TRACK_TYPE_TEXT) return selectedText;
		return "";
	}

	private boolean isAutoTrackChoice(String value) {
		return "auto".equals(value) || TextUtils.isEmpty(value);
	}

	private boolean isAutoOrUnknownTrackChoice(String value) {
		return isAutoTrackChoice(value) || "unknown".equalsIgnoreCase(value);
	}

	/** 供上层切换音视频/字幕轨。id 为 mpv track-list 中的 id（如 "1"、"2"），"auto"/"no" 为特殊值 */
	public void setTrackSelection(int type, String mpvId) {
		if (TextUtils.isEmpty(mpvId)) return;
		setMpvTrack(type, mpvId);
		refreshTracks();
		invalidateState();
	}

	private void setMpvTrack(int type, String mpvId) {
		if (!initialized) return;
		String property = type == C.TRACK_TYPE_VIDEO ? "vid" : type == C.TRACK_TYPE_AUDIO ? "aid" : "sid";
		if ("auto".equals(mpvId) || "no".equals(mpvId)) {
			safeSetPropertyString(property, mpvId);
		} else {
			safeSetPropertyString(property, mpvId);
		}
	}

	/** 视频实际渲染宽高 */
	public int getVideoWidth() {
		return videoSize.width;
	}

	/** 视频实际渲染高度 */
	public int getVideoHeight() {
		return videoSize.height;
	}

	private void markFailureSignal(String line) {
		String lower = line.toLowerCase(Locale.US);
		if (lower.contains("failed to resolve") || lower.contains("error resolving") || lower.contains("connection reset") || lower.contains("connection refused") || lower.contains("couldn't connect") || lower.contains("network is unreachable") || lower.contains("timeout")) {
			sawNetworkError = true;
		} else if (lower.contains("could not open codec") || lower.contains("failed to initialize decoder") || lower.contains("no audio or video data played")) {
			sawNoAvData = true;
			sawDecodeError = true;
		} else if (lower.contains("invalid data found when processing input") || lower.contains("video: png")) {
			sawInvalidData = true;
		} else if (lower.contains("drm") || lower.contains("encrypted media")) {
			sawDrmError = true;
		}
	}

	private String nativeEndFileErrorCode(int error) {
		if (sawDrmError) return "MPV_DRM_UNSUPPORTED";
		if (sawNetworkError) return "MPV_NETWORK_FAILED";
		if (sawNoAvData || error == MPVLib.MpvError.MPV_ERROR_NOTHING_TO_PLAY) return "MPV_NO_AV_DATA";
		if (sawInvalidData || error == MPVLib.MpvError.MPV_ERROR_UNKNOWN_FORMAT || error == MPVLib.MpvError.MPV_ERROR_UNSUPPORTED) return "MPV_INVALID_MEDIA_DATA";
		if (error == MPVLib.MpvError.MPV_ERROR_LOADING_FAILED) return "MPV_LOAD_FAILED";
		return "MPV_DECODE_FAILED";
	}

	private int nativeEndFilePlaybackExceptionCode(int error) {
		if (error == MPVLib.MpvError.MPV_ERROR_LOADING_FAILED) return PlaybackException.ERROR_CODE_IO_UNSPECIFIED;
		if (error == MPVLib.MpvError.MPV_ERROR_VO_INIT_FAILED) return PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED;
		return PlaybackException.ERROR_CODE_DECODING_FAILED;
	}

	private String nativeEndFileDetail(int reason, int error, @Nullable String errorText) {
		StringBuilder builder = new StringBuilder();
		builder.append("native end-file reason=").append(reason).append(" error=").append(error);
		if (!TextUtils.isEmpty(errorText)) builder.append(' ').append(errorText);
		return builder.toString();
	}

	private IOException mpvError(String code, @Nullable String detail) {
		return new IOException(TextUtils.isEmpty(detail) ? code : code + ": " + detail);
	}

	private void fail(Throwable e, int errorCode) {
		playerError = new PlaybackException(e.getMessage(), e, errorCode);
		playbackState = Player.STATE_IDLE;
		loading = false;
		fileLoaded = false;
		closeContentFds();
		mainHandler.removeCallbacks(endFileValidationRunnable);
		stopStateRefresh();
		invalidateState();
	}

	private void postToMain(Runnable runnable) {
		if (Looper.myLooper() == Looper.getMainLooper()) runnable.run();
		else mainHandler.post(runnable);
	}

	private void setOption(String name, String value) {
		if (value == null) value = "";
		try {
			MPVLib.setOptionString(name, value);
		} catch (Throwable ignored) {
		}
	}

	private void setRuntimeString(String name, String value) {
		if (value == null) value = "";
		if (initialized) {
			try {
				MPVLib.setPropertyString(name, value);
				return;
			} catch (Throwable ignored) {
			}
		}
		setOption(name, value);
	}

	private void observe(String property, int format) {
		try {
			MPVLib.observeProperty(property, format);
		} catch (Throwable ignored) {
		}
	}

	private void safeSetPropertyBoolean(String property, boolean value) {
		try {
			MPVLib.setPropertyBoolean(property, value);
		} catch (Throwable ignored) {
		}
	}

	private void safeSetPropertyDouble(String property, double value) {
		try {
			MPVLib.setPropertyDouble(property, value);
		} catch (Throwable ignored) {
		}
	}

	private void safeSetPropertyInt(String property, int value) {
		try {
			MPVLib.setPropertyInt(property, value);
		} catch (Throwable ignored) {
		}
	}

	private void safeSetPropertyString(String property, String value) {
		try {
			MPVLib.setPropertyString(property, value);
		} catch (Throwable ignored) {
		}
	}

	private void closeContentFds() {
		if (contentFds.isEmpty()) return;
		for (ParcelFileDescriptor fd : contentFds) {
			try {
				fd.close();
			} catch (IOException ignored) {
			}
		}
		contentFds.clear();
	}

	private long doublePropertyMs(String property, long fallback) {
		try {
			Double value = MPVLib.getPropertyDouble(property);
			if (value == null || value.isNaN() || value.isInfinite()) return fallback;
			return Math.max(0, Math.round(value * SECONDS_TO_MS));
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private double doubleProperty(String property, double fallback) {
		try {
			Double value = MPVLib.getPropertyDouble(property);
			if (value == null || value.isNaN() || value.isInfinite()) return fallback;
			return value;
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private long doubleSecondsToMs(@Nullable Object value, long fallback) {
		if (value instanceof Number) return Math.max(0, Math.round(((Number) value).doubleValue() * SECONDS_TO_MS));
		return fallback;
	}

	private int intProperty(String property, int fallback) {
		try {
			Integer value = MPVLib.getPropertyInt(property);
			return value == null ? fallback : value;
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	@Nullable
	private Integer intPropertyNullable(String property) {
		try {
			return MPVLib.getPropertyInt(property);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private boolean booleanProperty(String property, boolean fallback) {
		try {
			Boolean value = MPVLib.getPropertyBoolean(property);
			return value == null ? fallback : value;
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private String stringProperty(String property, String fallback) {
		try {
			String value = MPVLib.getPropertyString(property);
			return value == null ? fallback : value;
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private final class TrackInfo {
		final int type;
		final String id;
		final String title;
		final String lang;
		final String codec;
		final boolean selected;
		final int width;
		final int height;
		final float frameRate;
		final int sampleRate;
		final int channels;
		final int bitrate;

		TrackInfo(int type, String id, String title, String lang, String codec, boolean selected, int width, int height, float frameRate, int sampleRate, int channels, int bitrate) {
			this.type = type;
			this.id = id;
			this.title = title;
			this.lang = lang;
			this.codec = codec;
			this.selected = selected;
			this.width = width;
			this.height = height;
			this.frameRate = frameRate;
			this.sampleRate = sampleRate;
			this.channels = channels;
			this.bitrate = bitrate;
		}

		Format toFormat() {
			String label = TextUtils.isEmpty(title) ? (TextUtils.isEmpty(lang) ? trackLabel() : null) : title;
			Format.Builder builder = new Format.Builder()
					.setId(type + ":" + id)
					.setLabel(label)
					.setCodecs(TextUtils.isEmpty(codec) ? null : codec)
					.setLanguage(TextUtils.isEmpty(lang) ? null : lang)
					.setSampleMimeType(sampleMimeType(this));
			if (width > 0) builder.setWidth(width);
			if (height > 0) builder.setHeight(height);
			if (frameRate > 0) builder.setFrameRate(frameRate);
			if (sampleRate > 0) builder.setSampleRate(sampleRate);
			if (channels > 0) builder.setChannelCount(channels);
			if (bitrate > 0) builder.setAverageBitrate(bitrate);
			return builder.build();
		}

		private String trackLabel() {
			String prefix;
			if (type == C.TRACK_TYPE_VIDEO) prefix = "Video";
			else if (type == C.TRACK_TYPE_AUDIO) prefix = "Audio";
			else if (type == C.TRACK_TYPE_TEXT) prefix = "Subtitle";
			else prefix = "Track";
			return prefix + " " + id;
		}
	}

	private String sampleMimeType(TrackInfo info) {
		String codec = info.codec == null ? "" : info.codec.toLowerCase(Locale.US);
		if (info.type == C.TRACK_TYPE_TEXT) {
			if (codec.contains("pgs") || codec.contains("hdmv")) return MimeTypes.APPLICATION_PGS;
			if (codec.contains("dvd") || codec.contains("vobsub")) return MimeTypes.APPLICATION_VOBSUB;
			if (codec.contains("dvb")) return MimeTypes.APPLICATION_DVBSUBS;
			if (codec.contains("ass") || codec.contains("ssa")) return MimeTypes.TEXT_SSA;
			if (codec.contains("webvtt") || codec.contains("vtt")) return MimeTypes.TEXT_VTT;
			if (codec.contains("srt") || codec.contains("subrip")) return MimeTypes.APPLICATION_SUBRIP;
			if (codec.contains("ttml")) return MimeTypes.APPLICATION_TTML;
			return TextUtils.isEmpty(codec) ? MimeTypes.TEXT_UNKNOWN : MimeTypes.BASE_TYPE_TEXT + "/" + codec;
		}
		if (info.type == C.TRACK_TYPE_AUDIO) {
			if (codec.contains("aac")) return MimeTypes.AUDIO_AAC;
			if (codec.contains("ac3")) return MimeTypes.AUDIO_AC3;
			if (codec.contains("eac3") || codec.contains("e-ac-3")) return MimeTypes.AUDIO_E_AC3;
			if (codec.contains("opus")) return MimeTypes.AUDIO_OPUS;
			if (codec.contains("vorbis")) return MimeTypes.AUDIO_VORBIS;
			if (codec.contains("flac")) return MimeTypes.AUDIO_FLAC;
			if (codec.contains("mp3")) return MimeTypes.AUDIO_MPEG;
			return MimeTypes.BASE_TYPE_AUDIO + "/" + (TextUtils.isEmpty(codec) ? "unknown" : codec);
		}
		if (codec.contains("hevc") || codec.contains("h265")) return MimeTypes.VIDEO_H265;
		if (codec.contains("h264") || codec.contains("avc")) return MimeTypes.VIDEO_H264;
		if (codec.contains("av1")) return MimeTypes.VIDEO_AV1;
		if (codec.contains("vp9")) return MimeTypes.VIDEO_VP9;
		if (codec.contains("vp8")) return MimeTypes.VIDEO_VP8;
		if (codec.contains("mpeg2")) return MimeTypes.VIDEO_MPEG2;
		return MimeTypes.BASE_TYPE_VIDEO + "/" + (TextUtils.isEmpty(codec) ? "unknown" : codec);
	}
}
