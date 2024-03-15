package com.vagdedes.spartan.abstraction.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Timer {

    private final Map<String, Long> storage;

    public Timer() {
        storage = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    public boolean has(long l) {
        return l != Long.MAX_VALUE;
    }

    public boolean has(String name) {
        synchronized (storage) {
            return storage.get(name) != null;
        }
    }

    public long get(String name, long def) {
        Long ms;

        synchronized (storage) {
            ms = storage.get(name);
        }
        return ms != null ? System.currentTimeMillis() - ms : def;
    }

    public long get(String name) {
        return get(name, Long.MAX_VALUE);
    }

    public void set(String name) {
        synchronized (storage) {
            storage.put(name, System.currentTimeMillis());
        }
    }

    public void remove(String name) {
        synchronized (storage) {
            storage.remove(name);
        }
    }

    public void remove(String[] names) {
        synchronized (storage) {
            for (String name : names) {
                storage.remove(name);
            }
        }
    }
}
