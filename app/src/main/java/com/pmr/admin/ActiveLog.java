package com.pmr.admin;

import java.util.ArrayList;
import java.util.List;

public class ActiveLog {
    public static final int MAX = 500;
    private final List<String> lines = new ArrayList<>();

    public synchronized void add(String s) {
        if (s == null) return;
        this.lines.add(s);
        if (this.lines.size() > MAX) this.lines.remove(0);
    }

    public synchronized void clear() { this.lines.clear(); }

    public synchronized List<String> tail(int n) {
        int sz = this.lines.size();
        int start = Math.max(0, sz - n);
        return new ArrayList<>(this.lines.subList(start, sz));
    }

    public synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        for (String s : this.lines) sb.append(s).append('\n');
        return sb.toString();
    }

    public synchronized int size() { return this.lines.size(); }
}