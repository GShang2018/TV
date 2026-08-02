package com.fongmi.android.tv.player.exo;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;

import com.fongmi.android.tv.App;
import com.github.catvod.net.OkHttp;

import java.util.HashMap;
import java.util.Map;

public class MediaSourceFactory implements MediaSource.Factory {

    private final DefaultMediaSourceFactory defaultMediaSourceFactory;
    private HttpDataSource.Factory httpDataSourceFactory;
    private DataSource.Factory dataSourceFactory;
    private ExtractorsFactory extractorsFactory;

    public MediaSourceFactory() {
        defaultMediaSourceFactory = new DefaultMediaSourceFactory(getDataSourceFactory(), getExtractorsFactory());
    }

    @NonNull
    @Override
    public MediaSource.Factory setDrmSessionManagerProvider(@NonNull DrmSessionManagerProvider drmSessionManagerProvider) {
        return defaultMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider);
    }

    @NonNull
    @Override
    public MediaSource.Factory setLoadErrorHandlingPolicy(@NonNull LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        return defaultMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
    }

    @NonNull
    @Override
    public @C.ContentType int[] getSupportedTypes() {
        return defaultMediaSourceFactory.getSupportedTypes();
    }

    @NonNull
    @Override
    public MediaSource createMediaSource(@NonNull MediaItem mediaItem) {
        if (mediaItem.mediaId.contains("***") && mediaItem.mediaId.contains("|||")) {
            return createConcatenatingMediaSource(setHeader(mediaItem));
        } else if (mediaItem.mediaId.startsWith("file://")) {
            // file:// 协议直接使用 FileDataSource，绕过 CacheDataSource 避免缓存干扰
            return new DefaultMediaSourceFactory(new DefaultDataSource.Factory(App.get())).createMediaSource(setHeader(mediaItem));
        } else if (isLocalProxyUrl(mediaItem.mediaId)) {
            // 本地代理 (Thunder/BtEngine 磁力链接) 地址：迅雷 SDK 内部已管理磁盘缓存并实现边下边播，
            // 若再套一层 CacheDataSource 会与 SDK 的写入冲突且造成双份缓存。
            // 因此直接使用 HTTP 数据源，让播放器通过 Range 请求 (206) 从本地代理拉流。
            return new DefaultMediaSourceFactory(new DefaultDataSource.Factory(App.get(), getHttpDataSourceFactory())).createMediaSource(setHeader(mediaItem));
        } else {
            return defaultMediaSourceFactory.createMediaSource(setHeader(mediaItem));
        }
    }

    private boolean isLocalProxyUrl(String url) {
        return url != null && (url.startsWith("http://127.0.0.1") || url.startsWith("http://localhost"));
    }

    private MediaItem setHeader(MediaItem mediaItem) {
        Map<String, String> headers = new HashMap<>();
        for (String key : mediaItem.requestMetadata.extras.keySet()) headers.put(key, mediaItem.requestMetadata.extras.get(key).toString());
        getHttpDataSourceFactory().setDefaultRequestProperties(headers);
        return mediaItem;
    }

    private MediaSource createConcatenatingMediaSource(MediaItem mediaItem) {
        ConcatenatingMediaSource2.Builder builder = new ConcatenatingMediaSource2.Builder();
        for (String split : mediaItem.mediaId.split("\\*\\*\\*")) {
            String[] info = split.split("\\|\\|\\|");
            if (info.length >= 2) builder.add(defaultMediaSourceFactory.createMediaSource(mediaItem.buildUpon().setUri(Uri.parse(info[0])).build()), Long.parseLong(info[1]));
        }
        return builder.build();
    }

    private ExtractorsFactory getExtractorsFactory() {
        if (extractorsFactory == null) extractorsFactory = new DefaultExtractorsFactory().setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS).setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 10);
        return extractorsFactory;
    }

    private DataSource.Factory getDataSourceFactory() {
        if (dataSourceFactory == null) {
            DataSource.Factory baseFactory = buildReadOnlyCacheDataSource(new DefaultDataSource.Factory(App.get(), getHttpDataSourceFactory()));
            // 包装广告滤除数据源
            dataSourceFactory = new AdFilteringDataSourceFactory(baseFactory);
        }
        return dataSourceFactory;
    }

    private CacheDataSource.Factory buildReadOnlyCacheDataSource(DataSource.Factory upstreamFactory) {
        return new CacheDataSource.Factory().setCache(CacheManager.get().getCache()).setUpstreamDataSourceFactory(upstreamFactory).setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private HttpDataSource.Factory getHttpDataSourceFactory() {
        if (httpDataSourceFactory == null) httpDataSourceFactory = new OkHttpDataSource.Factory(OkHttp.client());
        return httpDataSourceFactory;
    }
}
