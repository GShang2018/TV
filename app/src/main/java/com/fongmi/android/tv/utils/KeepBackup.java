package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

        @SerializedName("configs")
        public List<Config> configs = new ArrayList<>();

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
            // 收集收藏所依赖的配置，跨设备导入时自动补齐
            data.configs = collectConfigs(data);
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
     * 收集所有收藏用到的配置（去重），用于跨设备导入时自动补齐缺失配置。
     */
    private static List<Config> collectConfigs(Data data) {
        List<Config> configs = new ArrayList<>();
        Map<Integer, Config> map = new HashMap<>();
        collectConfigs(data.defaultKeeps, map);
        if (data.folders != null) {
            for (Folder folder : data.folders) {
                collectConfigs(folder.keeps, map);
            }
        }
        configs.addAll(map.values());
        return configs;
    }

    private static void collectConfigs(List<Keep> keeps, Map<Integer, Config> map) {
        if (keeps == null) return;
        for (Keep keep : keeps) {
            Config config = Config.find(keep.getCid());
            if (config != null && !config.isEmpty()) map.putIfAbsent(config.getId(), config);
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
        // 先导入备份中携带的配置，本机缺失的会自动补齐，保证跨设备可播放
        importConfigs(data.configs);
        // 构建站点 key -> 本地配置 id 的映射，用于跨设备导入时重映射 cid
        Map<String, Integer> siteConfigMap = buildSiteConfigMap();
        // 导入默认收藏夹
        if (data.defaultKeeps != null) {
            for (Keep keep : data.defaultKeeps) {
                remapCid(keep, siteConfigMap);
                keep.setFolderId(0);
                AppDatabase.get().getKeepDao().insertOrUpdate(keep);
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
                    remapCid(keep, siteConfigMap);
                    keep.setFolderId(keepFolder.getId());
                    AppDatabase.get().getKeepDao().insertOrUpdate(keep);
                }
            }
        }
        RefreshEvent.keep();
    }

    /**
     * 导入备份携带的配置。按 url+type 匹配本机配置，缺失的自动创建，
     * 已存在的则补充 json/name 等信息，确保站点 key 能正确解析。
     */
    private static void importConfigs(List<Config> configs) {
        if (configs == null) return;
        for (Config config : configs) {
            if (config == null || config.isEmpty()) continue;
            try {
                // find(url, type) 在本机不存在时会自动 create + insert 并返回带新 id 的配置
                Config local = Config.find(config.getUrl(), config.getType());
                if (local.getJson() == null || local.getJson().isEmpty()) {
                    local.json(config.getJson()).name(config.getName()).logo(config.getLogo()).home(config.getHome()).parse(config.getParse()).save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 构建站点 key -> 本地配置 id 的映射。
     * 不同设备上 Config 的自增 id 可能不同，但站点 key 是稳定的标识，
     * 通过解析每个配置 JSON 中的 sites 数组，将站点 key 映射到本机配置 id。
     */
    private static Map<String, Integer> buildSiteConfigMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Config config : Config.getAll(0)) {
            String json = config.getJson();
            if (json == null || json.isEmpty()) continue;
            try {
                JsonObject object = App.gson().fromJson(json, JsonObject.class);
                if (object == null) continue;
                if (object.has("video")) object = object.getAsJsonObject("video");
                for (JsonElement element : Json.safeListElement(object, "sites")) {
                    if (element == null || !element.isJsonObject()) continue;
                    String key = Json.safeString(element.getAsJsonObject(), "key");
                    if (key != null && !key.isEmpty()) map.putIfAbsent(key, config.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * 根据站点 key 重映射收藏的 cid，使其指向本机对应的配置。
     * 若本机没有匹配的配置，则保留原 cid（点击时仍会回退到搜索页）。
     */
    private static void remapCid(Keep keep, Map<String, Integer> siteConfigMap) {
        try {
            String siteKey = keep.getSiteKey();
            if (siteKey == null || siteKey.isEmpty()) return;
            Integer cid = siteConfigMap.get(siteKey);
            if (cid != null) keep.setCid(cid);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
