package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.CustomVod;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.github.catvod.utils.Path;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "我的"影视数据导出导入工具类。
 * 导出为 JSON 文件，包含版本号字段，为后续 WebDAV 同步预留扩展空间。
 */
public class MineBackup {

    public static final String SUFFIX = "mine.json";
    public static final int VERSION = 1;

    /**
     * 备份数据模型。version 字段用于后续格式兼容判断。
     */
    public static class Data {

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("vods")
        public List<CustomVod> vods = new ArrayList<>();
    }

    /**
     * 导出全部自定义影视到 JSON 文件。
     *
     * @return 导出的文件，失败返回 null
     */
    public static File export() {
        try {
            Data data = new Data();
            data.vods = CustomVod.getAll();
            String json = App.gson().toJson(data);
            String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            File file = new File(Path.tv(), "mine_" + time + "." + SUFFIX);
            Path.write(file, json.getBytes("UTF-8"));
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从 JSON 文件导入，按 vod_id 去重合并（相同 vod_id 更新，缺失新增）。
     *
     * @param file 备份文件
     * @return 是否成功
     */
    public static boolean importFile(File file) {
        try {
            String json = Path.read(file);
            if (json == null || json.isEmpty()) return false;
            return importJson(json);
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
        // 本地按 vod_id 建索引（仅 vod_id 非空的参与去重）
        Map<String, CustomVod> local = new HashMap<>();
        for (CustomVod vod : CustomVod.getAll()) {
            if (!vod.getVodId().isEmpty()) local.put(vod.getVodId(), vod);
        }
        List<CustomVod> items = new ArrayList<>();
        for (CustomVod source : data.vods) {
            if (source == null || source.getVodName().isEmpty()) continue;
            CustomVod target = local.get(source.getVodId());
            if (target == null) {
                // 新增：重置内部主键，保留录入的 vod_id
                source.setId(0);
                items.add(source);
            } else {
                // 更新：保留本地主键与时间戳，覆盖其它字段
                int id = target.getId();
                long createTime = target.getCreateTime();
                source.setId(id);
                source.setCreateTime(createTime);
                items.add(source);
            }
        }
        if (!items.isEmpty()) {
            AppDatabase.get().getCustomVodDao().insertOrUpdate(items);
            RefreshEvent.mine();
        }
    }
}
