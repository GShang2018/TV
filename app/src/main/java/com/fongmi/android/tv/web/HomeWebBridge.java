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
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DriveCheckService;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.web.ext.WebHomeExtensionRegistry;
import com.bumptech.glide.Glide;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HomeWebBridge {

    private static final int INLINE_LIMIT = 12000;
    private static final int CHUNK_SIZE = 60000;
    private static final long INLINE_RESOLVE_TIMEOUT_SECONDS = 20;
    private static final String PUSH_KEY = "push_agent";

    private final HomeWebController controller;
    private final Activity activity;
    private final WebView webView;
    private final Map<String, String> results;
    private final Map<String, CompletableFuture<String>> inlineResults;

    public HomeWebBridge(HomeWebController controller, Activity activity, WebView webView) {
        this.controller = controller;
        this.activity = activity;
        this.webView = webView;
        this.results = new ConcurrentHashMap<>();
        this.inlineResults = new ConcurrentHashMap<>();
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
    public void inlineResult(String id, String payload) {
        CompletableFuture<String> future = inlineResults.remove(id);
        if (future != null) future.complete(payload);
    }

    // 同步取 /webResource 网关地址（页面 fm.res 依赖同步返回值做 URL 拼接）
    @JavascriptInterface
    public String resourceUrl(String url, String options) {
        try {
            JsonObject payload = WebCall.object(options);
            payload.addProperty("url", url == null ? "" : url);
            return resourceUrl(payload);
        } catch (Throwable e) {
            return url == null ? "" : url;
        }
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
                case "net.resourceUrl":
                    // 返回本地 /webResource 网关地址，供页面跨域获取资源
                    result = quote(resourceUrl(payload));
                    break;
                case "player.playUrl":
                    result = playUrl(payload);
                    break;
                case "player.playVod":
                    result = playVod(payload);
                    break;
                case "player.playVodInline":
                    result = playVodInline(payload);
                    break;
                case "player.preloadArtwork":
                    result = preloadArtwork(payload);
                    break;
                case "player.control":
                    result = control(payload);
                    break;
                case "player.status":
                    result = status();
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
                case "app.openSetting":
                    result = openSetting();
                    break;
                case "app.openKeep":
                    result = openKeep();
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
                case "ext.info":
                    result = extInfo();
                    break;
                case "ext.log":
                    result = extLog(payload);
                    break;
                case "ext.toast":
                    result = extToast(payload);
                    break;
                // TV 端默认普通 Chrome；宿主可通过 Listener.setChrome 等实现工具栏/沉浸式显隐
                case "ui.setToolbar":
                    result = setToolbar(payload);
                    break;
                case "ui.setChrome":
                    result = setChrome(payload);
                    break;
                case "ui.restoreChrome":
                    result = restoreChrome();
                    break;
                case "ui.getViewport":
                    result = controller.getViewportJson();
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

    // 页面多集推送：整剧数据（含选集）存入 WebHomeInlineVodStore，以 webhome_inline 站点进入原生详情/选集/播放
    private String playVodInline(JsonObject payload) throws Exception {
        SpiderDebug.log("webhome", "playVodInline keys=%s", payload == null ? "null" : payload.keySet());
        if (payload == null) throw new IllegalArgumentException("payload不能为空");
        int episodeCount = payload.has("episodes") && payload.get("episodes").isJsonArray() ? payload.getAsJsonArray("episodes").size() : 0;
        SpiderDebug.log("webhome", "playVodInline episodes=%s", episodeCount);
        if (!payload.has("episodes") && payload.has("url")) {
            JsonObject episode = new JsonObject();
            episode.addProperty("name", Json.safeString(payload, "name"));
            episode.addProperty("mediaUrl", Json.safeString(payload, "url"));
            com.google.gson.JsonArray episodes = new com.google.gson.JsonArray();
            episodes.add(episode);
            payload.add("episodes", episodes);
            SpiderDebug.log("webhome", "playVodInline wrapped single url as episode");
        }
        String vodId = WebHomeInlineVodStore.put(payload, this::resolveInlineEpisode);
        String title = Json.safeString(payload, "title");
        if (TextUtils.isEmpty(title)) title = Json.safeString(payload, "vod_name");
        if (TextUtils.isEmpty(title)) title = vodId;
        String pic = Json.safeString(payload, "pic");
        if (TextUtils.isEmpty(pic)) pic = Json.safeString(payload, "vod_pic");
        final String playTitle = title;
        final String playPic = pic;
        SpiderDebug.log("webhome", "player.playVodInline title=%s id=%s", playTitle, vodId);
        App.post(() -> VideoActivity.start(activity, WebHomeInlineVodStore.KEY, vodId, playTitle, playPic));
        JsonObject result = new JsonObject();
        result.addProperty("siteKey", WebHomeInlineVodStore.KEY);
        result.addProperty("vodId", vodId);
        return result.toString();
    }

    // 把单集数据交给页面侧 resolver（__fmWebHomeInlineResolver / __fmYmvidResolveEpisode）解析出可播直链
    private JsonObject resolveInlineEpisode(JsonObject payload) throws Exception {
        String id = "inline_" + UUID.randomUUID().toString().replace("-", "");
        CompletableFuture<String> future = new CompletableFuture<>();
        inlineResults.put(id, future);
        String script = "(function(){\n" +
                "  const id=" + quote(id) + ";\n" +
                "  const payload=" + (payload == null ? "{}" : payload.toString()) + ";\n" +
                "  const done=function(value){try{fongmiBridge.inlineResult(id,JSON.stringify(value||{}));}catch(e){}};\n" +
                "  const fail=function(error){const message=error&&error.message?error.message:String(error||'');done({error:message});};\n" +
                "  try{\n" +
                "    const resolver=window.__fmWebHomeInlineResolver||window.__fmYmvidResolveEpisode;\n" +
                "    if(typeof resolver!=='function'){fail('inline resolver unavailable');return;}\n" +
                "    Promise.resolve(resolver(payload)).then(done,fail);\n" +
                "  }catch(e){fail(e);}\n" +
                "})();";
        long start = System.currentTimeMillis();
        boolean lease = controller.beginInlineEvaluation();
        try {
            SpiderDebug.log("webhome-inline", "resolve start id=%s page=%s lease=%s", id, Json.safeString(payload, "pageUrl"), lease);
            eval(script);
            String result = future.get(INLINE_RESOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonObject object = WebCall.object(result);
            String error = Json.safeString(object, "error");
            if (!TextUtils.isEmpty(error)) throw new IllegalStateException(error);
            if (TextUtils.isEmpty(Json.safeString(object, "url"))) throw new IllegalStateException("inline resolve empty url");
            SpiderDebug.log("webhome-inline", "resolve ok id=%s cost=%sms", id, System.currentTimeMillis() - start);
            return object;
        } catch (Throwable e) {
            SpiderDebug.log("webhome-inline", "resolve failed id=%s cost=%sms error=%s", id, System.currentTimeMillis() - start, e.getMessage());
            if (e instanceof Exception) throw (Exception) e;
            throw new RuntimeException(e);
        } finally {
            inlineResults.remove(id);
            controller.endInlineEvaluation(lease);
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

    // 构造本地 /webResource 网关地址：携带 url、自定义 headers 与 credentials 参数
    private String resourceUrl(JsonObject payload) {
        StringBuilder builder = new StringBuilder(Server.get().getAddress("/webResource?url=")).append(encode(Json.safeString(payload, "url")));
        if (payload.has("headers")) builder.append("&headers=").append(encode(payload.get("headers").toString()));
        if ("include".equals(Json.safeString(payload, "credentials"))) builder.append("&credentials=include");
        return builder.toString();
    }

    private String openSetting() {
        App.post(controller::openSetting);
        return "{}";
    }

    private String preloadArtwork(JsonObject payload) {
        App.execute(() -> {
            preload(UrlUtil.convert(Json.safeString(payload, "pic")));
            preload(UrlUtil.convert(Json.safeString(payload, "wallPic")));
        });
        return "{}";
    }

    private void preload(String url) {
        if (TextUtils.isEmpty(url)) return;
        try {
            Glide.with(App.get()).load(url).preload();
        } catch (Throwable ignored) {
        }
    }

    // 页面在播控制：映射到当前播放器的媒体会话（仅在确有播放器时生效，否则静默）
    private String control(JsonObject payload) {
        String action = Json.safeString(payload, "action");
        App.post(() -> {
            Players player = Server.get().getPlayer();
            if (player == null) return;
            try {
                if ("play".equals(action)) player.play();
                else if ("pause".equals(action)) player.pause();
                else if ("stop".equals(action)) player.stop();
                else SpiderDebug.log("webhome", "player.control unsupported action=%s", action);
            } catch (Throwable ignored) {
            }
        });
        return "{}";
    }

    // 播放状态查询：复用本地 /media 端点（返回 ok/status/headers/body 包裹，body 为播放状态 JSON）
    private String status() {
        JsonObject payload = new JsonObject();
        payload.addProperty("url", Server.get().getAddress("/media"));
        payload.addProperty("responseType", "json");
        return WebCall.request(payload);
    }

    private String openKeep() {
        App.post(() -> KeepActivity.start(activity));
        return "{}";
    }

    private String extInfo() {
        Site site = VodConfig.get().getHome();
        WebHomeExtensionRegistry.Snapshot snapshot = WebHomeExtensionRegistry.get().snapshot();
        JsonObject object = new JsonObject();
        object.addProperty("siteKey", site.getKey());
        object.addProperty("siteName", site.getName());
        object.addProperty("homePage", site.getHomePage());
        object.addProperty("enabled", snapshot.enabled);
        object.addProperty("matched", snapshot.matchedCount);
        object.addProperty("ready", snapshot.readyCount);
        return object.toString();
    }

    private String extLog(JsonObject payload) {
        WebHomeExtensionRegistry.get().recordScriptLog(payload);
        SpiderDebug.log("webhome-ext", "script message=%s data=%s", Json.safeString(payload, "message"), payload.has("data") ? payload.get("data") : "");
        return "{}";
    }

    private String setToolbar(JsonObject payload) {
        boolean visible = !payload.has("visible") || (payload.get("visible").isJsonPrimitive() && payload.get("visible").getAsBoolean());
        controller.setToolbar(visible);
        return "{}";
    }

    private String setChrome(JsonObject payload) {
        controller.setChrome(payload);
        return "{}";
    }

    private String restoreChrome() {
        controller.restoreChrome();
        return "{}";
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

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
