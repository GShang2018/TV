package com.fongmi.android.tv.web;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Url;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.player.Source;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebHome 多集推送：把页面整剧数据（含选集）暂存本地，以独立站点 key 进入原生详情/选集/播放；
 * 选集无可播直链时，播放时按集回调页面 resolver 解析。
 */
public class WebHomeInlineVodStore {

    public static final String KEY = "webhome_inline";
    private static final String HLS_FORMAT = "application/x-mpegURL";

    private static final Map<String, Entry> ITEMS = new ConcurrentHashMap<>();
    private static final Map<String, HeaderSpec> URL_HEADERS = new ConcurrentHashMap<>();
    private static final Map<String, EpisodeSpec> URL_EPISODES = new ConcurrentHashMap<>();

    public static String put(JsonObject payload) {
        return put(payload, null);
    }

    public static String put(JsonObject payload, Resolver resolver) {
        Vod vod = vodFrom(payload);
        String id = first(vod.getVodId(), Json.safeString(payload, "vodId"), Json.safeString(payload, "id"), "webhome-" + UUID.randomUUID());
        Map<String, String> headers = HeaderPolicy.withDefaultUa(HeaderPolicy.parse(payload.get("headers")));
        HeaderSpec headerSpec = new HeaderSpec(headers, "include".equals(Json.safeString(payload, "credentials")));
        String playUrl = playUrl(payload.get("episodes"), headerSpec, resolver);
        vod.setVodId(id);
        if (vod.getVodName().isEmpty()) vod.setVodName(first(Json.safeString(payload, "vodName"), Json.safeString(payload, "title"), id));
        if (vod.getVodPic().isEmpty()) vod.setVodPic(first(Json.safeString(payload, "vodPic"), Json.safeString(payload, "pic")));
        if (vod.getVodPlayFrom().isEmpty()) vod.setVodPlayFrom(first(Json.safeString(payload, "vodPlayFrom"), Json.safeString(payload, "playFrom"), "WebHome"));
        if (vod.getVodPlayUrl().isEmpty()) vod.setVodPlayUrl(playUrl);
        if (vod.getVodPlayUrl().isEmpty()) {
            SpiderDebug.log("webhome-inline", "put empty playUrl id=%s payload=%s", id, payload == null ? "null" : payload.toString());
        }
        ITEMS.put(id, new Entry(App.gson().toJson(vod), headerSpec, resolver));
        return id;
    }

    public static Result detail(String id) {
        Entry entry = ITEMS.get(id);
        if (entry == null || TextUtils.isEmpty(entry.vod)) return Result.error("WebHome inline VOD not found");
        SpiderDebug.log("webhome-inline", "detail id=%s found=%s", id, true);
        return Result.vod(vodFrom(entry.vod));
    }

    public static Result player(String flag, String id) throws Exception {
        try {
            return playerInternal(flag, id);
        } catch (Throwable e) {
            String message = e == null ? "未知错误" : e.getMessage();
            if (TextUtils.isEmpty(message)) message = e.getClass().getSimpleName();
            SpiderDebug.log("webhome-inline", "player failed id=%s error=%s where=%s", id, message, where(e));
            SpiderDebug.log("webhome-inline", e);
            if (e instanceof Exception) throw (Exception) e;
            throw new RuntimeException(e);
        }
    }

