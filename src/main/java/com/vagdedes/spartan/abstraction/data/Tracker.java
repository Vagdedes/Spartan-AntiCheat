package com.vagdedes.spartan.abstraction.data;

import java.util.*;

public class Tracker {

    private final Map<String, List<String>> storage;

    public Tracker() {
        storage = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public void clear() {
        synchronized (storage) {
            storage.clear();
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

    public void add(String name, String content) {
        synchronized (storage) {
            List<String> array = storage.get(name);

            if (array != null) {
                array.add(content);
            } else {
                array = new ArrayList<>();
                array.add(content);
                storage.put(name, array);
            }
        }
    }

    public void addIfAbsent(String name, String content) {
        synchronized (storage) {
            List<String> array = storage.get(name);

            if (array != null) {
                if (!array.contains(content)) {
                    array.add(content);
                }
            } else {
                array = new ArrayList<>();
                array.add(content);
                storage.put(name, array);
            }
        }
    }

    public void remove(String name, String content) {
        synchronized (storage) {
            List<String> array = storage.get(name);

            if (array != null && array.remove(content) && array.isEmpty()) {
                storage.remove(name);
            }
        }
    }

    public int getCount(String name) {
        synchronized (storage) {
            List<String> array = storage.get(name);
            return array == null ? 0 : array.size();
        }
    }

    public Map<String, Integer> getMap(String name) {
        synchronized (storage) {
            List<String> array = storage.get(name);

            if (array != null) {
                int size = array.size();

                if (size > 0) {
                    Map<String, Integer> map = new LinkedHashMap<>(size);

                    for (String subKey : array) {
                        map.merge(subKey, 1, Integer::sum);
                    }
                    return map;
                }
            }
            return new LinkedHashMap<>(0);
        }
    }
}
