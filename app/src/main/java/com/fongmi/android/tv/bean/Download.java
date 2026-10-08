package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.db.AppDatabase;
import com.github.catvod.utils.Util;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Entity
public class Download {

    public static final int STATUS_QUEUED = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_FINISHED = 3;
    public static final int STATUS_FAILED = 4;

    @NonNull
    @PrimaryKey
    @SerializedName("id")
    private String id;
    @SerializedName("vodPic")
    private String vodPic;
    @SerializedName("vodName")
    private String vodName;
    @SerializedName("url")
    private String url;
    @SerializedName("header")
    private String header;
    @SerializedName("fileName")
    private String fileName;
    @SerializedName("savePath")
    private String savePath;
    @SerializedName("errorMsg")
    private String errorMsg;
    @SerializedName("source")
    private String source;
    @SerializedName("flag")
    private String flag;
    @SerializedName("status")
    private int status;
    @SerializedName("type")
    private int type;
    @SerializedName("progress")
    private long progress;
    @SerializedName("total")
    private long total;
    @SerializedName("createTime")
    private long createTime;

    public static Download objectFrom(String str) {
        return App.gson().fromJson(str, Download.class);
    }

    public static List<Download> arrayFrom(String str) {
        Type listType = new TypeToken<List<Download>>() {}.getType();
        List<Download> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Download() {
    }

    @Ignore
    public Download(String vodName, String vodPic, String url, String header, String fileName, String source, String flag) {
        this.id = Util.md5(url);
        this.vodName = vodName;
        this.vodPic = vodPic;
        this.url = url;
        this.header = header;
        this.fileName = fileName;
        this.source = source;
        this.flag = flag;
        this.status = STATUS_QUEUED;
        this.progress = 0;
        this.total = -1;
        this.type = 0;
        setCreateTime(System.currentTimeMillis());
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getVodPic() {
        return vodPic;
    }

    public void setVodPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public String getVodName() {
        return vodName;
    }

    public void setVodName(String vodName) {
        this.vodName = vodName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getErrorMsg() {
        return errorMsg == null ? "" : errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getSource() {
        return source == null ? "" : source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFlag() {
        return flag == null ? "" : flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public boolean isFinished() {
        return status == STATUS_FINISHED;
    }

    public boolean isDownloading() {
        return status == STATUS_DOWNLOADING;
    }

    public boolean isPaused() {
        return status == STATUS_PAUSED;
    }

    public boolean isFailed() {
        return status == STATUS_FAILED;
    }

    public int getPercent() {
        if (total <= 0) return 0;
        return (int) Math.min(100, progress * 100 / total);
    }

    public static List<Download> get() {
        return AppDatabase.get().getDownloadDao().find();
    }

    public static List<Download> getByStatus(int status) {
        return AppDatabase.get().getDownloadDao().findByStatus(status);
    }

    public static Download find(String id) {
        return AppDatabase.get().getDownloadDao().find(id);
    }

    public static void delete(String url) {
        AppDatabase.get().getDownloadDao().delete(Util.md5(url));
    }

    public static void delete(Download download) {
        AppDatabase.get().getDownloadDao().delete(download.getId());
    }

    public static void clear() {
        AppDatabase.get().getDownloadDao().delete();
    }

    public Download delete() {
        AppDatabase.get().getDownloadDao().delete(getId());
        return this;
    }

    public Download save() {
        AppDatabase.get().getDownloadDao().insertOrUpdate(this);
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }
}
