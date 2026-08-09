package com.fongmi.android.tv.bean;

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
 * 自定义点播源，以本地 JSON 文件形式存储。
 * 文件路径：TV/custom.json，格式：{"sites": [{key, name, api, type, ...}]}
 */
public class CustomSite {

    private static final String FILE_NAME = "custom.json";

    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("api")
    private String api;

    @SerializedName("type")
    private Integer type;

    @SerializedName("searchable")
    private Integer searchable;

    @SerializedName("quickSearch")
    private Integer quickSearch;

    @SerializedName("filterable")
    private Integer filterable;

    @SerializedName("style")
    private Style style;

    public static File getFile() {
        return new File(Path.tv(), FILE_NAME);
    }

    public static List<CustomSite> getAll() {
        try {
            File file = getFile();
            if (!file.exists()) return Collections.emptyList();
            JsonObject object = Json.parse(Path.read(file)).getAsJsonObject();
            List<CustomSite> items = new ArrayList<>();
            for (JsonElement element : Json.safeListElement(object, "sites")) {
                CustomSite item = App.gson().fromJson(element, CustomSite.class);
                if (item != null && !item.getKey().isEmpty()) items.add(item);
            }
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static CustomSite find(String key) {
        for (CustomSite item : getAll()) if (item.getKey().equals(key)) return item;
        return null;
    }

    public static void saveAll(List<CustomSite> items) {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        for (CustomSite item : items) array.add(App.gson().toJsonTree(item));
        object.add("sites", array);
        Path.write(getFile(), object.toString().getBytes());
    }

    public String getKey() {
        return key == null ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApi() {
        return api == null ? "" : api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Integer getType() {
        return type == null ? 1 : type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getSearchable() {
        return searchable == null ? 1 : searchable;
    }

    public void setSearchable(Integer searchable) {
        this.searchable = searchable;
    }

    public Integer getQuickSearch() {
        return quickSearch == null ? 1 : quickSearch;
    }

    public void setQuickSearch(Integer quickSearch) {
        this.quickSearch = quickSearch;
    }

    public Integer getFilterable() {
        return filterable == null ? 1 : filterable;
    }

    public void setFilterable(Integer filterable) {
        this.filterable = filterable;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public Site toSite() {
        Site site = new Site();
        site.setKey(getKey());
        site.setName(getName());
        site.setApi(getApi());
        site.setType(getType());
        site.setSearchable(getSearchable());
        site.setQuickSearch(getQuickSearch());
        site.setFilterable(getFilterable());
        site.setStyle(getStyle());
        return site;
    }

    public void save() {
        List<CustomSite> items = new ArrayList<>(getAll());
        items.remove(this);
        items.add(this);
        saveAll(items);
    }

    public void delete() {
        List<CustomSite> items = new ArrayList<>(getAll());
        items.remove(this);
        saveAll(items);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CustomSite)) return false;
        CustomSite it = (CustomSite) obj;
        return getKey().equals(it.getKey());
    }
}
