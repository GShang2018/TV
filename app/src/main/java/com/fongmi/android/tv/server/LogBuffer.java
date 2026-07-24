package com.fongmi.android.tv.server;

import java.util.ArrayList;
import java.util.List;

public class LogBuffer {

    private static final List<String> logs = new ArrayList<>();
    private static final int MAX_LOG_SIZE = 500;
    private static int lastIndex = 0;

    public static void append(String message) {
        synchronized (logs) {
            logs.add(message);
            if (logs.size() > MAX_LOG_SIZE) {
                logs.remove(0);
                if (lastIndex > 0) lastIndex--;
            }
        }
    }

    public static String poll() {
        synchronized (logs) {
            if (logs.isEmpty() || lastIndex >= logs.size()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = lastIndex; i < logs.size(); i++) {
                sb.append(logs.get(i)).append("\n");
            }
            lastIndex = logs.size();
            return sb.toString();
        }
    }
}
