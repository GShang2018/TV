package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.db.AppDatabase;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义影视实体（"我的" Tab）。
 * 字段对齐 CMS（苹果CMS/MacCMS）vod_xx 标准命名，可直接在详情页/播放页使用。
 */
@Entity(tableName = "CustomVod")
public class CustomVod {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @SerializedName("vod_id")
    private String vodId;

    @SerializedName("vod_name")
    private String vodName;

    @SerializedName("type_name")
    private String typeName;

    @SerializedName("vod_pic")
    private String vodPic;

    @SerializedName("vod_remarks")
    private String vodRemarks;

    @SerializedName("vod_year")
    private String vodYear;

    @SerializedName("vod_area")
    private String vodArea;

    @SerializedName("vod_director")
    private String vodDirector;

    @SerializedName("vod_actor")
    private String vodActor;

    @SerializedName("vod_content")
    private String vodContent;

    @SerializedName("vod_tv")
    private String vodTv;

    @SerializedName("vod_class")
    private String vodClass;

    @SerializedName("vod_pubdate")
    private String vodPubdate;

    @SerializedName("vod_duration")
    private String vodDuration;

    @SerializedName("vod_author")
    private String vodAuthor;

    @SerializedName("vod_score")
    private String vodScore;

    @SerializedName("vod_lang")
    private String vodLang;

    @SerializedName("vod_play_from")
    private String vodPlayFrom;

    @SerializedName("vod_play_url")
    private String vodPlayUrl;

    @SerializedName("vod_tag")
    private String vodTag;

    @SerializedName("vod_pic_thumb")
    private String vodPicThumb;

    @SerializedName("vod_pic_slide")
    private String vodPicSlide;

    @SerializedName("vod_pic_screenshot")
    private String vodPicScreenshot;

    private long createTime;

    private long updateTime;

