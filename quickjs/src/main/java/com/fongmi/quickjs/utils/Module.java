package com.fongmi.quickjs.utils;

import android.net.Uri;
import android.util.Base64;
import android.util.LruCache;

import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Asset;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import java.io.File;
import java.nio.charset.StandardCharsets;

import okhttp3.Headers;

public class Module {

    private static final int MAX_CACHE_SIZE = 64; // 最多缓存 64 个 JS 模块

    private final LruCache<String, String> cache;

    private static class Loader {
        static volatile Module INSTANCE = new Module();
    }

    public static Module get() {
        return Loader.INSTANCE;
    }

    public Module() {
        this.cache = new LruCache<>(MAX_CACHE_SIZE);
    }

    public String fetch(String name) {
        String cached = cache.get(name);
        if (cached != null) return cached;
        if (name.startsWith("http")) {
            String content = request(name);
            if (!content.isEmpty()) cache.put(name, content);
            return content;
        }
        if (name.startsWith("assets")) {
            String content = Asset.read(name);
            cache.put(name, content);
            return content;
        }
        if (name.startsWith("lib/")) {
            String content = Asset.read("js/" + name);
            cache.put(name, content);
            return content;
        }
        return "";
    }

    private String request(String url) {
        try {
            Uri uri = Uri.parse(url);
            File file = Path.js(uri.getLastPathSegment());
            boolean cache = !"127.0.0.1".equals(uri.getHost());
            byte[] data = OkHttp.newCall(url, Headers.of(HttpHeaders.USER_AGENT, "Mozilla/5.0")).execute().body().bytes();
            if (cache) new Thread(() -> Path.write(file, data)).start();
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cache(url);
        }
    }

    private String cache(String url) {
        try {
            Uri uri = Uri.parse(url);
            File file = Path.js(uri.getLastPathSegment());
            return file.exists() ? Path.read(file) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public byte[] bb(String content) {
        byte[] bytes = Base64.decode(content.substring(4), Base64.DEFAULT);
        byte[] newBytes = new byte[bytes.length - 4];
        newBytes[0] = 1;
        System.arraycopy(bytes, 5, newBytes, 1, bytes.length - 5);
        return newBytes;
    }
}
