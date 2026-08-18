package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubtitleSearch {

    @SerializedName("code")
    private int code;
    @SerializedName("data")
    private List<Data> data;
    @SerializedName("result")
    private String result;

    public int getCode() {
        return code;
    }

    public List<Data> getData() {
        return data;
    }

    public boolean isSuccess() {
        return code == 0 && data != null;
    }

    public static class Data {

        @SerializedName("url")
        private String url;
        @SerializedName("ext")
        private String ext;
        @SerializedName("name")
        private String name;
        @SerializedName("languages")
        private List<String> languages;
        @SerializedName("extra_name")
        private String extraName;

        public String getUrl() {
            return url == null ? "" : url.trim();
        }

        public String getExt() {
            return ext == null ? "" : ext;
        }

        public String getName() {
            return name == null ? "" : name;
        }

        public String getLanguages() {
            if (languages == null || languages.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < languages.size(); i++) {
                if (i > 0) sb.append("/");
                sb.append(languages.get(i));
            }
            return sb.toString();
        }

        public String getExtraName() {
            return extraName == null ? "" : extraName;
        }

        public Sub toSub() {
            return Sub.create(getUrl(), getName(), getLanguages());
        }

        @NonNull
        @Override
        public String toString() {
            String lang = getLanguages();
            String extra = getExtraName();
            StringBuilder sb = new StringBuilder(getName());
            if (!lang.isEmpty()) sb.append(" [").append(lang).append("]");
            if (!extra.isEmpty()) sb.append(" ").append(extra);
            return sb.toString();
        }
    }
}
