package com.fongmi.android.tv.player.extractor;

import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public class Thunder implements Source.Extractor {

    private GetTaskId taskId;

    private static final String[] DEFAULT_TRACKERS = {
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://tracker.moeking.me:6969/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://open.demonii.com:1337/announce",
        "udp://tracker.cyberia.is:6969/announce",
        "udp://tracker.dler.org:6969/announce",
        "https://tracker.nanoha.org:443/announce",
        "https://tracker.lilithraws.org:443/announce",
        "http://tracker.bt4g.com:2095/announce",
        "http://tracker.files.fm:6969/announce"
    };

    @Override
    public boolean match(String scheme, String host) {
        return "magnet".equals(scheme) || "ed2k".equals(scheme);
    }

    @Override
    public String fetch(String url) throws Exception {
        String scheme = UrlUtil.scheme(url);
        if ("magnet".equals(scheme)) {
            // Check if this is a magnet:// local torrent path (returned by addMagnetTask)
            // e.g. magnet:///data/.../xxx.torrent?name=xxx&index=0
            // These should be handled directly as torrent tasks, not as magnet links.
            if (url.startsWith("magnet://") && url.contains(".torrent")) {
                android.util.Log.e("Thunder", "detected local torrent path, calling addTorrentTask directly: " + url);
                return addTorrentTask(Uri.parse(url));
            }
            // Standard magnet:?xt=urn:btih:... link - resolve via addMagnetTask
            String torrentUrl = addMagnetTask(url);
            android.util.Log.e("Thunder", "addMagnetTask returned: " + torrentUrl);
            // addMagnetTask returns magnet:///data/...torrent?...?name=xxx&index=0
            // This is a torrent metadata path, not a playable video URL.
            // We need to further resolve it via addTorrentTask to get the actual video stream.
            if (torrentUrl != null && torrentUrl.startsWith("magnet://")) {
                android.util.Log.e("Thunder", "calling addTorrentTask with: " + torrentUrl);
                String result = addTorrentTask(Uri.parse(torrentUrl));
                android.util.Log.e("Thunder", "addTorrentTask returned: " + result);
                return result;
            }
            android.util.Log.e("Thunder", "not a magnet:// URL, returning directly");
            return torrentUrl;
        }
        if ("thunder".equals(scheme)) return addThunderTask(url);
        return addTorrentTask(Uri.parse(url));
    }

    private String addMagnetTask(String url) throws Exception {
        GetTaskId taskId = XLTaskHelper.get().parse(appendTrackers(url), Path.thunder(Util.md5(url)));
        // 等待磁力元数据 (.torrent) 下载完成，最多等待 30 秒，避免无 peer 时无限阻塞。
        // 元数据下载完成后 getTaskInfo 返回 status=2 (对应 .torrent 文件已存在)。
        int waitTime = 0;
        while (waitTime < 30000) {
            XLTaskInfo taskInfo = XLTaskHelper.get().getTaskInfo(taskId);
            if (taskInfo.getTaskStatus() == 2) break;
            if (taskInfo.getTaskStatus() == 3) throw new ExtractException(taskInfo.getErrorMsg());
            SystemClock.sleep(300);
            waitTime += 300;
        }
        List<TorrentFileInfo> medias = XLTaskHelper.get().getTorrentInfo(taskId.getSaveFile()).getMedias();
        if (medias.isEmpty()) throw new ExtractException("no media found");
        // 停止磁力元数据任务，避免与后续 BT 任务重复下载同一文件。
        // .torrent 文件已落盘保留，addTorrentTask 将基于它创建 BT 任务。
        XLTaskHelper.get().stopTask(taskId);
        return medias.get(0).getPlayUrl();
    }

    private String appendTrackers(String url) {
        String trackers = Setting.getTrackerList();
        if (TextUtils.isEmpty(trackers)) return url;
        String[] lines = trackers.split("\n");
        StringBuilder sb = new StringBuilder(url);
        for (String line : lines) {
            String tracker = line.trim();
            if (!TextUtils.isEmpty(tracker) && !tracker.startsWith("#")) {
                sb.append("&tr=").append(Uri.encode(tracker));
            }
        }
        return sb.toString();
    }

    private String addTorrentTask(Uri uri) throws Exception {
        // The URI may contain unencoded Chinese characters and spaces in both path and query.
        // Android's Uri.parse() handles these gracefully for getPath(), but getQueryParameter()
        // may return null for unencoded values. We manually extract query parameters from the
        // raw URI string to handle this robustly.
        String uriStr = uri.toString();
        String path = uri.getPath();
        android.util.Log.e("Thunder", "addTorrentTask uriStr: " + uriStr);
        android.util.Log.e("Thunder", "addTorrentTask path: " + path);
        File torrent = new File(path);
        android.util.Log.e("Thunder", "addTorrentTask torrent exists: " + torrent.exists() + ", path: " + torrent.getAbsolutePath());
        // Extract query parameters manually from the raw URI string to handle unencoded values
        String name = null;
        int index = 0;
        int qIdx = uriStr.indexOf('?');
        if (qIdx >= 0) {
            String query = uriStr.substring(qIdx + 1);
            for (String pair : query.split("&")) {
                int eqIdx = pair.indexOf('=');
                if (eqIdx < 0) continue;
                String key = pair.substring(0, eqIdx);
                String value = pair.substring(eqIdx + 1);
                if ("name".equals(key)) name = value;
                else if ("index".equals(key)) try { index = Integer.parseInt(value); } catch (NumberFormatException ignored) { }
            }
        }
        android.util.Log.e("Thunder", "addTorrentTask name: " + name + ", index: " + index);
        // If name is null (e.g., due to unencoded special chars in URL), try to get it from torrent info
        if (name == null || name.trim().isEmpty()) {
            android.util.Log.e("Thunder", "addTorrentTask name is null/empty, trying to get from torrent info");
            try {
                TorrentInfo torrentInfo = XLTaskHelper.get().getTorrentInfo(torrent);
                if (torrentInfo != null && torrentInfo.mSubFileInfo != null && index < torrentInfo.mSubFileInfo.length) {
                    name = torrentInfo.mSubFileInfo[index].getFileName();
                    android.util.Log.e("Thunder", "addTorrentTask got name from torrent info: " + name);
                }
            } catch (Exception e) {
                android.util.Log.e("Thunder", "addTorrentTask getTorrentInfo failed", e);
            }
        }
        if (name == null || name.trim().isEmpty()) name = "video_" + index;
        android.util.Log.e("Thunder", "addTorrentTask final name: " + name + ", calling addTorrentTask with torrent: " + torrent.getAbsolutePath());
        taskId = XLTaskHelper.get().addTorrentTask(torrent, Objects.requireNonNull(torrent.getParentFile()), index);
        android.util.Log.e("Thunder", "addTorrentTask taskId: " + (taskId != null ? taskId.getTaskId() : "null"));
        // 边下边播：等待子任务开始下载 (status != 0) 且本地 HTTP 代理就绪，最多等待 20 秒。
        // 迅雷 SDK 的 getLocalUrl 返回本地 HTTP 代理地址 (http://127.0.0.1:PORT/...)，
        // 播放器通过该地址发起 Range 请求 (206) 实现边下边播。
        File target = new File(torrent.getParent(), name);
        int waitTime = 0;
        while (waitTime < 20000) {
            BtSubTaskDetail subTaskDetail = XLTaskHelper.get().getBtSubTaskInfo(taskId, index);
            if (subTaskDetail == null || subTaskDetail.mTaskInfo == null) {
                android.util.Log.e("Thunder", "addTorrentTask subTaskDetail or mTaskInfo is null, waiting...");
                SystemClock.sleep(300);
                waitTime += 300;
                continue;
            }
            XLTaskInfo taskInfo = subTaskDetail.mTaskInfo;
            android.util.Log.e("Thunder", "addTorrentTask taskStatus: " + taskInfo.mTaskStatus + ", waitTime: " + waitTime);
            if (taskInfo.mTaskStatus == 3) throw new ExtractException(taskInfo.getErrorMsg());
            if (taskInfo.mTaskStatus != 0) {
                // 子任务已开始下载，确认本地代理就绪后返回
                String localUrl = XLTaskHelper.get().getLocalUrl(target);
                if (localUrl != null && !localUrl.isEmpty()) {
                    android.util.Log.e("Thunder", "addTorrentTask returning localUrl: " + localUrl);
                    return localUrl;
                }
                android.util.Log.w("Thunder", "addTorrentTask localUrl empty, keep waiting...");
            }
            SystemClock.sleep(300);
            waitTime += 300;
        }
        // Timeout reached, return local URL anyway (supports streaming)
        android.util.Log.w("Thunder", "addTorrentTask timeout, returning local URL anyway");
        String localUrl = XLTaskHelper.get().getLocalUrl(target);
        android.util.Log.e("Thunder", "addTorrentTask timeout returning localUrl: " + localUrl);
        return localUrl;
    }

    private String addThunderTask(String url) {
        File folder = Path.thunder(Util.md5(url));
        taskId = XLTaskHelper.get().addThunderTask(url, folder);
        return XLTaskHelper.get().getLocalUrl(taskId.getSaveFile());
    }

    @Override
    public void stop() {
        if (taskId == null) return;
        XLTaskHelper.get().deleteTask(taskId);
        taskId = null;
    }

    @Override
    public void exit() {
        XLTaskHelper.get().release();
    }

    public static class Parser implements Callable<List<Episode>> {

        private static final Pattern THUNDER = Pattern.compile("(magnet|thunder|ed2k):.*");
        private final String url;
        private int time;

        public static boolean match(String url) {
            return THUNDER.matcher(url).find() || isTorrent(url);
        }

        public static Parser get(String url) {
            return new Parser(url);
        }

        public Parser(String url) {
            this.url = url;
        }

        private void sleep() {
            SystemClock.sleep(10);
            time += 10;
        }

        private static boolean isTorrent(String url) {
            return !url.startsWith("magnet") && url.split(";")[0].endsWith(".torrent");
        }

        @Override
        public List<Episode> call() {
            boolean torrent = isTorrent(url);
            List<Episode> episodes = new ArrayList<>();
            String magnetUrl = url.startsWith("magnet") ? appendTrackers(url) : url;
            GetTaskId taskId = XLTaskHelper.get().parse(magnetUrl, Path.thunder(Util.md5(url)));
            if (!torrent && !taskId.getRealUrl().startsWith("magnet")) {
                Episode episode = Episode.create(taskId.getFileName(), taskId.getRealUrl());
                // 保存原始磁力链接 (magnet:?xt=urn:btih:...)，用于长按复制
                if (url.startsWith("magnet:")) episode.setOriginalUrl(url);
                return Arrays.asList(episode);
            }
            if (torrent) Download.create(url, taskId.getSaveFile()).start();
            else while (XLTaskHelper.get().getTaskInfo(taskId).getTaskStatus() != 2 && time < 5000) sleep();
            List<TorrentFileInfo> medias = XLTaskHelper.get().getTorrentInfo(taskId.getSaveFile()).getMedias();
            for (TorrentFileInfo media : medias) {
                Episode episode = Episode.create(media.getFileName(), media.getSize(), media.getPlayUrl());
                // 保存原始磁力链接 (magnet:?xt=urn:btih:...)，用于长按复制
                if (url.startsWith("magnet:")) episode.setOriginalUrl(url);
                episodes.add(episode);
            }
            XLTaskHelper.get().stopTask(taskId);
            return episodes;
        }

        private static String appendTrackers(String url) {
            String trackers = Setting.getTrackerList();
            if (TextUtils.isEmpty(trackers)) return url;
            String[] lines = trackers.split("\n");
            StringBuilder sb = new StringBuilder(url);
            for (String line : lines) {
                String tracker = line.trim();
                if (!TextUtils.isEmpty(tracker) && !tracker.startsWith("#")) {
                    sb.append("&tr=").append(Uri.encode(tracker));
                }
            }
            return sb.toString();
        }
    }
}
