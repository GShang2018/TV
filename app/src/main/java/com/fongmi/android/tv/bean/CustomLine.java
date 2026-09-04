package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义线路：名称/地址可自行编辑，并各自拥有一份手动添加的站点列表。
 * 存储文件：TV/custom_lines.json，格式：{"lines": [{id, name, url, sites: [...]}]}
 * 线路访问 url 形如 custom://&lt;id&gt;，站点列表保存在该线路内部。
 */
public class CustomLine {

    private static final String FILE_NAME = "custom_lines.json";
    public static final String PREFIX = "custom://";

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("sites")
    private List<CustomSite> sites;

    public static File getFile() {
        return new File(Path.tv(), FILE_NAME);
    }

    public static List<CustomLine> getAll() {
        try {
            File file = getFile();
            if (!file.exists()) return Collections.emptyList();
            JsonObject object = Json.parse(Path.read(file)).getAsJsonObject();
            List<CustomLine> items = new ArrayList<>();
            for (JsonElement element : Json.safeListElement(object, "lines")) {
                CustomLine item = App.gson().fromJson(element, CustomLine.class);
                if (item != null && !item.getId().isEmpty()) items.add(item);
            }
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static CustomLine find(String id) {
        for (CustomLine item : getAll()) if (item.getId().equals(id)) return item;
        return null;
    }

    public static void saveAll(List<CustomLine> items) {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        for (CustomLine item : items) array.add(App.gson().toJsonTree(item));
        object.add("lines", array);
        Path.write(getFile(), object.toString().getBytes());
    }

    public static String getLineId(String url) {
        if (TextUtils.isEmpty(url)) return "";
        return url.startsWith(PREFIX) ? url.substring(PREFIX.length()) : "";
    }

    public static String getUrl(String id) {
        return PREFIX + id;
    }

    public String getId() {
        return id == null ? "" : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url == null ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<CustomSite> getSites() {
        return sites == null ? Collections.emptyList() : sites;
    }

    public void setSites(List<CustomSite> sites) {
        this.sites = sites;
    }

    public List<CustomSite> sites() {
        return new ArrayList<>(getSites());
    }

    public CustomLine sites(List<CustomSite> sites) {
        setSites(sites);
        return this;
    }

    public List<CustomSite> getEnabledSites() {
        List<CustomSite> items = new ArrayList<>();
        for (CustomSite item : getSites()) if (item.getEnabled()) items.add(item);
        return items;
    }

    public boolean isEmpty() {
        return getId().isEmpty();
    }

    public void save() {
        List<CustomLine> items = new ArrayList<>(getAll());
        items.remove(this);
        items.add(this);
        saveAll(items);
    }

    public void delete() {
        List<CustomLine> items = new ArrayList<>(getAll());
        items.remove(this);
        saveAll(items);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CustomLine)) return false;
        CustomLine it = (CustomLine) obj;
        return getId().equals(it.getId());
    }
}
