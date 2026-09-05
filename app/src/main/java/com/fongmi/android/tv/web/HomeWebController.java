package com.fongmi.android.tv.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ConsoleMessage;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeWebController {

    private static final String BRIDGE = "fongmiBridge";
    private static final String SDK_ASSET = "webhome/sdk.js";
    private static final long LOAD_TIMEOUT_MS = 15000;

    private final Listener listener;
    private final Activity activity;
    private final float density;
    private WebView webView;
    private String defaultUserAgent;
    private String sdkScript;
    private Site site;
    private String homePage;
    private String lastPageUrl;
    private long pauseAt;
    private int loadToken;
    private int loadTimeoutRecoveries;
    private boolean sdkReady;
    private boolean paused;

    public HomeWebController(Activity activity, WebView webView, Listener listener) {
        this.activity = activity;
        this.webView = webView;
        this.listener = listener;
        this.density = activity.getResources().getDisplayMetrics().density;
        init();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {
        configureWebView(webView);
        defaultUserAgent = webView.getSettings().getUserAgentString();
        webView.setBackgroundColor(Color.TRANSPARENT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new HomeWebBridge(this, activity, webView), BRIDGE);
        webView.setWebViewClient(client());
        webView.setWebChromeClient(chrome());
    }

    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private boolean isLeanback() {
        return "leanback".equals(BuildConfig.FLAVOR_mode);
    }

    public void requestExit() {
        App.post(listener::onExit);
    }

    public boolean load(Site site) {
        if (site == null || !site.hasHomePage()) return false;
        Server.get().start();
        String url = getHomePage(site);
        this.site = site;
        boolean reload = !url.equals(homePage);
        if (reload) {
            sdkReady = false;
            homePage = url;
            loadUrl(homePage);
        }
        show();
        return true;
    }

    public void reload() {
        if (TextUtils.isEmpty(homePage)) {
            webView.reload();
        } else {
            webView.clearCache(false);
            loadUrl(reloadUrl(homePage));
        }
    }

    private void loadUrl(String url) {
        Map<String, String> requestHeaders = requestHeaders(url);
        lastPageUrl = url;
        int token = ++loadToken;
        SpiderDebug.log("webhome-webview", "load url=%s", url);
        if (requestHeaders.isEmpty()) webView.loadUrl(url);
        else webView.loadUrl(url, requestHeaders);
        webView.postDelayed(() -> handleLoadTimeout(token, url), LOAD_TIMEOUT_MS);
    }

    private void handleLoadTimeout(int token, String url) {
        if (token != loadToken || !isVisible() || activity.isFinishing() || activity.isDestroyed()) return;
        SpiderDebug.log("webhome-webview", "load timeout url=%s current=%s recoveries=%s", url, webView.getUrl(), loadTimeoutRecoveries);
        if (TextUtils.isEmpty(homePage) || loadTimeoutRecoveries++ > 0) {
            listener.onWebError();
            return;
        }
        String target = !TextUtils.isEmpty(lastPageUrl) && !isEmptyDocumentUrl(lastPageUrl) ? lastPageUrl : homePage;
        recreateWebView();
        listener.onWebLoading();
        loadUrl(reloadUrl(target, true));
    }

    private Map<String, String> requestHeaders(String url) {
        Map<String, String> headers;
        try {
            headers = Json.toMap(site == null ? null : site.getHeader());
        } catch (Throwable e) {
            headers = null;
        }
        if (headers == null || headers.isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (TextUtils.isEmpty(key) || value == null) continue;
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key)) {
                if (!TextUtils.isEmpty(value)) webView.getSettings().setUserAgentString(value);
                else if (!TextUtils.isEmpty(defaultUserAgent)) webView.getSettings().setUserAgentString(defaultUserAgent);
                continue;
            }
            if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) {
                CookieManager.getInstance().setCookie(url, value);
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

    public void evaluate(String script, ValueCallback<String> callback) {
        webView.post(() -> webView.evaluateJavascript(script, callback));
    }

    public void show() {
        webView.setVisibility(View.VISIBLE);
        focusWebView("show");
    }

    public void hide() {
        webView.setVisibility(View.GONE);
    }

    public boolean isVisible() {
        return webView.getVisibility() == View.VISIBLE;
    }

    public boolean handleBack() {
        if (!isVisible()) return false;
        // ① 先把 BACK 键作为 keydown 送进页面（nostr 等页面会把 GoBack/keyCode 4 归一为 Escape，
        //    由页面内部层级栈自行回退详情/图片/盘搜面板等非 URL 状态），页面消耗则直接返回。
        if (dispatchBackToPage()) return true;
        // ② 页面未消耗时回退 WebView 历史（URL/hash 状态）
        if (!webView.canGoBack()) return false;
        String current = webView.getUrl();
        if (samePage(current, homePage)) return false;
        String previous = previousHistoryUrl();
        if (!sameSite(current, previous)) return false;
        webView.goBack();
        return true;
    }

    private boolean dispatchBackToPage() {
        try {
            long now = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0);
            KeyEvent up = new KeyEvent(now + 30, now + 30, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0);
            boolean handled = webView.dispatchKeyEvent(down);
            if (!handled) handled = webView.dispatchKeyEvent(up);
            return handled;
        } catch (Throwable e) {
            return false;
        }
    }

    private String previousHistoryUrl() {
        try {
            WebBackForwardList list = webView.copyBackForwardList();
            int index = list.getCurrentIndex() - 1;
            WebHistoryItem item = index >= 0 ? list.getItemAtIndex(index) : null;
            return item == null ? "" : item.getUrl();
        } catch (Throwable e) {
            return "";
        }
    }

    private boolean sameSite(String current, String target) {
        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(target)) return false;
        Uri currentUri = Uri.parse(current);
        Uri targetUri = Uri.parse(target);
        String currentHost = UrlUtil.host(currentUri);
        String targetHost = UrlUtil.host(targetUri);
        if (currentHost.isEmpty() || targetHost.isEmpty()) return current.equals(target);
        return UrlUtil.scheme(currentUri).equals(UrlUtil.scheme(targetUri)) && currentHost.equals(targetHost) && port(currentUri) == port(targetUri);
    }

    private boolean samePage(String current, String target) {
        if (!sameSite(current, target)) return false;
        Uri currentUri = Uri.parse(current);
        Uri targetUri = Uri.parse(target);
        return path(currentUri).equals(path(targetUri))
                && cleanQuery(currentUri).equals(cleanQuery(targetUri))
                && fragment(currentUri).equals(fragment(targetUri));
    }

    private String path(Uri uri) {
        String path = uri.getEncodedPath();
        return TextUtils.isEmpty(path) ? "/" : path;
    }

    private String fragment(Uri uri) {
        String fragment = uri.getEncodedFragment();
        return fragment == null ? "" : fragment;
    }

    private String cleanQuery(Uri uri) {
        String query = uri.getEncodedQuery();
        if (TextUtils.isEmpty(query)) return "";
        StringBuilder result = new StringBuilder();
        for (String part : query.split("&")) {
            int index = part.indexOf('=');
            String name = index >= 0 ? part.substring(0, index) : part;
            if ("_fm_reload".equals(name) || "_fm_restore".equals(name)) continue;
            if (result.length() > 0) result.append('&');
            result.append(part);
        }
        return result.toString();
    }

    private int port(Uri uri) {
        int port = uri.getPort();
        if (port >= 0) return port;
        String scheme = UrlUtil.scheme(uri);
        if ("http".equals(scheme)) return 80;
        if ("https".equals(scheme)) return 443;
        return -1;
    }

    public void onResume() {
        paused = false;
        webView.onResume();
        webView.resumeTimers();
        recoverAfterResume();
    }

    public void onPause() {
        paused = true;
        pauseAt = System.currentTimeMillis();
        dispatchLifecycle("fmpause", "{time:" + pauseAt + "}");
        webView.onPause();
    }

    public void destroy() {
        webView.stopLoading();
        webView.destroy();
    }

    private void recreateWebView() {
        ViewGroup parent = webView.getParent() instanceof ViewGroup ? (ViewGroup) webView.getParent() : null;
        if (parent == null) return;
        int index = parent.indexOfChild(webView);
        int id = webView.getId();
        int visibility = webView.getVisibility();
        ViewGroup.LayoutParams params = webView.getLayoutParams();
        try {
            webView.stopLoading();
            parent.removeView(webView);
            webView.destroy();
        } catch (Throwable ignored) {
        }
        webView = new WebView(activity);
        webView.setId(id);
        webView.setVisibility(visibility);
        parent.addView(webView, Math.max(0, index), params);
        init();
    }

    private void recoverAfterResume() {
        if (!isVisible()) return;
        String current = webView.getUrl();
        if (isEmptyDocumentUrl(current) && !TextUtils.isEmpty(homePage)) {
            String target = !TextUtils.isEmpty(lastPageUrl) && !isEmptyDocumentUrl(lastPageUrl) ? lastPageUrl : homePage;
            SpiderDebug.log("webhome-webview", "restore reload reason=empty-url target=%s", target);
            sdkReady = false;
            loadUrl(reloadUrl(target, true));
            return;
        }
        webView.setBackgroundColor(Color.TRANSPARENT);
        focusWebView("resume");
        webView.requestLayout();
        webView.invalidate();
        webView.postInvalidateOnAnimation();
        dispatchResume(0);
        dispatchResume(80);
    }

    private boolean isEmptyDocumentUrl(String url) {
        return TextUtils.isEmpty(url) || "about:blank".equalsIgnoreCase(url);
    }

    private void dispatchResume(long delay) {
        webView.postDelayed(() -> {
            long now = System.currentTimeMillis();
            long pausedMs = pauseAt > 0 ? Math.max(0, now - pauseAt) : 0;
            dispatchLifecycle("fmresume", "{time:" + now + ",pausedMs:" + pausedMs + "}");
        }, delay);
    }

    private void nudgeCompositor() {
        webView.setAlpha(0.99f);
        webView.postDelayed(() -> {
            webView.setAlpha(1f);
            webView.invalidate();
            webView.postInvalidateOnAnimation();
        }, 50);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!isVisible() || !isLeanback() || !isRemoteKey(event)) return false;
        focusWebView("key");
        return webView.dispatchKeyEvent(event);
    }

    private boolean isRemoteKey(KeyEvent event) {
        int code = event.getKeyCode();
        return code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER;
    }

    private boolean focusWebView(String reason) {
        if (webView.hasFocus()) return true;
        boolean ok = webView.requestFocus();
        SpiderDebug.log("webhome-focus", "request reason=%s ok=%s url=%s", reason, ok, webView.getUrl());
        return ok;
    }

    private void dispatchLifecycle(String event, String detail) {
        String script = "(function(){try{window.dispatchEvent(new CustomEvent('" + event + "',{detail:" + detail + "}));}catch(e){}})();";
        webView.post(() -> webView.evaluateJavascript(script, null));
    }

    private WebViewClient client() {
        return new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                SpiderDebug.log("webhome-webview", "page started url=%s", url);
                lastPageUrl = url;
                sdkReady = false;
                listener.onWebLoading();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                SpiderDebug.log("webhome-webview", "page finished url=%s title=%s", url, view.getTitle());
                loadToken++;
                loadTimeoutRecoveries = 0;
                lastPageUrl = url;
                injectSdk();
                focusWebView("page-finished");
                listener.onWebReady();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                SpiderDebug.log("webhome-webview", "resource error main=%s code=%s url=%s", request.isForMainFrame(), error.getErrorCode(), request.getUrl());
                if (request.isForMainFrame() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    loadToken++;
                    Notify.show(error.getDescription().toString());
                    listener.onWebError();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                SpiderDebug.log("webhome-webview", "render process gone didCrash=%s", detail.didCrash());
                recreateWebView();
                if (!TextUtils.isEmpty(homePage)) {
                    listener.onWebLoading();
                    loadUrl(reloadUrl(homePage, true));
                } else {
                    listener.onWebError();
                }
                return true;
            }
        };
    }

    private WebChromeClient chrome() {
        return new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                if (message != null) {
                    SpiderDebug.log("webhome-console", String.format(Locale.ROOT, "%s %s:%s %s", message.messageLevel(), message.sourceId(), message.lineNumber(), message.message()));
                }
                return super.onConsoleMessage(message);
            }
        };
    }

    private void injectSdk() {
        // WebView 无浏览器剪贴板权限模型：接管 navigator.clipboard 走原生剪贴板
        webView.evaluateJavascript(getClipboardPatch(), null);
        webView.evaluateJavascript(getSdk(), value -> {
            sdkReady = true;
            nudgeCompositor();
        });
    }

    private String getClipboardPatch() {
        return "(function(){try{" +
                "if(window.__fmClipboardPatched)return;window.__fmClipboardPatched=true;" +
                "var bridge=window.fongmiBridge;" +
                "var write=function(text){if(bridge)bridge.copy(String(text==null?'':text));return Promise.resolve();};" +
                "try{" +
                "if(!navigator.clipboard){Object.defineProperty(navigator,'clipboard',{value:{writeText:write,readText:function(){return Promise.resolve('');}}});}" +
                "else if(!navigator.clipboard.__fmPatched){navigator.clipboard.writeText=write;navigator.clipboard.__fmPatched=true;}" +
                "}catch(e){try{navigator.clipboard={writeText:write};}catch(e2){}}" +
                "}catch(e){}})();";
    }

    private String getSdk() {
        if (sdkScript == null) sdkScript = readSdkAsset();
        return sdkScript;
    }

    private String readSdkAsset() {
        StringBuilder builder = new StringBuilder();
        try (InputStream input = activity.getAssets().open(SDK_ASSET); BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        } catch (Throwable e) {
            SpiderDebug.log("webhome", "read sdk asset error=%s", e.getMessage());
        }
        return builder.toString().replace("__FM_MODE__", BuildConfig.FLAVOR_mode).replace("__FM_LEANBACK__", String.valueOf(isLeanback()));
    }

    private String reloadUrl(String url) {
        return reloadUrl(url, false);
    }

    private String reloadUrl(String url, boolean restore) {
        try {
            Uri.Builder builder = Uri.parse(url).buildUpon().appendQueryParameter("_fm_reload", String.valueOf(System.currentTimeMillis()));
            if (restore) builder.appendQueryParameter("_fm_restore", "1");
            return builder.build().toString();
        } catch (Throwable e) {
            return url + (url.contains("?") ? "&" : "?") + "_fm_reload=" + System.currentTimeMillis() + (restore ? "&_fm_restore=1" : "");
        }
    }

    private String getHomePage(Site site) {
        String url = site.getHomePage();
        if (UrlUtil.scheme(url).isEmpty()) url = UrlUtil.resolve(VodConfig.getUrl(), url);
        return UrlUtil.convert(url);
    }

    public interface Listener {

        void onWebLoading();

        void onWebReady();

        void onWebError();

        default void onExit() {
        }
    }
}
