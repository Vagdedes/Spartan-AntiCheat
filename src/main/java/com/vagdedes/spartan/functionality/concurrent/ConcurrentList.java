package com.vagdedes.spartan.functionality.concurrent;

import lombok.Getter;

import java.util.*;

public class ConcurrentList<T> {

    @Getter
    private final List<T> list; // coverage without override
    private final Set<T> clamp;

    public ConcurrentList(List<T> list) {
        this.list = Collections.synchronizedList(list);
        this.clamp = Collections.synchronizedSet(new HashSet<>());
    }

    public void add(T object) {
        Objects.requireNonNull(object, "Object cannot be null");
        list.add(object);
    }

    public T get(int i) {
        synchronized (list) {
            return list.get(i);
        }
    }

    public void remove(T object) {
        Objects.requireNonNull(object, "Object cannot be null");
        list.remove(object);
    }

    public void clear() {
        list.clear();
    }

    @SafeVarargs
    public final void remove(T... objects) {
        Objects.requireNonNull(objects, "Objects cannot be null");
        synchronized (list) {
            list.removeAll(Arrays.asList(objects));
        }
    }

    public void remove(Set<T> objects) {
        Objects.requireNonNull(objects, "Objects cannot be null");
        synchronized (list) {
            list.removeAll(objects);
        }
    }

    public void removeClamp(T object) {
        Objects.requireNonNull(object, "Object cannot be null");
        clamp.add(object);
    }

    public void applyClamp() {
        synchronized (list) {
            list.removeAll(clamp);
        }
    }
}
