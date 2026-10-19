package com.pmr.admin;

import java.util.ArrayList;
import java.util.List;

public class ChanList {
    private final List<Item> items = new ArrayList<>();

    public static class Item {
        public int Id;
        public int ban;
        public int banLocal;
        public int cod;
        public int i;
        public int ip;
        public int port;
    }

    public synchronized void clear() { this.items.clear(); }
    public synchronized void add(Item it) { if (it != null) this.items.add(it); }
    public synchronized List<Item> snapshot() { return new ArrayList<>(this.items); }
    public synchronized int count() { return this.items.size(); }

    public synchronized int idByClient(int client) {
        for (Item it : this.items) {
            if (it.i == client) return it.Id;
        }
        return -1;
    }

    public synchronized Item findByClient(int client) {
        for (Item it : this.items) {
            if (it.i == client) return it;
        }
        return null;
    }
}