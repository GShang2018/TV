package com.fongmi.android.tv.player.exo;

import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.App;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 广告规则配置，对应外部 JSON 点播配置格式：
 * <pre>
 * {
 *   "rules": [
 *     {
 *       "name": "proxy",
 *       "hosts": []
 *     },
 *     {
 *       "name": "磁力廣告",
 *       "hosts": ["magnet"],
 *       "regex": ["更多", "社區"]
 *     },
 *     {
 *       "name": "量子廣告",
 *       "hosts": ["vip.lz", "hd.lz", "v.cdnlz"],
 *       "regex": [
 *         "#EXT-X-DISCONTINUITY\\r*\\n*#EXTINF:6.433333,[\\s\\S]*?#EXT-X-DISCONTINUITY",
 *         "#EXTINF.*?\\s+.*?1o.*?\\.ts\\s+"
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public class AdRule {

    private static final String TAG = "AdRule";

    @SerializedName("rules")
    private List<RuleItem> rules;

    public static AdRule from(String json) {
        try {
            return App.gson().fromJson(json, AdRule.class);
        } catch (Exception e) {
            Log.e(TAG, "解析广告规则 JSON 失败", e);
            return new AdRule();
        }
    }

    public static AdRule from(JsonElement element) {
        try {
            return App.gson().fromJson(element, AdRule.class);
        } catch (Exception e) {
            Log.e(TAG, "解析广告规则 JsonElement 失败", e);
            return new AdRule();
        }
    }

    public List<RuleItem> getRules() {
        return rules == null ? Collections.emptyList() : rules;
    }

    /**
     * 检查给定的 URL 是否匹配任何规则的 host 模式。
     */
    public boolean matchesHost(String url) {
        if (TextUtils.isEmpty(url)) return false;
        for (RuleItem rule : getRules()) {
            if (rule.matchesHost(url)) return true;
        }
        return false;
    }

    /**
     * 获取匹配给定 URL 的所有规则中的正则表达式列表。
     */
    public List<Pattern> getCompiledPatterns(String url) {
        if (TextUtils.isEmpty(url)) return Collections.emptyList();
        List<Pattern> patterns = new ArrayList<>();
        for (RuleItem rule : getRules()) {
            if (rule.matchesHost(url)) {
                patterns.addAll(rule.getCompiledPatterns());
            }
        }
        return patterns;
    }

    /**
     * 获取所有规则中匹配 URL 的正则表达式（原始字符串形式）。
     */
    public List<String> getRegexPatterns(String url) {
        if (TextUtils.isEmpty(url)) return Collections.emptyList();
        List<String> regexes = new ArrayList<>();
        for (RuleItem rule : getRules()) {
            if (rule.matchesHost(url)) {
                regexes.addAll(rule.getRegex());
            }
        }
        return regexes;
    }

    /**
     * 单个规则项。
     */
    public static class RuleItem {

        @SerializedName("name")
        private String name;
        @SerializedName("hosts")
        private List<String> hosts;
        @SerializedName("regex")
        private List<String> regex;

        private transient List<Pattern> compiledPatterns;

        public String getName() {
            return name == null ? "" : name;
        }

        public List<String> getHosts() {
            return hosts == null ? Collections.emptyList() : hosts;
        }

        public List<String> getRegex() {
            return regex == null ? Collections.emptyList() : regex;
        }

        /**
         * 检查给定的 URL 是否匹配此规则的 host 模式。
         * 支持通配符 * 匹配任意字符。
         */
        public boolean matchesHost(String url) {
            if (TextUtils.isEmpty(url)) return false;
            List<String> hostPatterns = getHosts();
            if (hostPatterns.isEmpty()) return false;
            for (String pattern : hostPatterns) {
                if (matchHostPattern(url, pattern)) return true;
            }
            return false;
        }

        private static boolean matchHostPattern(String url, String pattern) {
            if (TextUtils.isEmpty(pattern)) return false;
            // 支持通配符 * 匹配
            if (pattern.contains("*")) {
                String regex = pattern
                        .replace(".", "\\.")
                        .replace("*", ".*");
                try {
                    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(url).find();
                } catch (PatternSyntaxException e) {
                    Log.e(TAG, "host 通配符正则编译失败: " + pattern, e);
                    return false;
                }
            }
            // 普通子串匹配
            return url.toLowerCase().contains(pattern.toLowerCase());
        }

        /**
         * 获取编译后的正则表达式列表（懒加载）。
         */
        public List<Pattern> getCompiledPatterns() {
            if (compiledPatterns == null) {
                compiledPatterns = new ArrayList<>();
                for (String regexStr : getRegex()) {
                    try {
                        compiledPatterns.add(Pattern.compile(regexStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
                    } catch (PatternSyntaxException e) {
                        Log.e(TAG, "正则编译失败: " + regexStr, e);
                    }
                }
            }
            return compiledPatterns;
        }
    }
}
