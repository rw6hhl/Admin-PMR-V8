package com.pmr.admin;

import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppLog {
    private static final List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static LogUpdateListener listener;

    public interface LogUpdateListener {
        void onLogUpdated(String newLog);
    }

    public static void setListener(LogUpdateListener l) {
        listener = l;
    }

    public static synchronized void add(String message) {
        String time = sdf.format(new Date());
        String logLine = time + " " + message;
        logs.add(logLine);
        if (logs.size() > 1000) logs.remove(0); // Ограничиваем память
        
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onLogUpdated(logLine));
        }
    }

    public static synchronized String getAllLogs() {
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

    public static synchronized void clear() {
        logs.clear();
    }
}