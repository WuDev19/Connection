package com.example.connection.constants;

import android.content.Context;
import android.util.Log;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheWriter;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** One disk cache per process; each visible feed owns its cancellable prefetch task. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class VideoCache {
    private static SimpleCache cache;
    private static final long CACHE_BYTES = 100L * 1024 * 1024;
    private static final long PRELOAD_BYTES = 1024 * 1024;

    private static synchronized SimpleCache getCache(Context context) {
        if (cache == null) {
            Context app = context.getApplicationContext();
            cache = new SimpleCache(new File(app.getCacheDir(), "feed_video"),
                    new LeastRecentlyUsedCacheEvictor(CACHE_BYTES), new StandaloneDatabaseProvider(app));
        }
        return cache;
    }

    public static CacheDataSource.Factory dataSourceFactory(Context context) {
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(5000).setReadTimeoutMs(5000);
        return new CacheDataSource.Factory().setCache(getCache(context))
                .setUpstreamDataSourceFactory(new DefaultDataSource.Factory(context.getApplicationContext(), http))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    public static final class Preloader {
        private final CacheDataSource.Factory factory;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private CacheWriter writer;
        private Future<?> task;
        private String currentUrl;

        public Preloader(Context context) { factory = dataSourceFactory(context); }

        public void preload(String url) {
            if (url != null && url.equals(currentUrl)) return;
            cancel();
            if (url == null || url.isEmpty() || executor.isShutdown()) return;
            currentUrl = url;
            CacheWriter next = new CacheWriter(factory.createDataSource(),
                    new DataSpec.Builder().setUri(url).setLength(PRELOAD_BYTES).build(), null, null);
            writer = next;
            task = executor.submit(() -> {
                try {
                    next.cache();
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) Log.d("VideoCache", "Preload skipped: " + e.getMessage());
                }
            });
        }

        public void cancel() {
            if (writer != null) writer.cancel();
            if (task != null) task.cancel(true);
            writer = null;
            task = null;
            currentUrl = null;
        }

        public void release() {
            cancel();
            executor.shutdownNow();
        }
    }

    private VideoCache() {}
}
