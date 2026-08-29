package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class Epg {

    @SerializedName("key")
    private String key;
    @SerializedName("date")
    private String date;
    @SerializedName("epg_data")
    private List<EpgData> list;

    private int width;

    public static Epg objectFrom(String str, String key, SimpleDateFormat format) {
        try {
            Epg item = App.gson().fromJson(str, Epg.class);
            item.setTime(format);
            item.setKey(key);
            return item;
        } catch (Exception e) {
            return new Epg();
        }
    }

    public static Epg create(String key, String date) {
        Epg item = new Epg();
        item.setKey(key);
        item.setDate(date);
        item.setList(new ArrayList<>());
        return item;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDate() {
        return TextUtils.isEmpty(date) ? "" : date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<EpgData> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public void setList(List<EpgData> list) {
        this.list = list;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean equal(String date) {
        return getDate().equals(date);
    }

    // 以当前 date 解析节目起止时间；在外部 setDate(请求日期) 后调用可纠正响应不含日期字段导致的解析失败
    public void setTime(SimpleDateFormat format) {
        setList(new ArrayList<>(new LinkedHashSet<>(getList())));
        for (EpgData item : getList()) {
            item.setStartTime(Util.format(format, getDate().concat(item.getStart())));
            item.setEndTime(Util.format(format, getDate().concat(item.getEnd())));
            item.setTitle(Trans.s2t(item.getTitle()));
        }
        // 解析后做时间戳级去重 + 排序，修复部分源返回重复节目导致的当日节目循环重复
        dedupe();
    }

    // 部分 EPG 源（XML 多 id 映射同一频道、接口返回重复轮次等）会令同一天节目重复出现，
    // 按 (startTime, endTime) 去重并升序排序；时间解析失败的条目保留且排到末尾，避免误删
    public void dedupe() {
        if (getList().size() < 2) return;
        Map<String, EpgData> unique = new LinkedHashMap<>();
        int index = 0;
        for (EpgData item : getList()) {
            String key = item.getStartTime() > 0 ? "t" + item.getStartTime() + "_" + item.getEndTime() : "r" + index++;
            unique.putIfAbsent(key, item);
        }
        if (unique.size() == getList().size()) return;
        List<EpgData> sorted = new ArrayList<>(unique.values());
        sorted.sort((a, b) -> {
            if (a.getStartTime() <= 0 && b.getStartTime() <= 0) return 0;
            if (a.getStartTime() <= 0) return 1;
            if (b.getStartTime() <= 0) return -1;
            return Long.compare(a.getStartTime(), b.getStartTime());
        });
        setList(sorted);
    }

    public String getEpg() {
        for (EpgData item : getList()) if (item.isSelected()) return item.format();
        return "";
    }

    public Epg selected() {
        for (EpgData item : getList()) item.setSelected(item.isInRange());
        return this;
    }

    public int getSelected() {
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isSelected()) return i;
        return -1;
    }

    public int getInRange() {
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isInRange()) return i;
        return -1;
    }
}
