package com.fongmi.android.tv.web;

import android.text.TextUtils;
import android.util.Base64;

import com.fongmi.android.tv.server.Server;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Util;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把页面 resolver 返回的 data:application/dash+xml;base64 清单发布为本地 /webMpd 端点，
 * 使 ExoPlayer 等原生播放器能以 http 方式读取 MPD（其内 BaseURL 已是 /webResource 绝对地址）。
 */
public class WebMpd {

    private static final String PREFIX = "data:application/dash+xml";
    private static final Map<String, String> ITEMS = new ConcurrentHashMap<>();

    public static boolean isDashData(String url) {
        return !TextUtils.isEmpty(url) && url.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
    }

    // 把 data URI 解码为 MPD 文本并发布，返回本地 http 地址；非 data 或解码失败返回原样
    public static String publish(String url) {
        if (!isDashData(url)) return url;
        try {
            int index = url.indexOf(',');
            if (index < 0) return url;
            String payload = url.substring(index + 1);
            boolean base64 = url.substring(0, index).toLowerCase().contains("base64");
            byte[] bytes = base64 ? Base64.decode(payload, Base64.DEFAULT) : payload.getBytes(StandardCharsets.UTF_8);
            String mpd = new String(bytes, StandardCharsets.UTF_8);
            String key = Util.md5(mpd);
            ITEMS.put(key, mpd);
            SpiderDebug.log("webhome-inline", "mpd published id=%s bytes=%s", key, mpd.length());
            return Server.get().getAddress("/webMpd/" + key + ".mpd");
        } catch (Throwable e) {
            SpiderDebug.log("webhome-inline", "mpd publish failed error=%s", e.getMessage());
            return url;
        }
    }

    public static String get(String id) {
        return ITEMS.get(id);
    }
}
