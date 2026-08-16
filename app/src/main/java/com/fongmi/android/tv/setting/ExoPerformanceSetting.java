package com.fongmi.android.tv.setting;

import com.fongmi.android.tv.Setting;
import com.github.catvod.utils.Prefers;

/**
 * ExoPlayer 缓冲细调设置（移植自同源项目 webtv）。
 * 独立控制"持续缓冲档位"（minBufferMs/maxBufferMs 乘数）、
 * "起播阈值"（bufferForPlaybackMs）与"重缓冲恢复"（bufferForPlaybackAfterRebufferMs），
 * 三个参数均支持 AUTO 自适应（基于 AutoRebufferPolicy 的会话基线）。
 * 点击交互：AUTO → 手动（保留上次手动值）→ 各档位循环 → 末档后再点回到 AUTO。
 */
public final class ExoPerformanceSetting {

    private static final String KEY_AUTO_BUFFER = "exo_auto_buffer";
    private static final String KEY_AUTO_START_BUFFER = "exo_auto_start_buffer";
    private static final String KEY_AUTO_REBUFFER = "exo_auto_rebuffer";
    private static final String KEY_START_BUFFER_MS = "exo_start_buffer_ms";
    private static final String KEY_REBUFFER_MS = "exo_rebuffer_ms";
    private static final String KEY_AUTO_REBUFFER_MS = "exo_auto_rebuffer_ms";
    private static final String KEY_AUTO_CLEAN_STREAK = "exo_auto_clean_streak";

    private static volatile int autoSessionRebufferMs = AutoRebufferPolicy.DEFAULT_REBUFFER_MS;
    /** 磁力/本地代理播放模式：降低重缓冲恢复阈值，避免网速恢复后仍卡在缓冲态 */
    private static volatile boolean sProxyMode;

    private ExoPerformanceSetting() {
    }

    public static void setProxyMode(boolean enabled) {
        sProxyMode = enabled;
    }

    public static boolean isProxyMode() {
        return sProxyMode;
    }

    // ---------- 持续缓冲档位（minBufferMs/maxBufferMs 乘数，1-10） ----------

    public static boolean isAutoBuffer() {
        return Prefers.getBoolean(KEY_AUTO_BUFFER, true);
    }

    public static void putAutoBuffer(boolean enabled) {
        Prefers.put(KEY_AUTO_BUFFER, enabled);
    }

    public static String getBufferText() {
        if (isAutoBuffer()) return "自动";
        return Setting.getBuffer() + "/10";
    }

    public static void nextBuffer() {
        if (isAutoBuffer()) {
            putAutoBuffer(false);
        } else if (Setting.getBuffer() >= 10) {
            putAutoBuffer(true);
        } else {
            Setting.putBuffer(Setting.getBuffer() + 1);
        }
    }

    /** 实际生效的缓冲档位（AUTO 时由会话基线映射，手动时为用户设定值） */
    public static int getEffectiveBuffer() {
        if (isAutoBuffer()) return getAutoSessionBuffer();
        return Setting.getBuffer();
    }

    // ---------- 起播阈值（bufferForPlaybackMs） ----------

    public static boolean isAutoStartBuffer() {
        return Prefers.getBoolean(KEY_AUTO_START_BUFFER, true);
    }

    public static void putAutoStartBuffer(boolean enabled) {
        Prefers.put(KEY_AUTO_START_BUFFER, enabled);
    }

    public static int getStartBufferMs() {
        return normalizeStart(Prefers.getInt(KEY_START_BUFFER_MS, AutoRebufferPolicy.DEFAULT_START_BUFFER_MS));
    }

    public static void putStartBufferMs(int value) {
        Prefers.put(KEY_START_BUFFER_MS, normalizeStart(value));
    }

    public static String getStartBufferText() {
        if (isAutoStartBuffer()) return "自动";
        return (getStartBufferMs() / 1000) + "s";
    }

    public static void nextStartBuffer() {
        if (isAutoStartBuffer()) {
            putAutoStartBuffer(false);
        } else if (getStartBufferMs() >= 3_000) {
            putAutoStartBuffer(true);
        } else {
            putStartBufferMs(nextStartBufferMs());
        }
    }

    /** 实际生效的起播阈值（AUTO 时跟随会话基线，手动时为用户设定值） */
    public static int getEffectiveStartBufferMs() {
        int ms = isAutoStartBuffer() ? getAutoSessionStartBufferMs() : getStartBufferMs();
        return sProxyMode ? Math.min(ms, 1_000) : ms;
    }

