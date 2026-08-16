package com.fongmi.android.tv.player.exo;

import android.os.SystemClock;

import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 代理模式（磁力链接）下的动态 LoadControl：
 * 通过周期性采样 ExoPlayer 的缓冲水位变化，计算"内容缓冲增长率"（每秒实际时间内缓冲了多少毫秒内容），
 * 在 shouldStartPlayback(rebuffering=true) 时根据增长率动态调整恢复阈值：
 * - 增长率 ≥ 2x（网速远超播放速度）：500ms，尽快恢复播放
 * - 增长率 1~2x（网速正常）：1000ms
 * - 增长率 < 1x（网速慢）：3000ms，多缓冲避免再次卡顿
 */
@UnstableApi
public class ProxyLoadControl implements LoadControl {

    private final DefaultLoadControl delegate;

    /** 滑动窗口采样，记录每次采样的"缓冲增长率"（ms content / s real time） */
    private final Deque<Sample> samples = new ArrayDeque<>();
    private static final int MAX_SAMPLES = 5;
    private static final long SAMPLE_EXPIRE_MS = 6_000;

    private static final class Sample {
        final long timestampMs;
        final long growthRate;
        Sample(long timestampMs, long growthRate) {
            this.timestampMs = timestampMs;
            this.growthRate = growthRate;
        }
    }

    public ProxyLoadControl(DefaultLoadControl delegate) {
        this.delegate = delegate;
    }

    /**
     * 由 Players 中的定时器调用：提交一次缓冲水位采样。
     *
     * @param bufferedDeltaMs  本次采样间隔内，缓冲水位（bufferedPosition - currentPosition）的变化量（ms）
     * @param elapsedMs        本次采样间隔的实际时间（ms）
     */
    public void onBufferSample(long bufferedDeltaMs, long elapsedMs) {
        if (elapsedMs <= 0) return;
        long growthRate = bufferedDeltaMs * 1000L / elapsedMs;
        long now = SystemClock.elapsedRealtime();
        synchronized (samples) {
            samples.addLast(new Sample(now, growthRate));
            pruneLocked(now);
        }
    }

    private void pruneLocked(long now) {
        while (!samples.isEmpty() && now - samples.peekFirst().timestampMs > SAMPLE_EXPIRE_MS) {
            samples.pollFirst();
        }
        while (samples.size() > MAX_SAMPLES) {
            samples.pollFirst();
        }
    }

    /** 滑动窗口平均缓冲增长率 */
    private long getAverageGrowthRate() {
        long now = SystemClock.elapsedRealtime();
        synchronized (samples) {
            pruneLocked(now);
            if (samples.isEmpty()) return -1;
            long sum = 0;
            for (Sample s : samples) sum += s.growthRate;
            return sum / samples.size();
        }
    }

    /** 根据缓冲增长率动态计算 rebuffer 恢复阈值（ms） */
    private int getDynamicRebufferMs() {
        long rate = getAverageGrowthRate();
        if (rate < 0) return 1_000; // 无数据，用 1000ms
        if (rate >= 2_000) return 500;   // 网速 ≥ 2x：尽快恢复
        if (rate >= 1_000) return 1_000; // 网速 1~2x：正常
        return 3_000;                    // 网速 < 1x：多缓冲
    }

    @Override
    public boolean shouldStartPlayback(
            Timeline timeline,
            MediaPeriodId mediaPeriodId,
            long bufferedDurationUs,
            float playbackSpeed,
            boolean rebuffering,
            long targetLiveOffsetUs) {
        if (rebuffering) {
            long thresholdUs = getDynamicRebufferMs() * 1000L;
            return bufferedDurationUs >= thresholdUs;
        }
        return delegate.shouldStartPlayback(timeline, mediaPeriodId, bufferedDurationUs, playbackSpeed, rebuffering, targetLiveOffsetUs);
    }

    // ---------- 以下全部委托给 delegate ----------

    @Override
    public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
        return delegate.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed);
    }

    @Override
    public void onPrepared() {
        delegate.onPrepared();
    }

    @Override
    public void onTracksSelected(
            Timeline timeline,
            MediaPeriodId mediaPeriodId,
            Renderer[] renderers,
            TrackGroupArray trackGroups,
            ExoTrackSelection[] trackSelections) {
        delegate.onTracksSelected(timeline, mediaPeriodId, renderers, trackGroups, trackSelections);
    }

    @Override
    public void onStopped() {
        delegate.onStopped();
    }

    @Override
    public void onReleased() {
        delegate.onReleased();
    }

    @Override
    public Allocator getAllocator() {
        return delegate.getAllocator();
    }

    @Override
    public long getBackBufferDurationUs() {
        return delegate.getBackBufferDurationUs();
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return delegate.retainBackBufferFromKeyframe();
    }
}
