package com.fongmi.android.tv.utils;

import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 收藏夹 WebDAV 云同步工具类。
 * 参考 mooknote 的 WebDAV 备份实现，将收藏夹导出为 JSON 并上传到 WebDAV 服务器，
 * 或从服务器下载最新备份并导入本地。
 */
public class WebDav {

    public static final String PREFIX = "keep_backup_";
    public static final String SUFFIX = "keep.json";
    public static final int MAX_BACKUP_COUNT = 5;

    private static final String KEY_URL = "webdav_url";
    private static final String KEY_USERNAME = "webdav_username";
    private static final String KEY_PASSWORD = "webdav_password";
    private static final String KEY_PATH = "webdav_path";

    private static final int TIMEOUT = 30 * 1000;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType XML = MediaType.parse("application/xml; charset=utf-8");
    private static final String PROPFIND_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:resourcetype/></D:prop></D:propfind>";

    public static class Result {
        public boolean success;
        public String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static String getUrl() {
        return Prefers.getString(KEY_URL);
    }

    public static String getUsername() {
        return Prefers.getString(KEY_USERNAME);
    }

    public static String getPassword() {
        return Prefers.getString(KEY_PASSWORD);
    }

    public static String getPath() {
        String path = Prefers.getString(KEY_PATH);
        return TextUtils.isEmpty(path) ? "/tv" : path;
    }

    public static boolean isConfigured() {
        return !TextUtils.isEmpty(getUrl()) && !TextUtils.isEmpty(getUsername()) && !TextUtils.isEmpty(getPassword());
    }

    public static void saveConfig(String url, String username, String password, String path) {
        Prefers.put(KEY_URL, url);
        Prefers.put(KEY_USERNAME, username);
        Prefers.put(KEY_PASSWORD, password);
        Prefers.put(KEY_PATH, path);
    }

    public static void clearConfig() {
        Prefers.remove(KEY_URL);
        Prefers.remove(KEY_USERNAME);
        Prefers.remove(KEY_PASSWORD);
        Prefers.remove(KEY_PATH);
    }

    /**
     * 测试 WebDAV 连接，目录不存在则尝试创建。
     */
    public static Result testConnection(String url, String username, String password, String path) {
        try {
            String baseUrl = trimSlash(url);
            String davUrl = baseUrl + path;
            OkHttpClient client = OkHttp.client(TIMEOUT);

            // PROPFIND 检查目录
            Response propfind = execute(client, "PROPFIND", davUrl, username, password, "Depth", "0", XML, PROPFIND_BODY.getBytes(StandardCharsets.UTF_8));
            int code = propfind.code();
            propfind.close();
            if (code == 207) return new Result(true, "连接成功");
            if (code == 401) return new Result(false, "认证失败，请检查用户名和密码");
            if (code != 404) return new Result(false, "服务器返回错误: " + code);

            // 目录不存在，尝试 MKCOL 创建
            Response mkcol = execute(client, "MKCOL", davUrl, username, password, null, null, null);
            int mkCode = mkcol.code();
            mkcol.close();
            if (mkCode == 201) return new Result(true, "连接成功，已创建目录");
            if (mkCode == 405) return new Result(true, "连接成功，目录已存在");
            if (mkCode == 401) return new Result(false, "认证失败，请检查用户名和密码");
            if (mkCode == 409) return new Result(false, "父目录不存在，请检查路径");
            return new Result(false, "创建目录失败: " + mkCode);
        } catch (Exception e) {
            return new Result(false, "连接失败: " + e.getMessage());
        }
    }

