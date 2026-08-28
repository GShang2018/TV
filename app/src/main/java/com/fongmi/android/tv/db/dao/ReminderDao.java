package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.Reminder;

import java.util.List;

@Dao
public abstract class ReminderDao extends BaseDao<Reminder> {

    @Query("SELECT * FROM Reminder WHERE channelName = :channel AND startTime = :startTime LIMIT 1")
    public abstract Reminder find(String channel, long startTime);

    @Query("SELECT * FROM Reminder ORDER BY startTime")
    public abstract List<Reminder> getAll();

    @Query("DELETE FROM Reminder WHERE channelName = :channel AND startTime = :startTime")
    public abstract void delete(String channel, long startTime);

    @Query("DELETE FROM Reminder WHERE startTime < :time")
    public abstract void deleteExpired(long time);
}
