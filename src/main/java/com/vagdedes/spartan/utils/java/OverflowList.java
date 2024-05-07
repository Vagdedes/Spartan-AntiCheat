package com.vagdedes.spartan.utils.java;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OverflowList<E> implements List<E> {

    private final ArrayList<E> list;
    private final int maxSize;
    private int overflow;

    public OverflowList(int size, int maxSize) {
        this.list = new ArrayList<>(size);
        this.maxSize = maxSize;
        this.overflow = 0;
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.list.contains(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return this.list.toArray();
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        return this.list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        if (list.size() == maxSize) {
            this.list.set(this.overflow, e);
            this.overflow++;

            if (this.overflow == this.maxSize) {
                this.overflow = 0;
            }
            return true;
        } else {
            return this.list.add(e);
        }
    }

    @Override
    public boolean remove(Object o) {
        if (this.list.remove(o)) {
            this.overflow = 0;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.list.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (this.list.removeAll(c)) {
            this.overflow = 0;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        if (this.list.retainAll(c)) {
            this.overflow = 0;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        this.list.clear();
        this.overflow = 0;
    }

    @Override
    public boolean equals(Object o) {
        return this.list.equals(o);
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public E get(int index) {
        return this.list.get(index);
    }

    @Override
    public E set(int index, E element) {
        return this.list.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        this.list.add(index, element);
    }

    @Override
    public E remove(int index) {
        return this.list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return this.list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.list.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return this.list.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return this.list.listIterator(index);
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return this.list.subList(fromIndex, toIndex);
    }
}
