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

    public static synchronized void init(Context ctx) {
        if (ctx == null) return;
        try {
            File dir = new File(ctx.getFilesDir(), "logs");
            if (!dir.exists()) dir.mkdirs();
            int maxN = 0;
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.startsWith("log_") && name.endsWith(".txt")) {
                        try {
                            int n = Integer.parseInt(name.substring(4, name.length() - 4));
                            if (n > maxN) maxN = n;
                        } catch (Exception ignored) {}
                    }
                }
            }
            currentLogFile = new File(dir, "log_" + (maxN + 1) + ".txt");
            if (!currentLogFile.exists()) currentLogFile.createNewFile();
            add("AppLog: init, файл = " + currentLogFile.getAbsolutePath());
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

        // запись в файл
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