    /**
     * 上传收藏夹备份到 WebDAV。
     */
    public static Result upload() {
        if (!isConfigured()) return new Result(false, "未配置 WebDAV");
        try {
            File file = KeepBackup.export();
            if (file == null || !file.exists()) return new Result(false, "创建备份失败");

            String baseUrl = trimSlash(getUrl());
            String dirUrl = baseUrl + getPath();
            String fileName = generateFileName();
            String zipUrl = dirUrl + "/" + fileName;

            OkHttpClient client = OkHttp.client(TIMEOUT);
            byte[] data = Path.readToByte(file);
            Response response = execute(client, "PUT", zipUrl, getUsername(), getPassword(), null, null, JSON, data);
            int code = response.code();
            response.close();
            // 清理临时备份文件
            file.delete();

            if (code == 200 || code == 201 || code == 204) {
                cleanupOldBackups(client, dirUrl);
                return new Result(true, "上传成功");
            }
            return new Result(false, "上传备份文件失败: " + code);
        } catch (Exception e) {
            return new Result(false, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 从 WebDAV 下载最新备份并导入本地。
     */
    public static Result download() {
        if (!isConfigured()) return new Result(false, "未配置 WebDAV");
        try {
            String baseUrl = trimSlash(getUrl());
            String dirUrl = baseUrl + getPath();
            OkHttpClient client = OkHttp.client(TIMEOUT);

            List<String> backups = listRemoteBackups(client, dirUrl);
            if (backups.isEmpty()) return new Result(false, "服务器上没有备份文件，请先从其他设备上传");

            String latest = backups.get(backups.size() - 1);
            String zipUrl = dirUrl + "/" + latest;

            Response response = execute(client, "GET", zipUrl, getUsername(), getPassword(), null, null, null);
            if (response.code() != 200) {
                response.close();
                return new Result(false, "下载备份文件失败: " + response.code());
            }
            byte[] data = response.body().bytes();
            response.close();

            String json = new String(data, StandardCharsets.UTF_8);
            if (KeepBackup.importJson(json)) {
                return new Result(true, "下载并导入成功");
            }
            return new Result(false, "导入备份失败");
        } catch (Exception e) {
            return new Result(false, "下载失败: " + e.getMessage());
        }
    }

    /**
     * 生成带时间戳的备份文件名。
     */
    private static String generateFileName() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        return PREFIX + time + "." + SUFFIX;
    }

    /**
     * 获取远程目录中的备份文件列表（按文件名升序）。
     */
    private static List<String> listRemoteBackups(OkHttpClient client, String dirUrl) {
        List<String> backups = new ArrayList<>();
        try {
            Response response = execute(client, "PROPFIND", dirUrl, getUsername(), getPassword(), "Depth", "1", XML, PROPFIND_BODY.getBytes(StandardCharsets.UTF_8));
            if (response.code() != 207) {
                response.close();
                return backups;
            }
            String body = response.body().string();
            response.close();

            Pattern pattern = Pattern.compile("<(?:\\w+:)?href[^>]*>([^<]+)</(?:\\w+:)?href>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(body);
            while (matcher.find()) {
                String href = matcher.group(1);
                if (href == null) continue;
                try {
                    href = URLDecoder.decode(href, "UTF-8");
                } catch (Exception ignored) {
                }
                String fileName = href.substring(href.lastIndexOf('/') + 1);
                if (fileName.startsWith(PREFIX) && fileName.endsWith("." + SUFFIX)) {
                    backups.add(fileName);
                }
            }
            java.util.Collections.sort(backups);
        } catch (Exception ignored) {
        }
        return backups;
    }

    /**
     * 清理旧备份，保留最新的 MAX_BACKUP_COUNT 个。
     */
    private static void cleanupOldBackups(OkHttpClient client, String dirUrl) {
        List<String> backups = listRemoteBackups(client, dirUrl);
        if (backups.size() <= MAX_BACKUP_COUNT) return;
        List<String> toDelete = backups.subList(0, backups.size() - MAX_BACKUP_COUNT);
        for (String fileName : toDelete) {
            try {
                Response response = execute(client, "DELETE", dirUrl + "/" + fileName, getUsername(), getPassword(), null, null, null);
                response.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 执行 WebDAV 请求（同步）。
     */
    private static Response execute(OkHttpClient client, String method, String url, String username, String password, String headerName, String headerValue, MediaType mediaType, byte[] body) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).method(method, body != null ? RequestBody.create(mediaType, body) : null);
        builder.header("Authorization", basicAuth(username, password));
        if (headerName != null) builder.header(headerName, headerValue);
        return client.newCall(builder.build()).execute();
    }

    private static Response execute(OkHttpClient client, String method, String url, String username, String password, String headerName, String headerValue, MediaType mediaType) throws IOException {
        return execute(client, method, url, username, password, headerName, headerValue, mediaType, null);
    }

    /**
     * Basic Auth 编码。
     */
    private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.encodeToString(credentials.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static String trimSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
