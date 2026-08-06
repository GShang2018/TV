package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.github.catvod.utils.Path;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 收藏夹导入导出工具类。
 * 导出为 JSON 文件，包含版本号字段，为后续 WebDAV 同步预留扩展空间。
 */
public class KeepBackup {

    public static final String SUFFIX = "keep.json";
    public static final int VERSION = 1;

    /**
     * 备份数据模型。version 字段用于后续 WebDAV 同步时的格式兼容判断。
     */
    public static class Data {

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("folders")
        public List<Folder> folders = new ArrayList<>();

        @SerializedName("defaultKeeps")
        public List<Keep> defaultKeeps = new ArrayList<>();
    }

    public static class Folder {

        @SerializedName("name")
        public String name;

        @SerializedName("keeps")
        public List<Keep> keeps = new ArrayList<>();
    }

    /**
     * 导出所有收藏夹（含默认收藏夹）到 JSON 文件。
     *
     * @return 导出的文件，失败返回 null
     */
    public static File export() {
        try {
            Data data = new Data();
            // 默认收藏夹（folderId = 0）
            data.defaultKeeps = Keep.getVod(0);
            // 自定义收藏夹
            for (KeepFolder folder : KeepFolder.getAll()) {
                Folder f = new Folder();
                f.name = folder.getName();
                f.keeps = Keep.getVod(folder.getId());
                data.folders.add(f);
            }
            String json = App.gson().toJson(data);
            String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            File file = new File(Path.tv(), "keep_" + time + "." + SUFFIX);
            Path.write(file, json.getBytes("UTF-8"));
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从 JSON 文件导入收藏夹，合并到本地（按 key 去重，不覆盖已有收藏）。
     *
     * @param file 备份文件
     * @return 是否成功
     */
    public static boolean importFile(File file) {
        try {
            String json = Path.read(file);
            if (json == null || json.isEmpty()) return false;
            Data data = App.gson().fromJson(json, Data.class);
            if (data == null) return false;
            importData(data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从 JSON 字符串导入（预留 WebDAV 使用）。
     */
    public static boolean importJson(String json) {
        try {
            if (json == null || json.isEmpty()) return false;
            Data data = App.gson().fromJson(json, Data.class);
            if (data == null) return false;
            importData(data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void importData(Data data) {
        // 导入默认收藏夹
        if (data.defaultKeeps != null) {
            for (Keep keep : data.defaultKeeps) {
                keep.setFolderId(0);
                if (Keep.find(keep.getCid(), keep.getKey()) == null) keep.save();
            }
        }
        // 导入自定义收藏夹
        if (data.folders != null) {
            for (Folder folder : data.folders) {
                if (folder.name == null || folder.name.isEmpty()) continue;
                KeepFolder keepFolder = findByName(folder.name);
                if (keepFolder == null) {
                    keepFolder = new KeepFolder(folder.name);
                    keepFolder.save();
                }
                for (Keep keep : folder.keeps) {
                    keep.setFolderId(keepFolder.getId());
                    if (Keep.find(keep.getCid(), keep.getKey()) == null) keep.save();
                }
            }
        }
        RefreshEvent.keep();
    }

    private static KeepFolder findByName(String name) {
        for (KeepFolder folder : KeepFolder.getAll()) {
            if (folder.getName() != null && folder.getName().equals(name)) return folder;
        }
        return null;
    }

    /**
     * 删除收藏夹及其所有收藏。
     */
    public static void deleteFolder(KeepFolder folder) {
        AppDatabase.get().getKeepDao().deleteFolder(folder.getId());
        folder.delete();
    }
}
