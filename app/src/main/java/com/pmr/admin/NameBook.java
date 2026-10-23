package com.pmr.admin;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class NameBook {
    private static final String PREFS = "pmr_namebook";
    private static final String KEY_NAMES = "names";
    private static final Map<Integer, String> defaultNames = new LinkedHashMap<>();

    static {
        // V9.73: нулевой индекс пустой (""), как в admin.c. Загрузка из list.txt перекрывает эти значения.
        defaultNames.put(0, "");
        defaultNames.put(33333, "R6LQR");
        defaultNames.put(14465, "R6OHE");
        defaultNames.put(20029, "PMR ");
        defaultNames.put(20093, "PMR ");
        defaultNames.put(20095, "PMR ");
        defaultNames.put(20112, "PMR ");
        defaultNames.put(26000, "RW6HHL");
        defaultNames.put(26001, "R6FFM ");
        defaultNames.put(26002, "RW6HHL");
        defaultNames.put(26003, "RW6HHL ");
        defaultNames.put(26004, "UC7G");
        defaultNames.put(26015, "R6FOB");
        defaultNames.put(26016, "UB6HRW");
        defaultNames.put(26039, "KMV145300 ");
        defaultNames.put(26144, "Мин-Воды");
        defaultNames.put(26145, "Мин-Воды");
        defaultNames.put(26431, "KMV431425 ");
        defaultNames.put(26439, "KMV439025 ");
        defaultNames.put(57701, "R6LMT");
        defaultNames.put(57702, "R6LQR");
        defaultNames.put(57703, "RW6HHL");
        defaultNames.put(44444, "KMV");
    }

    public static synchronized String getName(Context ctx, int id) {
        Map<Integer, String> map = load(ctx);
        String name = map.get(id);
        if (name == null) return ""; // V9.72: нет в справочнике — пустая строка (как в admin.c)
        return name;
    }

    public static synchronized Map<Integer, String> load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_NAMES, null);
        Map<Integer, String> map = new LinkedHashMap<>();
        if (raw == null) {
            map.putAll(defaultNames);
            return map;
        }
        for (String line : raw.split(";")) {
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            try {
                int id = Integer.parseInt(line.substring(0, eq).trim());
                map.put(id, line.substring(eq + 1));
            } catch (Exception ignored) {}
        }
        return map;
    }

    public static synchronized void save(Context ctx, Map<Integer, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(';');
        }
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_NAMES, sb.toString()).apply();
    }

    public static synchronized int importFromStream(Context ctx, InputStream is) throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        int lineNo = 0;
        while ((line = br.readLine()) != null) {
            lineNo++;
            line = line.replace("\uFEFF", "").trim(); // BOM-фикс
            if (line.isEmpty() || line.startsWith("#")) continue;
            String idStr;
            String name;
            if (line.contains(";")) {
                int p = line.indexOf(';');
                idStr = line.substring(0, p).trim();
                name = line.substring(p + 1).trim();
            } else if (line.contains("=")) {
                int p = line.indexOf('=');
                idStr = line.substring(0, p).trim();
                name = line.substring(p + 1).trim();
            } else {
                int sp = line.indexOf(' ');
                if (sp <= 0) continue;
                idStr = line.substring(0, sp).trim();
                name = line.substring(sp + 1).trim();
            }
            try {
                int id = Integer.parseInt(idStr);
                map.put(id, name);
            } catch (Exception ignored) {
                AppLog.add("NameBook: строка " + lineNo + " — не число: " + idStr);
            }
        }
        br.close();
        if (map.isEmpty()) return 0;
        save(ctx, map);
        AppLog.add("NameBook: импорт завершён, записей=" + map.size());
        return map.size();
    }
}