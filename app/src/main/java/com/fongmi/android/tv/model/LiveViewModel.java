package com.fongmi.android.tv.model;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.EpgParser;
import com.fongmi.android.tv.api.LiveParser;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.github.catvod.net.OkHttp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveViewModel extends ViewModel {

    private static final int LIVE = 0;
    private static final int EPG = 1;
    private static final int URL = 2;
    private static final int XML = 3;

    private final SimpleDateFormat formatDate;
    private final SimpleDateFormat formatTime;

    public MutableLiveData<Channel> url;
    public MutableLiveData<Boolean> xml;
    public MutableLiveData<Live> live;
    public MutableLiveData<Epg> epg;

    private ExecutorService executor1;
    private ExecutorService executor2;
    private ExecutorService executor3;
    private ExecutorService executor4;
    private ExecutorService executor5;
    private ExecutorService executor6;

    public LiveViewModel() {
        this.formatTime = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault());
        this.formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.live = new MutableLiveData<>();
        this.epg = new MutableLiveData<>();
        this.url = new MutableLiveData<>();
        this.xml = new MutableLiveData<>();
    }

    public void getLive(Live item) {
        execute(LIVE, () -> {
            LiveParser.start(item.recent());
            setTimeZone(item.getEpg());
            verify(item);
            return item;
        });
    }

    public void getXml(Live item) {
        execute(XML, () -> EpgParser.start(item));
    }

    public void getEpg(Channel item) {
        getEpg(item, getEpgDate(), true);
    }

    // 切换日期拉取节目单：当天结果写回频道缓存，回切当天直接命中缓存不再重复请求（避免接口限流失败导致节目单丢失）；其余日期仅用于展示，不覆盖频道当天数据
    public void getEpg(Channel item, String date) {
        getEpg(item, date, date.equals(getEpgDate()));
    }

    // save=true 时写入频道数据（当天节目单），false 时仅返回结果用于展示
    private void getEpg(Channel item, String date, boolean save) {
        String url = item.getEpg().replace("{date}", date);
        execute(EPG, () -> {
            Epg data;
            if (item.getData().equal(date)) {
                data = item.getData();
            } else {
                data = Epg.objectFrom(OkHttp.string(url), item.getTvgName(), formatTime);
                // 以请求日期为准写回，避免响应日期与请求不一致导致缓存误判
                data.setDate(date);
                // 部分 EPG 源响应不含 date 字段，需以请求日期重新解析节目起止时间
                data.setTime(formatTime);
                // 各日期结果写入频道缓存，弹窗切换日期直接复用
                item.putData(data);
                if (save) item.setData(data);
            }
            // 页面加载(save)始终选中当前直播节目；弹窗展示时已有选中(如回看)则保留，否则选中当前直播节目
            if (save || data.getSelected() < 0) data.selected();
            return data;
        });
    }

    public String getEpgDate() {
        return formatDate.format(new Date());
    }

    // 频道列表页批量拉取 EPG：顺序执行，避免单个失败中断其余频道，结果逐个通过 epg 回调
    public void getEpgList(List<Channel> items) {
        getEpgList(items, null);
    }

    // 全部拉取完成后通过 done 回调通知（结果已写入频道数据，避免逐个回调因 postValue 合并而丢失）
    public void getEpgList(List<Channel> items, Runnable done) {
        if (executor5 != null) executor5.shutdownNow();
        executor5 = Executors.newSingleThreadExecutor();
        executor5.execute(() -> {
            String date = formatDate.format(new Date());
            for (Channel item : items) {
                if (Thread.interrupted()) return;
                try {
                    if (item.getEpg().isEmpty()) continue;
                    // 已有非空当天数据则跳过，避免重复拉取；空数据(拉取失败/无节目)需重试
                    if (item.getData().equal(date) && !item.getData().getList().isEmpty()) continue;
                    Epg data = Epg.objectFrom(OkHttp.string(item.getEpg().replace("{date}", date)), item.getTvgName(), formatTime);
                    // 拉取失败/空结果不写入缓存，避免污染导致后续跳过，永久无法加载
                    if (data.getList().isEmpty()) continue;
                    // 解析结果可能不含日期，补上当天日期便于 equal(date) 命中缓存，避免重复拉取
                    data.setDate(date);
                    // 部分 EPG 源响应不含 date 字段，需以请求日期重新解析节目起止时间
                    data.setTime(formatTime);
                    data.selected();
                    item.setData(data);
                    epg.postValue(item.getData());
                } catch (Throwable ignored) {
                }
            }
            if (done != null) App.post(done);
        });
    }

    // 弹窗预取：按传入顺序拉取全部日期节目单，结果写入频道各日期缓存并逐个回调；当天同步频道主数据
    public void getEpgDates(Channel item, List<String> dates) {
        if (executor6 != null) executor6.shutdownNow();
        executor6 = Executors.newSingleThreadExecutor();
        executor6.execute(() -> {
            String today = formatDate.format(new Date());
            for (String date : dates) {
                if (Thread.interrupted()) return;
                try {
                    Epg data = item.findData(date);
                    // 已有非空缓存则跳过，避免重复请求
                    if (data != null && !data.getList().isEmpty()) continue;
                    data = Epg.objectFrom(OkHttp.string(item.getEpg().replace("{date}", date)), item.getTvgName(), formatTime);
                    // 拉取失败/空结果不写入缓存，避免污染导致后续跳过
                    if (data.getList().isEmpty()) continue;
                    data.setDate(date);
                    data.setTime(formatTime);
                    item.putData(data);
                    if (date.equals(today)) {
                        data.selected();
                        item.setData(data);
                    }
                    epg.postValue(data);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public void getUrl(Channel item) {
        execute(URL, () -> {
            item.setMsg(null);
            Source.get().stop();
            item.setUrl(Source.get().fetch(item));
            return item;
        });
    }

    public void getUrl(Channel item, EpgData data) {
        execute(URL, () -> {
            item.setUrl(item.getCatchup().format(item.getCurrent(), data));
            return item;
        });
    }

    private void setTimeZone(String url) {
        try {
            if (!url.contains("serverTimeZone=")) return;
            TimeZone timeZone = TimeZone.getTimeZone(Uri.parse(url).getQueryParameter("serverTimeZone"));
            formatDate.setTimeZone(timeZone);
            formatTime.setTimeZone(timeZone);
        } catch (Exception ignored) {
        }
    }

    private void verify(Live item) {
        Iterator<Group> iterator = item.getGroups().iterator();
        while (iterator.hasNext()) if (iterator.next().isEmpty()) iterator.remove();
        if (item.getGroups().isEmpty() || item.getGroups().get(0).isKeep()) return;
        item.getGroups().add(0, Group.create(R.string.keep));
        LiveConfig.get().setKeep(item.getGroups());
    }

    private void execute(int type, Callable<?> callable) {
        switch (type) {
            case LIVE:
                if (executor1 != null) executor1.shutdownNow();
                executor1 = Executors.newFixedThreadPool(2);
                executor1.execute(runnable(type, callable, executor1));
                break;
            case EPG:
                if (executor2 != null) executor2.shutdownNow();
                executor2 = Executors.newFixedThreadPool(2);
                executor2.execute(runnable(type, callable, executor2));
                break;
            case URL:
                if (executor3 != null) executor3.shutdownNow();
                executor3 = Executors.newFixedThreadPool(2);
                executor3.execute(runnable(type, callable, executor3));
                break;
            case XML:
                if (executor4 != null) executor4.shutdownNow();
                executor4 = Executors.newFixedThreadPool(2);
                executor4.execute(runnable(type, callable, executor4));
                break;
        }
    }

    private Runnable runnable(int type, Callable<?> callable, ExecutorService executor) {
        return () -> {
            try {
                if (Thread.interrupted()) return;
                if (type == EPG) epg.postValue((Epg) executor.submit(callable).get(Constant.TIMEOUT_EPG, TimeUnit.MILLISECONDS));
                if (type == LIVE) live.postValue((Live) executor.submit(callable).get(Constant.TIMEOUT_LIVE, TimeUnit.MILLISECONDS));
                if (type == XML) xml.postValue((Boolean) executor.submit(callable).get(Constant.TIMEOUT_XML, TimeUnit.MILLISECONDS));
                if (type == URL) url.postValue((Channel) executor.submit(callable).get(Constant.TIMEOUT_PARSE_LIVE, TimeUnit.MILLISECONDS));
            } catch (Throwable e) {
                if (e instanceof InterruptedException || Thread.interrupted()) return;
                if (e.getCause() instanceof ExtractException) url.postValue(Channel.error(e.getCause().getMessage()));
                else if (type == URL) url.postValue(new Channel());
                if (type == LIVE) live.postValue(new Live());
                if (type == EPG) epg.postValue(new Epg());
                if (type == XML) xml.postValue(false);
                e.printStackTrace();
            }
        };
    }

    @Override
    protected void onCleared() {
        if (executor1 != null) executor1.shutdownNow();
        if (executor2 != null) executor2.shutdownNow();
        if (executor3 != null) executor3.shutdownNow();
        if (executor4 != null) executor4.shutdownNow();
        if (executor5 != null) executor5.shutdownNow();
        if (executor6 != null) executor6.shutdownNow();
    }
}
