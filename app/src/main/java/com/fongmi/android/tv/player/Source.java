package com.fongmi.android.tv.player;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.player.extractor.BtEngine;
import com.fongmi.android.tv.player.extractor.Force;
import com.fongmi.android.tv.player.extractor.JianPian;
import com.fongmi.android.tv.player.extractor.Proxy;
import com.fongmi.android.tv.player.extractor.Push;
import com.fongmi.android.tv.player.extractor.TVBus;
import com.fongmi.android.tv.player.extractor.Thunder;
import com.fongmi.android.tv.player.extractor.Video;
import com.fongmi.android.tv.player.extractor.Youtube;
import com.fongmi.android.tv.player.extractor.ZLive;
import com.fongmi.android.tv.utils.UrlUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Source {

    private final List<Extractor> extractors;

    private static class Loader {
        static volatile Source INSTANCE = new Source();
    }

    public static Source get() {
        return Loader.INSTANCE;
    }

    public Source() {
        extractors = new ArrayList<>();
        extractors.add(new Force());
        extractors.add(new JianPian());
        extractors.add(new Proxy());
        extractors.add(new Push());
        // Thunder 优先：迅雷 SDK 通过本地 HTTP 代理 (Range 206) 实现真正的边下边播，
        // 无需等文件下载完即可开始播放。BtEngine (libtorrent4j) 作为备选引擎。
        extractors.add(new Thunder());
        extractors.add(new BtEngine());
        extractors.add(new TVBus());
        extractors.add(new Video());
        extractors.add(new Youtube());
        extractors.add(new ZLive());
    }

    private Extractor getExtractor(String url) {
        String host = UrlUtil.host(url);
        String scheme = UrlUtil.scheme(url);
        for (Extractor extractor : extractors) if (extractor.match(scheme, host)) return extractor;
        return null;
    }

    private void addCallable(Iterator<Episode> iterator, List<Callable<List<Episode>>> items) {
        String url = iterator.next().getUrl();
        if (Thunder.Parser.match(url)) {
            items.add(Thunder.Parser.get(url));
            iterator.remove();
        } else if (BtEngine.Parser.match(url)) {
            items.add(() -> BtEngine.Parser.parse(url));
            iterator.remove();
        } else if (Youtube.Parser.match(url)) {
            items.add(Youtube.Parser.get(url));
            iterator.remove();
        }
    }

    public void parse(List<Flag> flags) throws Exception {
        for (Flag flag : flags) {
            ExecutorService executor = Executors.newFixedThreadPool(Constant.THREAD_POOL);
            List<Callable<List<Episode>>> items = new ArrayList<>();
            Iterator<Episode> iterator = flag.getEpisodes().iterator();
            while (iterator.hasNext()) addCallable(iterator, items);
            for (Future<List<Episode>> future : executor.invokeAll(items, 30, TimeUnit.SECONDS)) flag.getEpisodes().addAll(future.get());
            executor.shutdownNow();
        }
    }

    public String fetch(Result result) throws Exception {
        String url = result.getUrl().v();
        Extractor extractor = getExtractor(url);
        if (extractor != null) result.setParse(0);
        if (extractor instanceof Video) result.setParse(1);
        if (extractor instanceof Thunder || extractor instanceof BtEngine) {
            try {
                String fetchedUrl = extractor.fetch(url);
                if (fetchedUrl != null && !fetchedUrl.equals(url)) {
                    result.setParse(0);
                } else {
                    result.setParse(1);
                }
                return fetchedUrl;
            } catch (Exception e) {
                // 首选引擎失败时，尝试另一个磁力引擎 (Thunder <-> BtEngine 互为备选)
                for (Extractor ex : extractors) {
                    if (ex == extractor) continue;
                    if (ex instanceof Thunder || ex instanceof BtEngine) {
                        try {
                            String altUrl = ex.fetch(url);
                            if (altUrl != null && !altUrl.equals(url)) {
                                result.setParse(0);
                                return altUrl;
                            }
                        } catch (Exception e2) {
                            // ignore
                        }
                        break;
                    }
                }
                result.setParse(1);
                return url;
            }
        }
        return extractor == null ? url : extractor.fetch(url);
    }

    public String fetch(Channel channel) throws Exception {
        String url = channel.getCurrent().split("\\$")[0];
        Extractor extractor = getExtractor(url);
        if (extractor != null) channel.setParse(0);
        if (extractor instanceof Video) channel.setParse(1);
        return extractor == null ? url : extractor.fetch(url);
    }

    public void stop() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.stop();
    }

    public void exit() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.exit();
    }

    public interface Extractor {

        boolean match(String scheme, String host);

        String fetch(String url) throws Exception;

        void stop();

        void exit();
    }
}
