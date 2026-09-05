package com.fongmi.android.tv.web;

import android.app.Activity;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.drive.DriveCheckRequest;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DriveCheckService;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HomeWebBridge {

    private static final int INLINE_LIMIT = 12000;
    private static final int CHUNK_SIZE = 60000;
    private static final String PUSH_KEY = "push_agent";

    private final HomeWebController controller;
    private final Activity activity;
    private final WebView webView;
    private final Map<String, String> results;

    public HomeWebBridge(HomeWebController controller, Activity activity, WebView webView) {
        this.controller = controller;
        this.activity = activity;
        this.webView = webView;
        this.results = new ConcurrentHashMap<>();
    }

    @JavascriptInterface
    public void invoke(String requestId, String method, String payload) {
        App.execute(() -> handle(requestId, method, WebCall.object(payload)));
    }

    @JavascriptInterface
    public int resultLength(String id) {
        String result = results.get(id);
        return result == null ? 0 : result.length();
    }

    @JavascriptInterface
    public String resultChunk(String id, int start) {
        String result = results.get(id);
        if (result == null || start < 0 || start >= result.length()) return "";
        return result.substring(start, Math.min(start + CHUNK_SIZE, result.length()));
    }

    @JavascriptInterface
    public void clearResult(String id) {
        results.remove(id);
    }

    @JavascriptInterface
    public void copy(String text) {
        try {
            android.content.ClipboardManager manager = (android.content.ClipboardManager) activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (manager != null) manager.setPrimaryClip(android.content.ClipData.newPlainText("", text == null ? "" : text));
        } catch (Throwable ignored) {
        }
    }

    private void handle(String requestId, String method, JsonObject payload) {
        try {
            SpiderDebug.log("webhome", "invoke method=%s", method);
            String result;
            switch (method) {
                case "net.request":
                    result = WebCall.request(payload);
                    break;
                case "player.playUrl":
                    result = playUrl(payload);
                    break;
                case "player.playVod":
                    result = playVod(payload);
                    break;
                case "player.preloadArtwork":
                    result = "{}";
                    break;
                case "app.search":
                    result = search(payload);
                    break;
                case "app.openVod":
                    result = openVod();
                    break;
                case "app.openLive":
                    result = openLive();
                    break;
                case "app.history":
                    result = history();
                    break;
                case "pan.check":
                    result = checkLinks(payload);
                    break;
                case "pan.play":
                    result = playPan(payload);
                    break;
                case "cache.get":
                    result = quote(Prefers.getString(cacheKey(payload)));
                    break;
                case "cache.set":
                    result = cacheSet(payload);
                    break;
                case "cache.del":
                    result = cacheDel(payload);
                    break;
                case "device.info":
                    result = device();
                    break;
                case "site.info":
                    result = site();
                    break;
                case "config.info":
                    result = config();
                    break;
                case "ext.toast":
                    result = extToast(payload);
                    break;
                case "ui.setToolbar":
                case "ui.getViewport":
                    result = "{}";
                    break;
                case "navigation.back":
                    result = back();
                    break;
                case "navigation.reload":
                    result = reload();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
            resolve(requestId, result);
        } catch (Throwable e) {
            reject(requestId, e.getMessage());
        }
    }

    private String playUrl(JsonObject payload) {
        String url = Json.safeString(payload, "url");
        String title = Json.safeString(payload, "title");
        String pic = Json.safeString(payload, "pic");
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException("url不能为空");
        applyHeaders(payload);
        final String playTitle = TextUtils.isEmpty(title) ? url : title;
        SpiderDebug.log("webhome", "player.playUrl title=%s url=%s", playTitle, url);
        App.post(() -> VideoActivity.start(activity, PUSH_KEY, url, playTitle, pic));
        return "{}";
    }

    private void applyHeaders(JsonObject payload) {
        try {
            if (payload.has("headers") && payload.get("headers").isJsonObject()) {
                Map<String, String> headers = Json.toMap(payload.get("headers"));
                if (headers != null && !headers.isEmpty()) WebHomeHeaders.set(headers);
                else WebHomeHeaders.clear();
            } else {
                WebHomeHeaders.clear();
            }
        } catch (Throwable e) {
            WebHomeHeaders.clear();
        }
    }

    private String playVod(JsonObject payload) {
        String siteKey = Json.safeString(payload, "siteKey");
        String vodId = Json.safeString(payload, "vodId");
        String title = Json.safeString(payload, "title");
        String pic = Json.safeString(payload, "pic");
        if (TextUtils.isEmpty(siteKey) || TextUtils.isEmpty(vodId)) throw new IllegalArgumentException("siteKey/vodId不能为空");
        final String playTitle = TextUtils.isEmpty(title) ? vodId : title;
        App.post(() -> VideoActivity.start(activity, siteKey, vodId, playTitle, pic));
        return "{}";
    }

    private String search(JsonObject payload) {
        String keyword = Json.safeString(payload, "keyword");
        if (TextUtils.isEmpty(keyword)) throw new IllegalArgumentException("keyword不能为空");
        boolean direct = payload.has("direct") && payload.get("direct").getAsBoolean();
        if (direct) {
            // 详情页直达播放的搜索：透传详情页海报/剧照，作为结果项无图时的兜底封面
            String pic = Json.safeString(payload, "pic");
            String wall = Json.safeString(payload, "wallPic");
            String art = TextUtils.isEmpty(pic) ? wall : pic;
            if (!TextUtils.isEmpty(art)) WebHomeArt.set(art);
            else WebHomeArt.clear();
            App.post(() -> CollectActivity.start(activity, keyword));
        } else {
            // 普通搜索建议等不携带详情图，清掉会话残留
            WebHomeArt.clear();
            App.post(() -> CollectActivity.start(activity, keyword));
        }
        return "{}";
    }

    private String openVod() {
        App.post(controller::requestExit);
        return "{}";
    }

    private String openLive() {
        App.post(() -> LiveActivity.start(activity));
        return "{}";
    }

    private String history() {
        return App.gson().toJson(History.get());
    }

    private String checkLinks(JsonObject payload) {
        if (!Setting.isDriveCheck()) throw new IllegalStateException("网盘检测未开启");
        DriveCheckRequest request = App.gson().fromJson(payload, DriveCheckRequest.class);
        if (request == null || request.getItems().isEmpty()) throw new IllegalArgumentException("items不能为空");
        SpiderDebug.log("webhome", "pan.check count=%s", request.getItems().size());
        return App.gson().toJson(DriveCheckService.get().check(request.getItems()));
    }

    private String playPan(JsonObject payload) {
        String url = Json.safeString(payload, "url");
        String title = Json.safeString(payload, "title");
        String pic = Json.safeString(payload, "pic");
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException("url不能为空");
        applyHeaders(payload);
        final String playUrl = stripPush(url.trim());
        final String playTitle = TextUtils.isEmpty(title) ? playUrl : title;
        SpiderDebug.log("webhome", "pan.play type=%s title=%s url=%s", Json.safeString(payload, "type"), playTitle, playUrl);
        App.post(() -> VideoActivity.start(activity, PUSH_KEY, playUrl, playTitle, pic));
        return "{}";
    }

    private String stripPush(String url) {
        return url.regionMatches(true, 0, "push://", 0, 7) ? url.substring(7) : url;
    }

    private String cacheSet(JsonObject payload) {
        Prefers.put(cacheKey(payload), Json.safeString(payload, "value"));
        return "{}";
    }

    private String cacheDel(JsonObject payload) {
        Prefers.remove(cacheKey(payload));
        return "{}";
    }

    private String cacheKey(JsonObject payload) {
        String rule = Json.safeString(payload, "rule");
        String key = Json.safeString(payload, "key");
        return "cache_" + (TextUtils.isEmpty(rule) ? "" : rule + "_") + key;
    }

    private String device() {
        JsonObject object = new JsonObject();
        object.addProperty("address", Server.get().getAddress());
        return object.toString();
    }

    private String site() {
        Site site = VodConfig.get().getHome();
        JsonObject object = new JsonObject();
        object.addProperty("key", site.getKey());
        object.addProperty("name", site.getName());
        object.addProperty("homePage", site.getHomePage());
        object.addProperty("type", site.getType());
        object.add("header", App.gson().toJsonTree(site.getHeader()));
        return object.toString();
    }

    private String config() {
        JsonObject object = new JsonObject();
        object.addProperty("id", VodConfig.getCid());
        object.addProperty("url", VodConfig.getUrl());
        object.addProperty("desc", VodConfig.getDesc());
        return object.toString();
    }

    private String extToast(JsonObject payload) {
        String message = Json.safeString(payload, "message");
        if (!TextUtils.isEmpty(message)) App.post(() -> Notify.show(message));
        return "{}";
    }

    private String back() {
        App.post(() -> controller.handleBack());
        return "{}";
    }

    private String reload() {
        App.post(() -> controller.reload());
        return "{}";
    }

    private void resolve(String requestId, String data) {
        String payload = TextUtils.isEmpty(data) ? "null" : data;
        if (payload.length() > INLINE_LIMIT) {
            String resultId = requestId + "_" + System.nanoTime();
            results.put(resultId, payload);
            payload = "{\"__fmResultId\":" + quote(resultId) + "}";
        }
        eval("window.fongmiNative&&window.fongmiNative.resolve(" + quote(requestId) + "," + payload + ")");
    }

    private void reject(String requestId, String error) {
        eval("window.fongmiNative&&window.fongmiNative.reject(" + quote(requestId) + "," + quote(error) + ")");
    }

    private void eval(String script) {
        App.post(() -> webView.evaluateJavascript(script, null));
    }

    private static String quote(String text) {
        return App.gson().toJson(text == null ? "" : text);
    }
}
