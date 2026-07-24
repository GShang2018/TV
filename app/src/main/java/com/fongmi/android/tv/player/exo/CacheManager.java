package com.fongmi.android.tv.player.exo;

import android.os.StatFs;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Path;

import java.io.File;

public class CacheManager {

    private static final int CACHE_SPACE_PERCENT = 80;
    private static final long MAX_CACHE_SIZE = 1024L * 1024 * 1024; // 1GB 硬上限
    private static final long MIN_CACHE_SIZE = 256L * 1024 * 1024;  // 256MB 硬下限

    private SimpleCache cache;

    private static class Loader {
        static volatile CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager get() {
        return Loader.INSTANCE;
    }

    public Cache getCache() {
        if (cache == null) create();
        return cache;
    }

    private void create() {
        cache = new SimpleCache(Path.exo(), new LeastRecentlyUsedCacheEvictor(getMaxCacheSize()), new StandaloneDatabaseProvider(App.get()));
    }

    private long getMaxCacheSize() {
        // 使用 StatFs 获取分区总空间，更准确地计算可用容量
        File dir = Path.exo();
        long totalSpace = getTotalSpace(dir);
        long cacheSize = Math.max(MIN_CACHE_SIZE, totalSpace * CACHE_SPACE_PERCENT / 100);
        return Math.min(MAX_CACHE_SIZE, cacheSize);
    }

    private long getTotalSpace(File dir) {
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            return stat.getBlockCountLong() * stat.getBlockSizeLong();
        } catch (Exception e) {
            // 降级方案：使用 getTotalSpace()
            return dir.getTotalSpace();
        }
    }
}

