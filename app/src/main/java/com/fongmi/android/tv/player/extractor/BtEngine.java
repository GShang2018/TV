package com.fongmi.android.tv.player.extractor;

import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.Setting;
import com.fongmi.btengine.LibtorrentEngine;
import com.fongmi.btengine.BtExtractor;
import com.fongmi.android.tv.player.Source;

import java.io.IOException;

/**
 * Open-source BT engine extractor using libtorrent4j.
 * <p>
 * This extractor handles magnet links using the libtorrent4j BT engine,
 * which runs in-process (no external process needed) and supports
 * all Android architectures including x86.
 * <p>
 * When enabled, libtorrent4j session is started and magnet links
 * are resolved directly via its native API with DHT/PEX support.
 */
public class BtEngine implements Source.Extractor {

    private static final String TAG = BtEngine.class.getSimpleName();

    private BtExtractor extractor;

    @Override
    public boolean match(String scheme, String host) {
        return "magnet".equals(scheme) && isEnabled();
    }

    @Override
    public String fetch(String url) throws Exception {
        ensureRunning();
        if (extractor == null) {
            extractor = new BtExtractor();
        }
        return extractor.fetch(url);
    }

    @Override
    public void stop() {
        if (extractor != null) {
            extractor.stop();
        }
    }

    @Override
    public void exit() {
        if (extractor != null) {
            extractor.exit();
            extractor = null;
        }
        LibtorrentEngine.get().stop();
    }

    /**
     * Check if the BT engine is enabled in settings.
     */
    public static boolean isEnabled() {
        return Setting.isBtEngineEnabled();
    }

    /**
     * Ensure the libtorrent4j session is running.
     */
    public static void ensureRunning() {
        if (!LibtorrentEngine.get().isRunning()) {
            LibtorrentEngine.get().start();
        }
    }

    /**
     * Restart the BT engine.
     */
    public static void restartWithTrackers() {
        LibtorrentEngine.get().start();
    }

    /**
     * Shutdown the BT engine from settings UI.
     */
    public static void shutdown() {
        LibtorrentEngine.get().stop();
    }

    /**
     * Parser for batch magnet link resolution.
     * Used in Source.parse() for episode list parsing.
     */
    public static class Parser {

        public static boolean match(String url) {
            return BtEngine.isEnabled() && url != null && url.startsWith("magnet:");
        }

        public static java.util.List<com.fongmi.android.tv.bean.Episode> parse(String url) {
            java.util.List<com.fongmi.android.tv.bean.Episode> episodes = new java.util.ArrayList<>();
            try {
                ensureRunning();
                BtExtractor be = new BtExtractor();
                String fileUrl = be.fetch(url);
                if (fileUrl != null) {
                    android.net.Uri uri = android.net.Uri.parse(fileUrl);
                    if (uri.getPath() != null) {
                        java.io.File file = new java.io.File(uri.getPath());
                        com.fongmi.android.tv.bean.Episode episode = com.fongmi.android.tv.bean.Episode.create(file.getName(), fileUrl);
                        // 保存原始磁力链接 (magnet:?xt=urn:btih:...)，用于长按复制
                        if (url.startsWith("magnet:")) episode.setOriginalUrl(url);
                        episodes.add(episode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing magnet: " + url, e);
            }
            return episodes;
        }
    }
}
