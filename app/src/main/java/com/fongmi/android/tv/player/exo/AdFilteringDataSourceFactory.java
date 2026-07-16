package com.fongmi.android.tv.player.exo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

/**
 * 广告滤除数据源工厂。
 * 包装原始的 {@link DataSource.Factory}，创建 {@link AdFilteringDataSource}。
 */
@UnstableApi
public final class AdFilteringDataSourceFactory implements DataSource.Factory {

    private final DataSource.Factory upstreamFactory;

    public AdFilteringDataSourceFactory(DataSource.Factory upstreamFactory) {
        this.upstreamFactory = upstreamFactory;
    }

    @NonNull
    @Override
    public DataSource createDataSource() {
        return new AdFilteringDataSource(upstreamFactory.createDataSource());
    }
}