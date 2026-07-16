package com.fongmi.android.tv.server;

import java.util.ArrayList;
import java.util.List;

public class LogBuffer {

    private static final List<String> logs = new ArrayList<>();
    private static final int MAX_LOG_SIZE = 500;

    public static void append(String message) {
        synchronized (logs) {
            logs.add(message);
            if (logs.size() > MAX_LOG_SIZE) {
                logs.remove(0);
            }
        }
    }

    public static String poll() {
        synchronized (logs) {
            if (logs.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (String log : logs) {
                sb.append(log).append("\n");
            }
            logs.clear();
            return sb.toString();
        }
    }
}
