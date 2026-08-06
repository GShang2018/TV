package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.KeepFolder;

import java.util.List;

@Dao
public abstract class KeepFolderDao extends BaseDao<KeepFolder> {

    @Query("SELECT * FROM KeepFolder ORDER BY createTime DESC")
    public abstract List<KeepFolder> getAll();

    @Query("SELECT * FROM KeepFolder WHERE id = :id")
    public abstract KeepFolder find(int id);

    @Query("DELETE FROM KeepFolder WHERE id = :id")
    public abstract void delete(int id);
}
