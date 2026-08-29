package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.LiveHistory;

import java.util.List;

@Dao
public abstract class LiveHistoryDao extends BaseDao<LiveHistory> {

	@Query("SELECT * FROM LiveHistory ORDER BY createTime DESC")
	public abstract List<LiveHistory> findAll();

	@Query("DELETE FROM LiveHistory WHERE channelName = :channelName")
	public abstract void delete(String channelName);

	@Query("DELETE FROM LiveHistory")
	public abstract void deleteAll();
}
