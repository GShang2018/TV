package com.fongmi.android.tv.player.exo;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.fongmi.android.tv.Setting;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 广告滤除数据源。
 * 包装真实的 {@link DataSource}，在读取 HLS 播放清单（.m3u8）时，
 * 拦截响应内容并通过 {@link HlsAdsParser} 滤除广告片段。
 * <p>
 * 支持外部 JSON 规则配置，通过 {@link Setting#getAdRulesUrl()} 获取规则 URL，
 * 规则中的 host 匹配 + 正则表达式用于精准过滤广告片段。
 */
@UnstableApi
public final class AdFilteringDataSource implements DataSource {

    private static final String TAG = "AdFilteringDataSource";

    private final DataSource upstream;
    private byte[] filteredData;
    private int position;

    /** 缓存的广告规则实例 */
    private static AdRule cachedAdRule;
    /** 上次加载规则时的 URL，用于检测 URL 变化 */
    private static String lastAdRulesUrl;

    public AdFilteringDataSource(DataSource upstream) {
        this.upstream = upstream;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        upstream.addTransferListener(transferListener);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        long result = upstream.open(dataSpec);
        // 检查是否启用广告滤除且为 M3U8 播放清单
        if (Setting.isRemoveAd() && isHlsPlaylist(dataSpec)) {
            try {
                // 读取完整响应内容
                byte[] originalData = readAllBytes(result);
                String originalContent = new String(originalData, StandardCharsets.UTF_8);

                // 获取 M3U8 URL 和外部规则
                String m3u8Url = dataSpec.uri != null ? dataSpec.uri.toString() : null;
                AdRule adRule = getAdRule();

                // 应用广告滤除（传入 URL 和外部规则）
                String filteredContent = HlsAdsParser.process(originalContent, m3u8Url, adRule);
                filteredData = filteredContent.getBytes(StandardCharsets.UTF_8);
                position = 0;
                Log.d(TAG, "M3U8 播放清單廣告濾除完成，大小: " + originalData.length + " -> " + filteredData.length + " 字節");
                return filteredData.length;
            } catch (Exception e) {
                Log.e(TAG, "廣告濾除處理失敗，使用原始數據", e);
                // 如果滤除失败，使用原始数据
                filteredData = null;
                return result;
            }
        }
        filteredData = null;
        return result;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (filteredData != null) {
            // 从过滤后的数据中读取
            int bytesAvailable = filteredData.length - position;
            int bytesToRead = Math.min(bytesAvailable, readLength);
            if (bytesToRead <= 0) {
                return -1; // 结束
            }
            System.arraycopy(filteredData, position, buffer, offset, bytesToRead);
            position += bytesToRead;
            return bytesToRead;
        }
        // 非 M3U8 内容或未启用滤除，直接委托
        return upstream.read(buffer, offset, readLength);
    }

    @Override
    @Nullable
    public Uri getUri() {
        return upstream.getUri();
    }

    @Override
    public void close() throws IOException {
        filteredData = null;
        position = 0;
        upstream.close();
    }

    /**
     * 判断是否为 HLS 播放清单请求。
     * 通过 URL 路径判断。
     */
    private static boolean isHlsPlaylist(DataSpec dataSpec) {
        if (dataSpec == null || dataSpec.uri == null) {
            return false;
        }
        String path = dataSpec.uri.getPath();
        if (path == null) {
            return false;
        }
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".m3u8") || lowerPath.endsWith(".m3u");
    }

    /**
     * 获取广告规则实例。
     * 从 {@link Setting#getAdRulesUrl()} 获取规则 URL 并加载解析。
     * 规则会被缓存，仅在 URL 变化时重新加载。
     */
    private static AdRule getAdRule() {
        String url = Setting.getAdRulesUrl();
        if (TextUtils.isEmpty(url)) {
            cachedAdRule = null;
            lastAdRulesUrl = null;
            return null;
        }

        // 如果 URL 未变化且已有缓存，直接返回
        if (cachedAdRule != null && url.equals(lastAdRulesUrl)) {
            return cachedAdRule;
        }

        // 加载新规则
        try {
            Log.d(TAG, "正在加載廣告規則: " + url);
            String json = OkHttp.newCall(url).execute().body().string();
            if (!TextUtils.isEmpty(json)) {
                cachedAdRule = AdRule.from(json);
                lastAdRulesUrl = url;
                Log.d(TAG, "廣告規則加載成功，共 " + cachedAdRule.getRules().size() + " 條規則");
                return cachedAdRule;
            }
        } catch (Exception e) {
            Log.e(TAG, "加載廣告規則失敗: " + url, e);
        }

        // 加载失败时，如果之前有缓存则保留，否则返回 null
        if (cachedAdRule == null) {
            lastAdRulesUrl = null;
        }
        return cachedAdRule;
    }

    /**
     * 从数据源读取所有字节。
     */
    private byte[] readAllBytes(long length) throws IOException {
        int bufferSize;
        if (length > 0 && length < Integer.MAX_VALUE) {
            bufferSize = (int) length;
        } else {
            bufferSize = 8192;
        }

        byte[] buffer = new byte[bufferSize];
        int totalBytesRead = 0;
        int bytesRead;

        while ((bytesRead = upstream.read(buffer, totalBytesRead, buffer.length - totalBytesRead)) != -1) {
            totalBytesRead += bytesRead;
            // 如果缓冲区满了，扩容
            if (totalBytesRead == buffer.length) {
                byte[] newBuffer = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }
        }

        // 截取实际读取的字节数
        byte[] result = new byte[totalBytesRead];
        System.arraycopy(buffer, 0, result, 0, totalBytesRead);
        return result;
    }
}
