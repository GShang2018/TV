package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.db.AppDatabase;

import java.util.List;

// 直播观看历史：按频道名去重，每次播放更新分组与时间
@Entity(tableName = "LiveHistory", primaryKeys = {"channelName"})
public class LiveHistory {

	@NonNull
	private String channelName;
	private String groupName;
	private String logo;
	private long createTime;

	public static LiveHistory create(Channel channel) {
		LiveHistory item = new LiveHistory();
		item.setChannelName(channel.getName());
		if (channel.getGroup() != null) item.setGroupName(channel.getGroup().getName());
		item.setLogo(channel.getLogo());
		item.setCreateTime(System.currentTimeMillis());
		return item;
	}

	public static List<LiveHistory> getAll() {
		return AppDatabase.get().getLiveHistoryDao().findAll();
	}

	public static void delete(String channelName) {
		AppDatabase.get().getLiveHistoryDao().delete(channelName);
	}

	public static void deleteAll() {
		AppDatabase.get().getLiveHistoryDao().deleteAll();
	}

	@NonNull
	public String getChannelName() {
		return channelName == null ? "" : channelName;
	}

	public void setChannelName(@NonNull String channelName) {
		this.channelName = channelName;
	}

	public String getGroupName() {
		return groupName == null ? "" : groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getLogo() {
		return logo == null ? "" : logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public void save() {
		App.execute(() -> AppDatabase.get().getLiveHistoryDao().insertOrUpdate(this));
	}
}
