package me.vagdedes.spartan.objects.data;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.utils.java.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Timer {

    private final Map<String, Long> hm;

    public Timer(boolean async) {
        hm = (async && !MultiVersion.folia ? new ConcurrentHashMap<>() : new LinkedHashMap<>());
    }

    public void clear() {
        hm.clear();
    }

    public boolean has(long l) {
        return l != Long.MAX_VALUE;
    }

    public boolean has(String name) {
        return hm.get(name) != null;
    }

    public long get(String name, long def) {
        Long ms = hm.get(name);
        return ms != null ? System.currentTimeMillis() - ms : def;
    }

    public long get(String name) {
        return get(name, Long.MAX_VALUE);
    }

    public void set(String name) {
        hm.put(name, System.currentTimeMillis());
    }

    public void set(String name, long ms, boolean future) {
        hm.put(name, System.currentTimeMillis() + (future ? ms : -ms));
    }

    public void remove(String name) {
        hm.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            remove(name);
        }
    }

    public void clear(String[] ignore) {
        if (!hm.isEmpty()) {
            List<String> internal = new ArrayList<>();

            for (String name : hm.keySet()) {
                if (!StringUtils.stringContainsPartOfArray(ignore, name)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }

    public void clear(String s) {
        if (!hm.isEmpty()) {
            List<String> internal = new ArrayList<>();

            for (String name : hm.keySet()) {
                if (name.contains(s)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }
}