    public static List<CustomVod> arrayFrom(String str) {
        Type listType = new TypeToken<List<CustomVod>>() {}.getType();
        List<CustomVod> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public static CustomVod from(Vod vod) {
        CustomVod item = new CustomVod();
        item.setVodId(vod.getVodId());
        item.setVodName(vod.getVodName());
        item.setTypeName(vod.getTypeName());
        item.setVodPic(vod.getVodPic());
        item.setVodRemarks(vod.getVodRemarks());
        item.setVodYear(vod.getVodYear());
        item.setVodArea(vod.getVodArea());
        item.setVodDirector(vod.getVodDirector());
        item.setVodActor(vod.getVodActor());
        item.setVodContent(vod.getVodContent());
        item.setVodTv(vod.getVodTv());
        item.setVodClass(vod.getVodClass());
        item.setVodPubdate(vod.getVodPubdate());
        item.setVodDuration(vod.getVodDuration());
        item.setVodAuthor(vod.getVodAuthor());
        item.setVodScore(vod.getVodScore());
        item.setVodPlayFrom(vod.getVodPlayFrom());
        item.setVodPlayUrl(vod.getVodPlayUrl());
        item.setVodTag(vod.getVodTag());
        item.setVodPicThumb(vod.getVodPicThumb());
        item.setVodPicSlide(vod.getVodPicSlide());
        item.setVodPicScreenshot(vod.getVodPicScreenshot());
        return item;
    }

    /** 转换为站点 Vod，可直接用于详情/播放链路 */
    public Vod toVod() {
        Vod vod = new Vod();
        vod.setVodId(getVodId());
        vod.setVodName(getVodName());
        vod.setVodPic(getVodPic());
        vod.setVodRemarks(getVodRemarks());
        vod.setVodYear(getVodYear());
        vod.setVodArea(getVodArea());
        vod.setVodDirector(getVodDirector());
        vod.setVodActor(getVodActor());
        vod.setVodContent(getVodContent());
        vod.setVodTv(getVodTv());
        vod.setVodClass(getVodClass());
        vod.setVodPubdate(getVodPubdate());
        vod.setVodDuration(getVodDuration());
        vod.setVodAuthor(getVodAuthor());
        vod.setVodScore(getVodScore());
        vod.setVodPlayFrom(getVodPlayFrom());
        vod.setVodPlayUrl(getVodPlayUrl());
        vod.setVodTag(getVodTag());
        vod.setVodPicThumb(getVodPicThumb());
        vod.setVodPicSlide(getVodPicSlide());
        vod.setVodPicScreenshot(getVodPicScreenshot());
        return vod;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVodId() {
        return TextUtils.isEmpty(vodId) ? "" : vodId.trim();
    }

    public void setVodId(String vodId) {
        this.vodId = vodId;
    }

    public String getVodName() {
        return TextUtils.isEmpty(vodName) ? "" : vodName.trim();
    }

    public void setVodName(String vodName) {
        this.vodName = vodName;
    }

    public String getTypeName() {
        return TextUtils.isEmpty(typeName) ? "" : typeName.trim();
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getVodPic() {
        return TextUtils.isEmpty(vodPic) ? "" : vodPic.trim();
    }

    public void setVodPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public String getVodRemarks() {
        return TextUtils.isEmpty(vodRemarks) ? "" : vodRemarks.trim();
    }

    public void setVodRemarks(String vodRemarks) {
        this.vodRemarks = vodRemarks;
    }

    public String getVodYear() {
        return TextUtils.isEmpty(vodYear) ? "" : vodYear.trim();
    }

    public void setVodYear(String vodYear) {
        this.vodYear = vodYear;
    }

    public String getVodArea() {
        return TextUtils.isEmpty(vodArea) ? "" : vodArea.trim();
    }

    public void setVodArea(String vodArea) {
        this.vodArea = vodArea;
    }

    public String getVodDirector() {
        return TextUtils.isEmpty(vodDirector) ? "" : vodDirector.trim();
    }

    public void setVodDirector(String vodDirector) {
        this.vodDirector = vodDirector;
    }

    public String getVodActor() {
        return TextUtils.isEmpty(vodActor) ? "" : vodActor.trim();
    }

    public void setVodActor(String vodActor) {
        this.vodActor = vodActor;
    }

    public String getVodContent() {
        return TextUtils.isEmpty(vodContent) ? "" : vodContent.trim();
    }

    public void setVodContent(String vodContent) {
        this.vodContent = vodContent;
    }

    public String getVodTv() {
        return TextUtils.isEmpty(vodTv) ? "" : vodTv.trim();
    }

    public void setVodTv(String vodTv) {
        this.vodTv = vodTv;
    }

    public String getVodClass() {
        return TextUtils.isEmpty(vodClass) ? "" : vodClass.trim();
    }

    public void setVodClass(String vodClass) {
        this.vodClass = vodClass;
    }

    public String getVodPubdate() {
        return TextUtils.isEmpty(vodPubdate) ? "" : vodPubdate.trim();
    }

    public void setVodPubdate(String vodPubdate) {
        this.vodPubdate = vodPubdate;
    }

    public String getVodDuration() {
        return TextUtils.isEmpty(vodDuration) ? "" : vodDuration.trim();
    }

    public void setVodDuration(String vodDuration) {
        this.vodDuration = vodDuration;
    }

    public String getVodAuthor() {
        return TextUtils.isEmpty(vodAuthor) ? "" : vodAuthor.trim();
    }

    public void setVodAuthor(String vodAuthor) {
        this.vodAuthor = vodAuthor;
    }

    public String getVodScore() {
        return TextUtils.isEmpty(vodScore) ? "" : vodScore.trim();
    }

    public void setVodScore(String vodScore) {
        this.vodScore = vodScore;
    }

    public String getVodLang() {
        return TextUtils.isEmpty(vodLang) ? "" : vodLang.trim();
    }

    public void setVodLang(String vodLang) {
        this.vodLang = vodLang;
    }

    public String getVodPlayFrom() {
        return TextUtils.isEmpty(vodPlayFrom) ? "" : vodPlayFrom;
    }

    public void setVodPlayFrom(String vodPlayFrom) {
        this.vodPlayFrom = vodPlayFrom;
    }

    public String getVodPlayUrl() {
        return TextUtils.isEmpty(vodPlayUrl) ? "" : vodPlayUrl;
    }

    public void setVodPlayUrl(String vodPlayUrl) {
        this.vodPlayUrl = vodPlayUrl;
    }

    public String getVodTag() {
        return TextUtils.isEmpty(vodTag) ? "" : vodTag;
    }

    public void setVodTag(String vodTag) {
        this.vodTag = vodTag;
    }

    public String getVodPicThumb() {
        return TextUtils.isEmpty(vodPicThumb) ? "" : vodPicThumb.trim();
    }

    public void setVodPicThumb(String vodPicThumb) {
        this.vodPicThumb = vodPicThumb;
    }

    public String getVodPicSlide() {
        return TextUtils.isEmpty(vodPicSlide) ? "" : vodPicSlide.trim();
    }

    public void setVodPicSlide(String vodPicSlide) {
        this.vodPicSlide = vodPicSlide;
    }

    public String getVodPicScreenshot() {
        return TextUtils.isEmpty(vodPicScreenshot) ? "" : vodPicScreenshot.trim();
    }

    public void setVodPicScreenshot(String vodPicScreenshot) {
        this.vodPicScreenshot = vodPicScreenshot;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    /** 判断是否录入了可播放的播放地址 */
    public boolean hasPlayUrl() {
        return !getVodPlayUrl().isEmpty();
    }

    /** 保存（新增或更新），维护时间戳 */
    public void save() {
        long now = System.currentTimeMillis();
        if (getId() == 0) setCreateTime(now);
        setUpdateTime(now);
        AppDatabase.get().getCustomVodDao().insertOrUpdate(this);
    }

    public void delete() {
        AppDatabase.get().getCustomVodDao().delete(this);
    }

    public static List<CustomVod> getAll() {
        return AppDatabase.get().getCustomVodDao().getAll();
    }

    public static void deleteAll() {
        AppDatabase.get().getCustomVodDao().deleteAll();
    }

    public static List<String> getTypeNames() {
        List<String> types = new ArrayList<>();
        for (CustomVod vod : getAll()) {
            if (!vod.getTypeName().isEmpty() && !types.contains(vod.getTypeName())) types.add(vod.getTypeName());
        }
        return types;
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }
}
