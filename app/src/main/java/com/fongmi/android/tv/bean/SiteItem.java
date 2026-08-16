package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.JsonElement;

/**
 * 通用站点/线路条目，用于站点列表页面展示。
 * 可表示 VOD 站点、Live 频道或仓库线路。
 */
public class SiteItem {

    private final String name;
    private final String url;
    private final String key;
    private final JsonElement json;

    public SiteItem(String name, String url, String key, JsonElement json) {
        this.name = name;
        this.url = url;
        this.key = key;
        this.json = json;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? getUrl() : name;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? getUrl() : key;
    }

    public JsonElement getJson() {
        return json;
    }
}
