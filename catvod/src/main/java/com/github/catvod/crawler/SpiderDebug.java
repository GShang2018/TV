package com.github.catvod.crawler;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();

    public static void log(Throwable th) {
        log(TAG, th);
    }

    public static void log(String tag, Throwable th) {
        if (th == null) return;
        th.printStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        String msg = sw.toString();
        // Logger 输出 logcat 后经 App 桥接统一写入 DebugLogStore，避免重复
        Logger.t(tag).e(msg);
    }

    public static void log(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        Logger.t(TAG).d(msg);
    }

    public static void log(String tag, String msg, Object... args) {
        if (TextUtils.isEmpty(msg)) return;
        String formatted = format(msg, args);
        Logger.t(tag).d(formatted);
    }

    private static String format(String msg, Object... args) {
        try {
            return args == null || args.length == 0 ? msg : String.format(Locale.US, msg, args);
        } catch (Throwable e) {
            return msg;
        }
    }
}
