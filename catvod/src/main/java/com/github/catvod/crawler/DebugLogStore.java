package com.github.catvod.crawler;

import android.os.Process;
import android.text.TextUtils;

import com.github.catvod.Init;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebugLogStore {

    // 日志等级：D=调试 I=信息 W=警告 E=错误
    public static final int D = 1;
    public static final int I = 2;
    public static final int W = 3;
    public static final int E = 4;

    private static final String[] LEVELS = {"", "D", "I", "W", "E"};
    private static final Object LOCK = new Object();
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();
    private static final ThreadLocal<SimpleDateFormat> FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US));
    private static final String FILE_NAME = "tv-debug-log.txt";
    private static final int MAX_LINES = 2000;
    private static final int MAX_MESSAGE_CHARS = 12000;
    // 单线程异步写文件，避免同步磁盘 I/O 阻塞爬虫线程
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "debug-log-writer");
        thread.setDaemon(true);
        return thread;
    });
    private static long version;
    private static volatile boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        DebugLogStore.enabled = enabled;
    }

    public static void add(String tag, String msg) {
        add(D, tag, msg);
    }

    public static void add(int level, String tag, String msg) {
        if (!isEnabled()) return;
        if (TextUtils.isEmpty(msg)) return;
        int lv = level < D || level > E ? D : level;
        // 与 logcat threadtime 格式保持一致：MM-dd HH:mm:ss.SSS PID TID 级别 TAG: 消息
        String line = String.format(Locale.US, "%s %5d %5d %s %s: %s", FORMAT.get().format(new Date()), Process.myPid(), Process.myTid(), LEVELS[lv], safe(tag), limit(msg));
        synchronized (LOCK) {
            LINES.addLast(line);
            if (LINES.size() > MAX_LINES) LINES.removeFirst();
            version++;
        }
        // 异步写文件，不阻塞调用线程
        WRITER.execute(() -> writeLocked(line));
    }

    public static String text() {
        List<String> copy;
        synchronized (LOCK) {
            copy = new ArrayList<>(LINES);
        }
        if (copy.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : copy) builder.append(line).append('\n');
        return builder.toString();
    }

    public static List<String> snapshot() {
        synchronized (LOCK) {
            return new ArrayList<>(LINES);
        }
    }

    public static int size() {
        synchronized (LOCK) {
            return LINES.size();
        }
    }

    public static long bytes() {
        try {
            File file = file();
            return file != null && file.exists() ? file.length() : 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    public static long version() {
        return version;
    }

    public static void clear() {
        synchronized (LOCK) {
            LINES.clear();
            version++;
            delete();
        }
    }

    private static String safe(String tag) {
        return TextUtils.isEmpty(tag) ? "Debug" : tag;
    }

    private static String limit(String msg) {
        if (msg.length() <= MAX_MESSAGE_CHARS) return msg;
        return msg.substring(0, MAX_MESSAGE_CHARS) + " ...(truncated " + (msg.length() - MAX_MESSAGE_CHARS) + " chars)";
    }

    private static File file() {
        try {
            return new File(Init.context().getCacheDir(), FILE_NAME);
        } catch (Throwable e) {
            return null;
        }
    }

    private static void writeLocked(String line) {
        try {
            File file = file();
            if (file == null) return;
            try (FileOutputStream stream = new FileOutputStream(file, true)) {
                stream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void delete() {
        try {
            File file = file();
            if (file != null && file.exists()) file.delete();
        } catch (Throwable ignored) {
        }
    }
}
