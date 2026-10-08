package com.fongmi.android.tv.event;

import com.fongmi.android.tv.bean.Download;

import org.greenrobot.eventbus.EventBus;

public class DownloadEvent {

    private final Download item;
    private final Action action;

    public enum Action {
        ADD, UPDATE, DELETE
    }

    public static void add(Download item) {
        EventBus.getDefault().post(new DownloadEvent(item, Action.ADD));
    }

    public static void update(Download item) {
        EventBus.getDefault().post(new DownloadEvent(item, Action.UPDATE));
    }

    public static void delete(Download item) {
        EventBus.getDefault().post(new DownloadEvent(item, Action.DELETE));
    }

    private DownloadEvent(Download item, Action action) {
        this.item = item;
        this.action = action;
    }

    public Download getItem() {
        return item;
    }

    public Action getAction() {
        return action;
    }
}
