package com.vagdedes.spartan.functionality.concurrent;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForceThreadList<T> {

    @Getter
    private final List<T> list;
    private final Set<T> clamp;
    private final ExecutorService service;

    public ForceThreadList(List<T> list, ExecutorService service) {
        this.list = Collections.synchronizedList(list);
        this.clamp = Collections.synchronizedSet(new HashSet<>());
        this.service = service;
    }

    public ForceThreadList(List<T> list, int pool) {
        this(list, Executors.newScheduledThreadPool(pool));
    }

    public ForceThreadList(List<T> list) {
        this(list, Executors.newScheduledThreadPool(1));
    }

    public void add(T object) {
        service.submit(() -> {
            synchronized (list) {
                list.add(object);
            }
        });
    }

    public T get(int i) {
        try {
            return service.submit(() -> {
                synchronized (list) {
                    return list.get(i);
                }
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get element", e);
        }
    }

    public void remove(T object) {
        service.submit(() -> {
            synchronized (list) {
                list.remove(object);
            }
        });
    }

    public void clear() {
        service.submit(() -> {
            synchronized (list) {
                list.clear();
            }
        });
    }

    @SafeVarargs
    public final void remove(T... objects) {
        service.submit(() -> {
            synchronized (list) {
                list.removeAll(Arrays.asList(objects));
            }
        });
    }

    public void remove(Set<T> objects) {
        service.submit(() -> {
            synchronized (list) {
                list.removeAll(objects);
            }
        });
    }

    public void removeClamp(T object) {
        service.submit(() -> clamp);
    }
}