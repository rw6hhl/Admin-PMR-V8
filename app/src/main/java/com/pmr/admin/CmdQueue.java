package com.pmr.admin;

import java.util.LinkedList;
import java.util.Queue;

public class CmdQueue {
    private static final int MAX = 32;
    private final Queue<String> q = new LinkedList<>();

    public synchronized boolean push(String cmd) {
        if (cmd == null) return false;
        if (this.q.size() >= MAX) return false;
        this.q.offer(cmd);
        return true;
    }

    public synchronized String pop() { return this.q.poll(); }
    public synchronized void clear() { this.q.clear(); }
    public synchronized int size() { return this.q.size(); }
}