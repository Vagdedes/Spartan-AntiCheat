package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.utils.java.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Tracker {

    private final Map<String, List<String>> hm;

    public Tracker(boolean async) {
        hm = (async && !MultiVersion.folia ? new ConcurrentHashMap<>() : new LinkedHashMap<>());
    }

    public void clear() {
        hm.clear();
    }

    public void remove(String name) {
        hm.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            remove(name);
        }
    }

    public void add(String name, String content) {
        List<String> array = hm.get(name);

        if (array != null) {
            array.add(content);
        } else {
            array = new CopyOnWriteArrayList<>();
            array.add(content);
            hm.put(name, array);
        }
    }

    public void addIfAbsent(String name, String content) {
        List<String> array = hm.get(name);

        if (array != null) {
            if (!array.contains(content)) {
                array.add(content);
            }
        } else {
            array = new CopyOnWriteArrayList<>();
            array.add(content);
            hm.put(name, array);
        }
    }

    public void remove(String name, String content) {
        List<String> array = hm.get(name);

        if (array != null && array.remove(content) && array.isEmpty()) {
            hm.remove(name);
        }
    }

    public int getCount(String name) {
        List<String> array = hm.get(name);
        return array == null ? 0 : array.size();
    }

    public Map<String, Integer> getMap(String name) {
        List<String> array = hm.get(name);

        if (array != null) {
            int size = array.size();

            if (size > 0) {
                Map<String, Integer> map = new LinkedHashMap<>(size);

                for (String subKey : array) {
                    Integer integer = map.get(subKey);

                    if (integer != null) {
                        map.put(subKey, integer + 1);
                    } else {
                        map.put(subKey, 1);
                    }
                }
            }
        }
        return new LinkedHashMap<>(0);
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