    // 失败位置摘要：取堆栈里最靠前的几个类名.方法名，便于直接定位抛错源
    private static String where(Throwable e) {
        try {
            StackTraceElement[] trace = e.getStackTrace();
            StringBuilder builder = new StringBuilder();
            int count = 0;
            for (StackTraceElement element : trace) {
                String name = element.getClassName();
                int dot = name.lastIndexOf('.');
                if (dot >= 0) name = name.substring(dot + 1);
                String frame = name + "." + element.getMethodName();
                if (builder.length() > 0) builder.append("→");
                builder.append(frame);
                if (++count >= 4) break;
            }
            return builder.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static Result playerInternal(String flag, String id) throws Exception {
        Entry entry = ITEMS.get(id);
        HeaderSpec headerSpec = URL_HEADERS.get(id);
        EpisodeSpec episodeSpec = URL_EPISODES.get(id);
        String url = id;
        String format = "";
        long start = System.currentTimeMillis();
        SpiderDebug.log("webhome-inline", "player start flag=%s id=%s entry=%s episode=%s", flag, id, entry != null, episodeSpec != null);
        if (episodeSpec != null) {
            ResolveResult resolved = resolve(entry, id, episodeSpec);
            url = resolved.url;
            format = resolved.format;
            headerSpec = resolved.headerSpec;
        }
        if (headerSpec == null && entry != null) headerSpec = entry.headerSpec;
        if (headerSpec == null) headerSpec = new HeaderSpec(new HashMap<String, String>(), false);
        // data-URI DASH 清单解码发布为本地 /webMpd，供原生播放器读取
        if (WebMpd.isDashData(url)) {
            url = WebMpd.publish(url);
            if (TextUtils.isEmpty(format)) format = "application/dash+xml";
        }
        Result result = new Result();
        // 首次必须先以 Url 对象填充（内部走 add），空列表直接 setUrl(String) 会触发 Url.replace 越界
        result.setUrl(Url.create().add(url));
        result.setParse(0);
        result.setFlag(flag);
        result.setHeader(App.gson().toJsonTree(headers(url, headerSpec)));
        if (!TextUtils.isEmpty(format)) result.setFormat(format);
        else if (isHls(url)) result.setFormat(HLS_FORMAT);
        SpiderDebug.log("webhome-inline", "player resolved cost=%sms id=%s url=%s format=%s", System.currentTimeMillis() - start, id, url, result.getFormat());
        result.setUrl(Source.get().fetch(result));
        SpiderDebug.log("webhome-inline", "player fetch ok cost=%sms id=%s url=%s", System.currentTimeMillis() - start, id, result.getUrl().v());
        return result;
    }

    private static Vod vodFrom(JsonObject payload) {
        try {
            return App.gson().fromJson(payload, Vod.class);
        } catch (Throwable e) {
            return new Vod();
        }
    }

    private static Vod vodFrom(String json) {
        try {
            return App.gson().fromJson(json, Vod.class);
        } catch (Throwable e) {
            return new Vod();
        }
    }

    private static Map<String, String> headers(String url, HeaderSpec headerSpec) {
        Map<String, String> result = HeaderPolicy.withDefaultUa(headerSpec.headers);
        boolean hasCookie = hasHeader(result, HttpHeaders.COOKIE);
        if (headerSpec.includeCookies && !hasCookie) {
            String cookie = CookieBridge.get(url);
            if (!TextUtils.isEmpty(cookie)) result.put("Cookie", cookie);
        }
        addBrowserCodeHeader(result);
        return result;
    }

    private static String playUrl(JsonElement element, HeaderSpec defaultSpec, Resolver resolver) {
        if (element == null || !element.isJsonArray()) return "";
        StringBuilder builder = new StringBuilder();
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement child = array.get(i);
            JsonObject object = child != null && child.isJsonObject() ? child.getAsJsonObject() : new JsonObject();
            String mediaUrl = Json.safeString(object, "mediaUrl");
            String pageUrl = first(Json.safeString(object, "pageUrl"), Json.safeString(object, "href"));
            String rawUrl = child != null && child.isJsonPrimitive() ? child.getAsString() : Json.safeString(object, "url");
            String url = first(mediaUrl, pageUrl, rawUrl);
            if (TextUtils.isEmpty(url)) continue;
            HeaderSpec episodeHeaders = episodeHeaders(object, defaultSpec);
            URL_HEADERS.put(url, episodeHeaders);
            if (child != null && child.isJsonObject()) {
                boolean resolve = bool(object, "resolve") || (!TextUtils.isEmpty(pageUrl) && TextUtils.isEmpty(mediaUrl));
                String format = first(Json.safeString(object, "format"), isHls(mediaUrl) ? HLS_FORMAT : "");
                URL_EPISODES.put(url, new EpisodeSpec(object.deepCopy(), episodeHeaders, resolver, resolve, mediaUrl, format));
                if (!TextUtils.isEmpty(mediaUrl)) URL_HEADERS.put(mediaUrl, episodeHeaders);
            }
            String name = first(Json.safeString(object, "name"), Json.safeString(object, "label"), Json.safeString(object, "title"), String.format("%02d", i + 1));
            if (builder.length() > 0) builder.append("#");
            builder.append(name.replace("$", " ").replace("#", " ")).append("$").append(url);
        }
        return builder.toString();
    }

    private static HeaderSpec episodeHeaders(JsonObject object, HeaderSpec defaultSpec) {
        Map<String, String> result = new HashMap<>(defaultSpec.headers);
        if (object != null && object.has("headers")) result.putAll(HeaderPolicy.parse(object.get("headers")));
        String referer = object == null ? "" : Json.safeString(object, "referer");
        if (!TextUtils.isEmpty(referer)) result.put(HttpHeaders.REFERER, referer);
        boolean includeCookies = defaultSpec.includeCookies || (object != null && "include".equals(Json.safeString(object, "credentials")));
        return new HeaderSpec(result, includeCookies);
    }

    private static ResolveResult resolve(Entry entry, String id, EpisodeSpec episodeSpec) throws Exception {
        if (!TextUtils.isEmpty(episodeSpec.mediaUrl)) {
            SpiderDebug.log("webhome-inline", "resolve cached id=%s url=%s", id, episodeSpec.mediaUrl);
            return new ResolveResult(episodeSpec.mediaUrl, episodeSpec.headerSpec, episodeSpec.format);
        }
        Resolver resolver = entry != null ? entry.resolver : episodeSpec.resolver;
        if (!episodeSpec.resolve || resolver == null) {
            String url = Json.safeString(episodeSpec.payload, "url");
            SpiderDebug.log("webhome-inline", "resolve direct id=%s url=%s resolve=%s resolver=%s", id, url, episodeSpec.resolve, resolver != null);
            return new ResolveResult(url, episodeSpec.headerSpec, episodeSpec.format);
        }
        long start = System.currentTimeMillis();
        SpiderDebug.log("webhome-inline", "resolve episode start id=%s page=%s", id, Json.safeString(episodeSpec.payload, "pageUrl"));
        JsonObject resolved = resolver.resolve(episodeSpec.payload.deepCopy());
        String url = Json.safeString(resolved, "url");
        if (TextUtils.isEmpty(url)) throw new IllegalStateException("WebHome inline episode resolve failed");
        HeaderSpec headerSpec = resolvedHeaders(resolved, episodeSpec.headerSpec);
        String format = first(Json.safeString(resolved, "format"), isHls(url) ? HLS_FORMAT : episodeSpec.format);
        URL_HEADERS.put(url, headerSpec);
        URL_EPISODES.put(id, new EpisodeSpec(episodeSpec.payload.deepCopy(), headerSpec, resolver, false, url, format));
        SpiderDebug.log("webhome-inline", "resolve episode ok cost=%sms id=%s url=%s", System.currentTimeMillis() - start, id, url);
        return new ResolveResult(url, headerSpec, format);
    }

    private static HeaderSpec resolvedHeaders(JsonObject resolved, HeaderSpec fallback) {
        Map<String, String> headers = new HashMap<>(fallback.headers);
        if (resolved != null && resolved.has("headers")) headers.putAll(HeaderPolicy.parse(resolved.get("headers")));
        String referer = resolved == null ? "" : Json.safeString(resolved, "referer");
        if (!TextUtils.isEmpty(referer)) headers.put(HttpHeaders.REFERER, referer);
        boolean includeCookies = fallback.includeCookies || (resolved != null && "include".equals(Json.safeString(resolved, "credentials")));
        return new HeaderSpec(headers, includeCookies);
    }

    private static void addBrowserCodeHeader(Map<String, String> headers) {
        if (hasHeader(headers, "browser-code")) return;
        String cookie = header(headers, HttpHeaders.COOKIE);
        if (TextUtils.isEmpty(cookie)) return;
        for (String part : cookie.split(";")) {
            String value = part.trim();
            if (!value.startsWith("browser-code=")) continue;
            headers.put("browser-code", value.substring("browser-code=".length()));
            return;
        }
    }

    private static boolean hasHeader(Map<String, String> headers, String name) {
        for (String key : headers.keySet()) if (name.equalsIgnoreCase(key)) return true;
        return false;
    }

    private static String header(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) if (name.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        return "";
    }

    private static boolean isHls(String url) {
        String value = url == null ? "" : url.toLowerCase();
        return value.contains(".m3u8") || value.contains("/playlist/");
    }

    private static boolean bool(JsonObject object, String name) {
        try {
            return object != null && object.has(name) && object.get(name).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static String first(String... values) {
        for (String value : values) if (!TextUtils.isEmpty(value)) return value.trim();
        return "";
    }

    public interface Resolver {
        JsonObject resolve(JsonObject payload) throws Exception;
    }

    private static class Entry {
        private final String vod;
        private final HeaderSpec headerSpec;
        private final Resolver resolver;

        private Entry(String vod, HeaderSpec headerSpec, Resolver resolver) {
            this.vod = vod;
            this.headerSpec = headerSpec;
            this.resolver = resolver;
        }
    }

    private static class HeaderSpec {
        private final Map<String, String> headers;
        private final boolean includeCookies;

        private HeaderSpec(Map<String, String> headers, boolean includeCookies) {
            this.headers = headers;
            this.includeCookies = includeCookies;
        }
    }

    private static class EpisodeSpec {
        private final JsonObject payload;
        private final HeaderSpec headerSpec;
        private final Resolver resolver;
        private final boolean resolve;
        private final String mediaUrl;
        private final String format;

        private EpisodeSpec(JsonObject payload, HeaderSpec headerSpec, Resolver resolver, boolean resolve, String mediaUrl, String format) {
            this.payload = payload;
            this.headerSpec = headerSpec;
            this.resolver = resolver;
            this.resolve = resolve;
            this.mediaUrl = mediaUrl;
            this.format = format;
        }
    }

    private static class ResolveResult {
        private final String url;
        private final HeaderSpec headerSpec;
        private final String format;

        private ResolveResult(String url, HeaderSpec headerSpec, String format) {
            this.url = url;
            this.headerSpec = headerSpec;
            this.format = format;
        }
    }
}
