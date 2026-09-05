package com.fongmi.android.tv.web;

import java.util.HashMap;
import java.util.Map;

/**
 * WebHome 直链播放的会话级请求头传递（单次有效）。
 * nostr 页面播放带 UA/Referer 的网盘直链时经 player.playUrl/pan.play 携带 headers，
 * 由播放器消费一次后清空，避免串到后续其它播放。
 */
public class WebHomeHeaders {

    private static volatile Map<String, String> headers;

    public static void set(Map<String, String> value) {
        headers = value == null || value.isEmpty() ? null : new HashMap<>(value);
    }

    public static void clear() {
        headers = null;
    }

    /** 取走一次并清空；无残留时返回 null */
    public static Map<String, String> take() {
        Map<String, String> value = headers;
        headers = null;
        return value;
    }
}
