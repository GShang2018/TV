package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.utils.Trans;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Root(strict = false)
public class Vod implements Parcelable {

    @Element(name = "id", required = false)
    @SerializedName("vod_id")
    private String vodId;

    @Element(name = "name", required = false)
    @SerializedName("vod_name")
    private String vodName;

    @Element(name = "type", required = false)
    @SerializedName("type_name")
    private String typeName;

    @Element(name = "pic", required = false)
    @SerializedName("vod_pic")
    private String vodPic;

    @Element(name = "note", required = false)
    @SerializedName("vod_remarks")
    private String vodRemarks;

    @Element(name = "year", required = false)
    @SerializedName("vod_year")
    private String vodYear;

    @Element(name = "area", required = false)
    @SerializedName("vod_area")
    private String vodArea;

    @Element(name = "director", required = false)
    @SerializedName("vod_director")
    private String vodDirector;

    @Element(name = "actor", required = false)
    @SerializedName("vod_actor")
    private String vodActor;

    @Element(name = "des", required = false)
    @SerializedName("vod_content")
    private String vodContent;

    @Element(name = "tv", required = false)
    @SerializedName("vod_tv")
    private String vodTv;

    @Element(name = "class", required = false)
    @SerializedName("vod_class")
    private String vodClass;

    @Element(name = "pubdate", required = false)
    @SerializedName("vod_pubdate")
    private String vodPubdate;

    @Element(name = "duration", required = false)
    @SerializedName("vod_duration")
    private String vodDuration;

    @Element(name = "author", required = false)
    @SerializedName("vod_author")
    private String vodAuthor;

    @Element(name = "score", required = false)
    @SerializedName("vod_score")
    private String vodScore;

    @SerializedName("vod_play_from")
    private String vodPlayFrom;

    @SerializedName("vod_play_url")
    private String vodPlayUrl;

    @Element(name = "rel", required = false)
    private String relXml;

    @SerializedName("vod_rel_vod")
    private JsonElement vodRelVod;

    @SerializedName("vod_rel_vod_list")
    private List<Vod> vodRelVodList;

    @SerializedName("vod_tag")
    private String vodTag;

    @SerializedName("vod_pic_thumb")
    private String vodPicThumb;

    @SerializedName("vod_pic_slide")
    private String vodPicSlide;

    @SerializedName("vod_pic_screenshot")
    private String vodPicScreenshot;

    @SerializedName("action")
    private String action;

    @SerializedName("cate")
    private Cate cate;

    @SerializedName("style")
    private Style style;

    @SerializedName("land")
    private int land;

    @SerializedName("circle")
    private int circle;

    @SerializedName("ratio")
    private float ratio;

    @Path("dl")
    @ElementList(entry = "dd", required = false, inline = true)
    private List<Flag> vodFlags;

    private Site site;

