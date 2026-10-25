package com.pmr.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ListFile {
    private final List<Item> items = new ArrayList<>();

    public static class Item {
        public int id;
        public String name;

        public Item(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public synchronized void clear() { this.items.clear(); }

    public synchronized void put(int id, String name) {
        for (Item it : this.items) {
            if (it.id == id) { it.name = name; return; }
        }
        this.items.add(new Item(id, name));
    }

    public synchronized String getName(int id) {
        for (Item it : this.items) {
            if (it.id == id) return it.name;
        }
        return "     ";
    }

    public synchronized List<Item> snapshot() { return new ArrayList<>(this.items); }
    public synchronized int count() { return this.items.size(); }

    public synchronized int load(File f) {
        this.items.clear();
        if (f == null || !f.exists()) return 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                parseLine(line);
            }
            return this.items.size();
        } catch (Exception e) { return 0; }
    }

    public synchronized int loadFromStream(InputStream in) {
        this.items.clear();
        if (in == null) return 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                parseLine(line);
            }
            return this.items.size();
        } catch (Exception e) { return 0; }
    }

    public synchronized void save(File f) {
        if (f == null) return;
        try (FileOutputStream fos = new FileOutputStream(f)) {
            StringBuilder sb = new StringBuilder();
            for (Item it : this.items) {
                sb.append(String.format("%05d", it.id)).append(' ').append(it.name).append('\n');
            }
            fos.write(sb.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {}
    }

    private void parseLine(String line) {
        if (line != null && line.length() >= 6 && line.charAt(5) == ' ') {
            try {
                int id = Integer.parseInt(line.substring(0, 5).trim());
                String name = line.substring(6);
                this.items.add(new Item(id, name));
            } catch (NumberFormatException ignored) {}
        }
    }
}