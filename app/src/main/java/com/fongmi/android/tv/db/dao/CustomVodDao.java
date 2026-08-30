package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;

import com.fongmi.android.tv.bean.CustomVod;

import java.util.List;

@Dao
public abstract class CustomVodDao extends BaseDao<CustomVod> {

    @Query("SELECT * FROM CustomVod ORDER BY updateTime DESC")
    public abstract List<CustomVod> getAll();

    @Query("SELECT * FROM CustomVod WHERE id = :id LIMIT 1")
    public abstract CustomVod find(int id);

    @Query("SELECT COUNT(*) FROM CustomVod")
    public abstract int getCount();

    @Delete
    public abstract void delete(CustomVod item);

    @Query("DELETE FROM CustomVod")
    public abstract void deleteAll();
}