    public static List<Vod> arrayFrom(String str) {
        Type listType = new TypeToken<List<Vod>>() {}.getType();
        List<Vod> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Vod() {
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

    public String getVodPic() {
        return TextUtils.isEmpty(vodPic) ? "" : vodPic.trim();
    }

    public void setVodPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public String getVodRemarks() {
        return TextUtils.isEmpty(vodRemarks) ? "" : vodRemarks.trim();
    }

    public String getVodYear() {
        return TextUtils.isEmpty(vodYear) ? "" : vodYear.trim();
    }

    public String getVodArea() {
        return TextUtils.isEmpty(vodArea) ? "" : vodArea.trim();
    }

    public String getVodDirector() {
        return TextUtils.isEmpty(vodDirector) ? "" : vodDirector.trim();
    }

    public String getVodActor() {
        return TextUtils.isEmpty(vodActor) ? "" : vodActor.trim();
    }

    public String getVodContent() {
        return TextUtils.isEmpty(vodContent) ? "" : vodContent.trim().replace("\n", "<br>");
    }

    public String getVodTv() {
        return TextUtils.isEmpty(vodTv) ? "" : vodTv.trim();
    }

    public String getVodClass() {
        return TextUtils.isEmpty(vodClass) ? "" : vodClass.trim();
    }

    public String getVodPubdate() {
        return TextUtils.isEmpty(vodPubdate) ? "" : vodPubdate.trim();
    }

    public String getVodDuration() {
        return TextUtils.isEmpty(vodDuration) ? "" : vodDuration.trim();
    }

    public String getVodAuthor() {
        return TextUtils.isEmpty(vodAuthor) ? "" : vodAuthor.trim();
    }

    public String getVodScore() {
        return TextUtils.isEmpty(vodScore) ? "" : vodScore.trim();
    }

    public String getVodPlayFrom() {
        return TextUtils.isEmpty(vodPlayFrom) ? "" : vodPlayFrom;
    }

    public String getVodPlayUrl() {
        return TextUtils.isEmpty(vodPlayUrl) ? "" : vodPlayUrl;
    }

    public List<String> getRelIds() {
        List<String> ids = new ArrayList<>();
        if (vodRelVod != null) {
            if (vodRelVod.isJsonArray()) {
                for (JsonElement element : vodRelVod.getAsJsonArray()) {
                    if (element.isJsonPrimitive()) addRelIds(ids, element.getAsString());
                }
            } else if (vodRelVod.isJsonPrimitive()) {
                addRelIds(ids, vodRelVod.getAsString());
            }
        }
        if (!TextUtils.isEmpty(relXml)) addRelIds(ids, relXml);
        return ids;
    }

    private void addRelIds(List<String> ids, String raw) {
        if (TextUtils.isEmpty(raw)) return;
        for (String part : raw.split("[,，|;；]+")) {
            String id = part.trim();
            if (id.isEmpty() || "0".equals(id)) continue;
            if (!id.contains("://")) {
                int colon = id.indexOf(':');
                if (colon > 0) id = id.substring(0, colon).trim();
            }
            if (id.isEmpty() || ids.contains(id)) continue;
            ids.add(id);
        }
    }

    public List<Vod> getRelVods() {
        List<Vod> items = new ArrayList<>();
        if (vodRelVodList == null) return items;
        for (Vod vod : vodRelVodList) {
            if (vod == null || vod.getVodId().isEmpty()) continue;
            // 推荐列表条目默认按普通视频处理，源显式传入 tag（如 folder/manga）则保留覆盖
            if (vod.getVodTag().isEmpty()) vod.setVodTag("file");
            if (!items.contains(vod)) items.add(vod);
        }
        return items;
    }

    public boolean hasRel() {
        return !getRelVods().isEmpty() || !getRelIds().isEmpty();
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

    public String getVodPicSlide() {
        return TextUtils.isEmpty(vodPicSlide) ? "" : vodPicSlide.trim();
    }

    public String getVodPicScreenshot() {
        return TextUtils.isEmpty(vodPicScreenshot) ? "" : vodPicScreenshot.trim();
    }

    public List<String> getGallery() {
        List<String> items = new ArrayList<>();
        //if (!getVodPic().isEmpty()) items.add(getVodPic());
        if (!getVodPicThumb().isEmpty()) items.add(getVodPicThumb());
        for (String s : getVodPicSlide().split("\\$\\$\\$")) {
            if (!s.trim().isEmpty()) items.add(s.trim());
        }
        for (String s : getVodPicScreenshot().split("\\$\\$\\$")) {
            if (!s.trim().isEmpty()) items.add(s.trim());
        }
        return items;
    }

    public boolean hasGallery() {
        return !getVodPicThumb().isEmpty() || !getVodPicSlide().isEmpty() || !getVodPicScreenshot().isEmpty();
    }

    public String getAction() {
        return TextUtils.isEmpty(action) ? "" : action;
    }

    public Cate getCate() {
        return cate;
    }

    public Style getStyle() {
        if (style != null) {
            if (style.getRawRatio() <= 0 && getRatio() > 0) {
                return new Style(style.getType(), getRatio());
            }
            return style;
        }
        return Style.get(getLand(), getCircle(), getRatio());
    }

    public int getLand() {
        return land;
    }

    public int getCircle() {
        return circle;
    }

    public float getRatio() {
        return ratio;
    }

    public List<Flag> getVodFlags() {
        return vodFlags = vodFlags == null ? new ArrayList<>() : vodFlags;
    }

    public void setVodFlags(List<Flag> vodFlags) {
        this.vodFlags = vodFlags;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getSiteName() {
        return getSite() == null ? "" : getSite().getName();
    }

    public String getSiteKey() {
        return getSite() == null ? "" : getSite().getKey();
    }

    public int getSiteVisible() {
        return getSite() == null ? View.GONE : View.VISIBLE;
    }

    public int getYearVisible() {
        return getSite() != null || getVodYear().length() < 4 ? View.GONE : View.VISIBLE;
    }

    public int getNameVisible() {
        return getVodName().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public int getRemarkVisible() {
        return getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public boolean isFolder() {
        return "folder".equals(getVodTag()) || getCate() != null;
    }

    public boolean isAction() {
        return !getAction().isEmpty();
    }

    public boolean isManga() {
        return "manga".equals(getVodTag());
    }

    public Style getStyle(Style style) {
        return getStyle() != null ? getStyle() : style != null ? style : Style.rect();
    }

    public String getVodPic(String pic) {
        if (getVodPic().isEmpty()) setVodPic(pic);
        return getVodPic();
    }

    public String getVodName(String name) {
        if (getVodName().isEmpty()) setVodName(name);
        return getVodName();
    }

    public void trans() {
        if (Trans.pass()) return;
        this.vodName = Trans.s2t(vodName);
        this.vodArea = Trans.s2t(vodArea);
        this.typeName = Trans.s2t(typeName);
        this.vodRemarks = Trans.s2t(vodRemarks);
        if (vodActor != null) this.vodActor = Sniffer.CLICKER.matcher(vodActor).find() ? vodActor : Trans.s2t(vodActor);
        if (vodContent != null) this.vodContent = Sniffer.CLICKER.matcher(vodContent).find() ? vodContent : Trans.s2t(vodContent);
        if (vodDirector != null) this.vodDirector = Sniffer.CLICKER.matcher(vodDirector).find() ? vodDirector : Trans.s2t(vodDirector);
        if (vodTv != null) this.vodTv = Trans.s2t(vodTv);
        if (vodClass != null) this.vodClass = Trans.s2t(vodClass);
        if (vodPubdate != null) this.vodPubdate = Trans.s2t(vodPubdate);
        if (vodDuration != null) this.vodDuration = Trans.s2t(vodDuration);
        if (vodAuthor != null) this.vodAuthor = Trans.s2t(vodAuthor);
        if (vodScore != null) this.vodScore = Trans.s2t(vodScore);
    }

    // ==================== 补充 Setter（供代码构造 Vod 使用，如"我的"自定义影视） ====================

    public void setVodRemarks(String vodRemarks) {
        this.vodRemarks = vodRemarks;
    }

    public void setVodYear(String vodYear) {
        this.vodYear = vodYear;
    }

    public void setVodArea(String vodArea) {
        this.vodArea = vodArea;
    }

    public void setVodDirector(String vodDirector) {
        this.vodDirector = vodDirector;
    }

    public void setVodActor(String vodActor) {
        this.vodActor = vodActor;
    }

    public void setVodContent(String vodContent) {
        this.vodContent = vodContent;
    }

    public void setVodTv(String vodTv) {
        this.vodTv = vodTv;
    }

    public void setVodClass(String vodClass) {
        this.vodClass = vodClass;
    }

    public void setVodPubdate(String vodPubdate) {
        this.vodPubdate = vodPubdate;
    }

    public void setVodDuration(String vodDuration) {
        this.vodDuration = vodDuration;
    }

    public void setVodAuthor(String vodAuthor) {
        this.vodAuthor = vodAuthor;
    }

    public void setVodScore(String vodScore) {
        this.vodScore = vodScore;
    }

    public void setVodPlayFrom(String vodPlayFrom) {
        this.vodPlayFrom = vodPlayFrom;
    }

    public void setVodPlayUrl(String vodPlayUrl) {
        this.vodPlayUrl = vodPlayUrl;
    }

    public void setVodPicThumb(String vodPicThumb) {
        this.vodPicThumb = vodPicThumb;
    }

    public void setVodPicSlide(String vodPicSlide) {
        this.vodPicSlide = vodPicSlide;
    }

    public void setVodPicScreenshot(String vodPicScreenshot) {
        this.vodPicScreenshot = vodPicScreenshot;
    }

    public void setVodFlags() {
        String[] playFlags = getVodPlayFrom().split("\\$\\$\\$");
        String[] playUrls = getVodPlayUrl().split("\\$\\$\\$");
        for (int i = 0; i < playFlags.length; i++) {
            if (playFlags[i].isEmpty() || i >= playUrls.length) continue;
            Flag item = Flag.create(playFlags[i].trim());
            item.createEpisode(playUrls[i]);
            getVodFlags().add(item);
        }
        for (Flag item : getVodFlags()) {
            if (item.getUrls() == null) continue;
            item.createEpisode(item.getUrls());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vod)) return false;
        Vod it = (Vod) obj;
        return getVodId().equals(it.getVodId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.vodId);
        dest.writeString(this.vodName);
        dest.writeString(this.typeName);
        dest.writeString(this.vodPic);
        dest.writeString(this.vodRemarks);
        dest.writeString(this.vodYear);
        dest.writeString(this.vodArea);
        dest.writeString(this.vodDirector);
        dest.writeString(this.vodActor);
        dest.writeString(this.vodContent);
        dest.writeString(this.vodTv);
        dest.writeString(this.vodClass);
        dest.writeString(this.vodPubdate);
        dest.writeString(this.vodDuration);
        dest.writeString(this.vodAuthor);
        dest.writeString(this.vodScore);
        dest.writeString(this.vodPlayFrom);
        dest.writeString(this.vodPlayUrl);
        dest.writeString(this.relXml);
        dest.writeString(this.vodRelVod == null ? null : this.vodRelVod.toString());
        dest.writeTypedList(this.vodRelVodList);
        dest.writeString(this.vodTag);
        dest.writeString(this.vodPicThumb);
        dest.writeString(this.vodPicSlide);
        dest.writeString(this.vodPicScreenshot);
        dest.writeString(this.action);
        dest.writeInt(this.land);
        dest.writeInt(this.circle);
        dest.writeFloat(this.ratio);
        dest.writeParcelable(this.cate, flags);
        dest.writeParcelable(this.style, flags);
        dest.writeTypedList(this.vodFlags);
        dest.writeParcelable(this.site, flags);
    }

    protected Vod(Parcel in) {
        this.vodId = in.readString();
        this.vodName = in.readString();
        this.typeName = in.readString();
        this.vodPic = in.readString();
        this.vodRemarks = in.readString();
        this.vodYear = in.readString();
        this.vodArea = in.readString();
        this.vodDirector = in.readString();
        this.vodActor = in.readString();
        this.vodContent = in.readString();
        this.vodTv = in.readString();
        this.vodClass = in.readString();
        this.vodPubdate = in.readString();
        this.vodDuration = in.readString();
        this.vodAuthor = in.readString();
        this.vodScore = in.readString();
        this.vodPlayFrom = in.readString();
        this.vodPlayUrl = in.readString();
        this.relXml = in.readString();
        String rel = in.readString();
        this.vodRelVod = rel == null ? null : JsonParser.parseString(rel);
        this.vodRelVodList = in.createTypedArrayList(Vod.CREATOR);
        this.vodTag = in.readString();
        this.vodPicThumb = in.readString();
        this.vodPicSlide = in.readString();
        this.vodPicScreenshot = in.readString();
        this.action = in.readString();
        this.land = in.readInt();
        this.circle = in.readInt();
        this.ratio = in.readFloat();
        this.cate = in.readParcelable(Cate.class.getClassLoader());
        this.style = in.readParcelable(Style.class.getClassLoader());
        this.vodFlags = in.createTypedArrayList(Flag.CREATOR);
        this.site = in.readParcelable(Site.class.getClassLoader());
    }

    public static final Creator<Vod> CREATOR = new Creator<>() {
        @Override
        public Vod createFromParcel(Parcel source) {
            return new Vod(source);
        }

        @Override
        public Vod[] newArray(int size) {
            return new Vod[size];
        }
    };
}
