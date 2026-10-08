package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.bean.Result;

public class Downloader {

    private Result result;
    private Activity activity;
    private String title;
    private String image;

    private static class Loader {
        static volatile Downloader INSTANCE = new Downloader();
    }

    public static Downloader get() {
        return Loader.INSTANCE;
    }

    public Downloader title(String title) {
        this.title = title;
        return this;
    }

    public Downloader image(String image) {
        this.image = image;
        return this;
    }

    public Downloader result(Result result) {
        this.result = result;
        return this;
    }

    public void start(Activity activity) {
        this.activity = activity;
        if (result.hasMsg()) {
            Notify.show(result.getMsg());
        } else {
            download();
        }
    }

    private void download() {
        if (Setting.isBuiltinDownload()) builtin();
        else external();
    }

    private void builtin() {
        String url = UrlUtil.fixDownloadUrl(result.getRealUrl());
        if (TextUtils.isEmpty(url)) {
            Notify.show(R.string.download_url_empty);
            return;
        }
        String header = App.gson().toJson(result.getHeaders());
        String fileName = buildFileName(url);
        Download item = new Download(title, image, url, header, fileName, result.getKey(), result.getFlag());
        DownloadManager.get().enqueue(item);
        Notify.show(R.string.download_started);
    }

    private void external() {
        boolean ok = IDMUtil.downloadFile(activity, UrlUtil.fixDownloadUrl(result.getRealUrl()), title, result.getHeaders(), false, false);
        if (!ok) Notify.show(R.string.download_1dm_missing);
    }

    private String buildFileName(String url) {
        String name = TextUtils.isEmpty(title) ? "video" : title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return name + extension(url);
    }

    private String extension(String url) {
        try {
            String path = Uri.parse(url).getPath();
            if (path == null) return ".mp4";
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && path.length() - dot <= 5) return path.substring(dot);
        } catch (Exception ignored) {
        }
        return ".mp4";
    }
}
