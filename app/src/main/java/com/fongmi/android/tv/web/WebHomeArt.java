package com.fongmi.android.tv.web;

/**
 * WebHome 详情页 → 原生搜索播放的会话级海报/剧照传递。
 * nostr 详情页播放时通过 fm.search 携带 pic/wallPic，存入此处，
 * 搜索结果项自身无图时作为兜底封面，保证播放器与最近观看能带上 WebHome 详情页的图。
 */
public class WebHomeArt {

    private static volatile String pic;

    public static void set(String value) {
        pic = value;
    }

    public static void clear() {
        pic = null;
    }

    /** @return 无残留时返回空串 */
    public static String get() {
        return pic == null ? "" : pic;
    }

    /** 结果自带图优先，其次会话级 WebHome 图 */
    public static String fallback(String own) {
        return own == null || own.isEmpty() ? get() : own;
    }
}
