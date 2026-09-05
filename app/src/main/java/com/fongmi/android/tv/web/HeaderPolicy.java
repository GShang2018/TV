package com.fongmi.android.tv.web;

import android.text.TextUtils;

import com.github.catvod.utils.Json;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;

public class HeaderPolicy {

    private static final String DEFAULT_UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static Map<String, String> parse(JsonElement element) {
        if (element == null || element.isJsonNull()) return new HashMap<>();
        Map<String, String> headers = Json.toMap(element);
        return headers == null ? new HashMap<>() : headers;
    }

    public static Map<String, String> parse(String json) {
        Map<String, String> headers = Json.toMap(json);
        return headers == null ? new HashMap<>() : headers;
    }

    public static Headers of(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (TextUtils.isEmpty(entry.getKey()) || TextUtils.isEmpty(entry.getValue())) continue;
            try {
                builder.add(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return builder.build();
    }

    public static Map<String, String> withDefaultUa(Map<String, String> headers) {
        boolean hasUa = false;
        for (String key : headers.keySet()) if ("User-Agent".equalsIgnoreCase(key)) hasUa = true;
        if (!hasUa) headers.put("User-Agent", DEFAULT_UA);
        return headers;
    }

    public static boolean hasCookie(Map<String, String> headers) {
        for (String key : headers.keySet()) if ("Cookie".equalsIgnoreCase(key)) return true;
        return false;
    }
}
