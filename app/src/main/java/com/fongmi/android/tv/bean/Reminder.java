package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.fongmi.android.tv.db.AppDatabase;

@Entity(tableName = "Reminder", primaryKeys = {"channelName", "startTime"})
public class Reminder {

    @NonNull
    private String channelName;
    private String groupName;
    private String programTitle;
    private long startTime;
    private long createTime;

    public static Reminder find(String channel, long startTime) {
        return AppDatabase.get().getReminderDao().find(channel, startTime);
    }

    public static boolean exist(String channel, long startTime) {
        return find(channel, startTime) != null;
    }

    public String getChannelName() {
        return channelName == null ? "" : channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getGroupName() {
        return groupName == null ? "" : groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getProgramTitle() {
        return programTitle == null ? "" : programTitle;
    }

    public void setProgramTitle(String programTitle) {
        this.programTitle = programTitle;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