    public static int nextStartBufferMs() {
        switch (getStartBufferMs()) {
            case 500: return 1_000;
            case 1_000: return 1_500;
            case 1_500: return 2_000;
            case 2_000: return 3_000;
            default: return 500;
        }
    }

    // ---------- 重缓冲恢复（bufferForPlaybackAfterRebufferMs） ----------

    public static boolean isAutoRebuffer() {
        return Prefers.getBoolean(KEY_AUTO_REBUFFER, true);
    }

    public static void putAutoRebuffer(boolean enabled) {
        Prefers.put(KEY_AUTO_REBUFFER, enabled);
    }

    public static int getRebufferMs() {
        if (isAutoRebuffer()) return AutoRebufferPolicy.normalize(autoSessionRebufferMs);
        return normalizeRebuffer(Prefers.getInt(KEY_REBUFFER_MS, AutoRebufferPolicy.DEFAULT_REBUFFER_MS));
    }

    /** 代理模式（磁力链接）下，重缓冲恢复阈值封顶 1000ms，网速恢复即可尽快恢复播放 */
    public static int getEffectiveRebufferMs() {
        int ms = getRebufferMs();
        return sProxyMode ? Math.min(ms, 1_000) : ms;
    }

    public static void putRebufferMs(int value) {
        Prefers.put(KEY_REBUFFER_MS, normalizeRebuffer(value));
    }

    public static String getRebufferText() {
        if (isAutoRebuffer()) return "自动";
        return (getRebufferMs() / 1000) + "s";
    }

    public static void nextRebuffer() {
        if (isAutoRebuffer()) {
            putAutoRebuffer(false);
        } else if (getRebufferMs() >= 15_000) {
            putAutoRebuffer(true);
        } else {
            putRebufferMs(nextRebufferMs());
        }
    }

    public static int nextRebufferMs() {
        switch (getRebufferMs()) {
            case 1_000: return 2_000;
            case 2_000: return 3_000;
            case 3_000: return 5_000;
            case 5_000: return 8_000;
            case 8_000: return 10_000;
            case 10_000: return 15_000;
            default: return 1_000;
        }
    }

    // ---------- AUTO 会话统计（一个策略决策驱动三个水位） ----------

    public static void recordAutoSession(int rebufferCount, long rebufferTotalMs, long positionMs, long mediaBitrate, long bandwidthEstimate) {
        if (!isAutoRebuffer() && !isAutoBuffer() && !isAutoStartBuffer()) return;
        AutoRebufferPolicy.Result result = AutoRebufferPolicy.resolve(getAutoRebufferMs(), Prefers.getInt(KEY_AUTO_CLEAN_STREAK), rebufferCount, rebufferTotalMs, positionMs, mediaBitrate, bandwidthEstimate);
        Prefers.put(KEY_AUTO_REBUFFER_MS, result.rebufferMs());
        Prefers.put(KEY_AUTO_CLEAN_STREAK, result.cleanStreak());
    }

    public static void beginAutoSession() {
        autoSessionRebufferMs = getAutoRebufferMs();
    }

    public static int getAutoSessionRebufferMs() {
        return AutoRebufferPolicy.normalize(autoSessionRebufferMs);
    }

    public static int getAutoSessionStartBufferMs() {
        return AutoRebufferPolicy.startBufferMs(autoSessionRebufferMs);
    }

    /** 会话基线对应的缓冲档位：重缓冲越高则持续缓冲水位越高 */
    public static int getAutoSessionBuffer() {
        switch (AutoRebufferPolicy.normalize(autoSessionRebufferMs)) {
            case 8_000: return 8;
            case 5_000: return 6;
            case 2_000: return 4;
            default: return 5;
        }
    }

    static int getAutoRebufferMs() {
        return AutoRebufferPolicy.normalize(Prefers.getInt(KEY_AUTO_REBUFFER_MS, AutoRebufferPolicy.DEFAULT_REBUFFER_MS));
    }

    private static int normalizeStart(int value) {
        if (value <= 500) return 500;
        if (value <= 1_000) return 1_000;
        if (value <= 1_500) return 1_500;
        if (value <= 2_000) return 2_000;
        return 3_000;
    }

    private static int normalizeRebuffer(int value) {
        if (value <= 1_000) return 1_000;
        if (value <= 2_000) return 2_000;
        if (value <= 3_000) return 3_000;
        if (value <= 5_000) return 5_000;
        if (value <= 8_000) return 8_000;
        if (value <= 10_000) return 10_000;
        return 15_000;
    }
}
