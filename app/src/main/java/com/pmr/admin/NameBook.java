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
        defaultNames.put(0, "     ");
        defaultNames.put(11111, "145.025 приемник ростовского репитера");
        defaultNames.put(11100, "145.625 передатчик ростовского репитера");
        defaultNames.put(20095, "Константиновский р-н Денис");
        defaultNames.put(11122, "145.625 тон 88.5 Каменск-Шахтинский");
        defaultNames.put(11121, "145.400 Миллерово");
        defaultNames.put(11777, "Админ");
        defaultNames.put(11000, "Шарманка");
        defaultNames.put(26003, "145.300 Мин. Воды");
        defaultNames.put(26002, "145.000 Пятигорск Приемник");
        defaultNames.put(26004, "UC7G");
        defaultNames.put(11105, "145.550 тон 88.5 Мучкап Тамбовская обл");
        defaultNames.put(20112, "PMR Миллерово");
        defaultNames.put(20029, "PMR Ростов-на-Дону");
        defaultNames.put(26001, "Георгиевск");
        defaultNames.put(20093, "PMR Пятигорск");
        defaultNames.put(26000, "RW6HHL");
    }

    public static synchronized String getName(Context ctx, int id) {
        Map<Integer, String> map = load(ctx);
        String name = map.get(id);
        if (name == null || name.trim().isEmpty()) return "Абонент";
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