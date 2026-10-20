package com.pmr.admin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppLog {
    private static final List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static LogUpdateListener listener;
    private static File currentLogFile;
    private static boolean verbose = true;
    private static final Object fileLock = new Object();
    private static boolean initialized = false;

    public interface LogUpdateListener {
        void onLogUpdated(String newLog);
    }

    public static void setListener(LogUpdateListener l) {
        listener = l;
    }

    public static synchronized void setVerbose(boolean v) {
        verbose = v;
        add("AppLog: verbose=" + v);
    }

    public static synchronized boolean isVerbose() {
        return verbose;
    }

    public static synchronized void init(Context ctx, String versionName) {
        if (ctx == null) return;
        if (initialized && currentLogFile != null) {
            add("AppLog: init уже выполнен, файл=" + currentLogFile.getName());
            return;
        }
        initialized = true;
        try {
            File dir = new File(ctx.getFilesDir(), "logs");
            if (!dir.exists()) dir.mkdirs();
            String prefix = "log_v" + versionName + "_";
            int maxN = 0;
            File[] files = dir.listFiles();
            StringBuilder sb = new StringBuilder("AppLog: init — файлы в logs/: ");
            if (files != null) {
                for (File f : files) {
                    sb.append(f.getName()).append(" ");
                    String name = f.getName();
                    if (name.startsWith(prefix) && name.endsWith(".txt")) {
                        try {
                            String numStr = name.substring(prefix.length(), name.length() - 4);
                            int n = Integer.parseInt(numStr);
                            if (n > maxN) maxN = n;
                        } catch (Exception ignored) {}
                    }
                }
            }
            add("AppLog: " + sb.toString());
            int nextN = maxN + 1;
            File candidate = new File(dir, prefix + nextN + ".txt");
            while (candidate.exists()) {
                nextN++;
                candidate = new File(dir, prefix + nextN + ".txt");
            }
            currentLogFile = candidate;
            if (!currentLogFile.exists()) currentLogFile.createNewFile();
            add("AppLog: init — выбран файл " + currentLogFile.getName()
                    + " (prefix=" + prefix + ", maxN=" + maxN + ", nextN=" + nextN + ")");
        } catch (Exception e) {
            add("AppLog: ошибка init — " + e.getMessage());
        }
    }

    public static synchronized File getCurrentLogFile() {
        return currentLogFile;
    }

    public static synchronized void add(String message) {
        if (message == null) return;
        String time = sdf.format(new Date());
        String logLine = time + " " + message;
        logs.add(logLine);
        if (logs.size() > 2000) logs.remove(0);

        if (currentLogFile != null) {
            synchronized (fileLock) {
                try {
                    FileOutputStream fos = new FileOutputStream(currentLogFile, true);
                    Writer w = new OutputStreamWriter(fos, "UTF-8");
                    w.write(logLine);
                    w.write("\n");
                    w.flush();
                    w.close();
                } catch (Exception ignored) {}
            }
        }

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