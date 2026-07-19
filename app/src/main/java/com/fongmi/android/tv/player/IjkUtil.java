package com.fongmi.android.tv.player;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.player.exo.HlsAdsParser;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;
import tv.danmaku.ijk.media.player.MediaSource;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class IjkUtil {

    private static final String TAG = "IjkUtil";

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getRealUrl());
    }

    public static MediaSource getSource(Channel channel) {
        return getSource(channel.getHeaders(), channel.getUrl());
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        Uri uri = UrlUtil.uri(url);
        // IJK 播放器广告滤除：如果是 M3U8 且启用了广告滤除，预先下载并滤除广告
        if (Setting.isRemoveAd() && isHlsUrl(url)) {
            try {
                uri = filterHlsAd(uri, headers);
            } catch (Exception e) {
                Log.e(TAG, "IJK 广告滤除失败，使用原始 URL", e);
            }
        }
        return new MediaSource(Players.checkUa(headers), uri);
    }

    private static boolean isHlsUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        String lower = url.toLowerCase();
        return lower.contains(".m3u8") || lower.contains(".m3u");
    }

    /**
     * 下载 M3U8 内容，滤除广告后通过 data URI 返回，避免磁盘写入。
     * 仅当实际滤除了广告片段时才使用 data URI，否则返回原始 URI。
     */
    private static Uri filterHlsAd(Uri uri, Map<String, String> headers) throws IOException {
        String url = uri.toString();
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!TextUtils.isEmpty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue())) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }
        }
        Response response = OkHttp.client().newCall(builder.build()).execute();
        if (!response.isSuccessful() || response.body() == null) return uri;
        String content = response.body().string();

        // 滤除广告
        String filtered = HlsAdsParser.process(content, url, null);
        if (TextUtils.isEmpty(filtered) || filtered.equals(content)) return uri;

        // 使用 data URI 内联传递滤除后的内容，避免写临时文件
        String encoded = Uri.encode(filtered);
        Log.d(TAG, "IJK 广告滤除完成，大小: " + content.length() + " -> " + filtered.length() + " 字节");
        return Uri.parse("data:text/plain;charset=utf-8," + encoded);
    }

    public static void setSubtitleView(IjkVideoView ijk) {
        ijk.getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        ijk.getSubtitleView().setApplyEmbeddedFontSizes(false);
        ijk.getSubtitleView().setApplyEmbeddedStyles(!Setting.isCaption());
        if (Setting.getSubtitleTextSize() != 0) ijk.getSubtitleView().setFractionalTextSize(Setting.getSubtitleTextSize());
        if (Setting.getSubtitleBottomPadding() != 0) ijk.getSubtitleView().setBottomPaddingFraction(Setting.getSubtitleBottomPadding());
    }
}
