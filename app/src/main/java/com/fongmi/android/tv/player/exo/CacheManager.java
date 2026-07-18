package com.fongmi.android.tv.player.exo;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Path;

import java.io.File;

public class CacheManager {

    private static final int CACHE_SPACE_PERCENT = 80;

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
        File dir = Path.exo();
        long usedBytes = getDirectorySize(dir);
        long availableBytes = Math.max(0, dir.getUsableSpace());
        return Math.min(512L * 1024 * 1024, (usedBytes + availableBytes) * CACHE_SPACE_PERCENT / 100);
    }

    private long getDirectorySize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isFile()) size += file.length();
            else size += getDirectorySize(file);
        }
        return size;
    }
}

