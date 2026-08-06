package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.db.AppDatabase;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Entity(tableName = "KeepFolder")
public class KeepFolder {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("createTime")
    private long createTime;

    public static List<KeepFolder> arrayFrom(String str) {
        Type listType = new TypeToken<List<KeepFolder>>() {}.getType();
        List<KeepFolder> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public KeepFolder() {
    }

    public KeepFolder(String name) {
        this.name = name;
        this.createTime = System.currentTimeMillis();
    }

    @NonNull
    public int getId() {
        return id;
    }

    public void setId(@NonNull int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public static List<KeepFolder> getAll() {
        return AppDatabase.get().getKeepFolderDao().getAll();
    }

    public static KeepFolder find(int id) {
        return AppDatabase.get().getKeepFolderDao().find(id);
    }

    public static KeepFolder findByName(String name) {
        for (KeepFolder folder : getAll()) {
            if (folder.getName() != null && folder.getName().equals(name)) return folder;
        }
        return null;
    }

    public void save() {
        AppDatabase.get().getKeepFolderDao().insertOrUpdate(this);
    }

    public KeepFolder delete() {
        AppDatabase.get().getKeepFolderDao().delete(id);
        return this;
    }

    public int getCount() {
        return AppDatabase.get().getKeepDao().getFolderCount(id);
    }
}
