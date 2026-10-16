package ai.idealistic.spartan.utils.java;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A list backed by a map of index to element, where reads are lock-free and every
 * modification is synchronized so that indexes always remain contiguous (0 to size - 1).
 */
public class ConcurrentList<E> implements List<E> {

    private final Map<Integer, E> map;

    public ConcurrentList(int size) {
        this.map = new ConcurrentHashMap<>(size);
    }

    public ConcurrentList() {
        this(2);
    }

    // Ordered copy of the current elements
    private List<E> snapshot() {
        int size = this.map.size();
        List<E> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            E e = this.map.get(i);

            if (e != null) {
                list.add(e);
            }
        }
        return list;
    }

    // Replaces all elements while keeping the indexes contiguous
    private void rebuild(List<E> elements) {
        int previousSize = this.map.size();

        for (int i = 0; i < elements.size(); i++) {
            this.map.put(i, elements.get(i));
        }
        for (int i = elements.size(); i < previousSize; i++) {
            this.map.remove(i);
        }
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.map.containsValue(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return this.snapshot().iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.snapshot().toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return this.snapshot().toArray(a);
    }

    @Override
    public synchronized boolean add(E e) {
        this.map.put(this.map.size(), e);
        return true;
    }

    @Override
    public synchronized boolean remove(Object o) {
        int size = this.map.size();

        for (int i = 0; i < size; i++) {
            if (Objects.equals(this.map.get(i), o)) {
                this.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!this.contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized boolean addAll(@NotNull Collection<? extends E> c) {
        for (E e : c) {
            this.add(e);
        }
        return !c.isEmpty();
    }

    @Override
    public synchronized boolean addAll(int index, @NotNull Collection<? extends E> c) {
        for (E e : c) {
            this.add(index++, e);
        }
        return !c.isEmpty();
    }

    @Override
    public synchronized boolean removeAll(@NotNull Collection<?> c) {
        List<E> elements = this.snapshot();
        boolean modified = elements.removeAll(c);

        if (modified) {
            this.rebuild(elements);
        }
        return modified;
    }

    @Override
    public synchronized boolean retainAll(@NotNull Collection<?> c) {
        List<E> elements = this.snapshot();
        boolean modified = elements.retainAll(c);

        if (modified) {
            this.rebuild(elements);
        }
        return modified;
    }

    @Override
    public synchronized void clear() {
        this.map.clear();
    }

    @Override
    public synchronized void sort(Comparator<? super E> c) {
        List<E> elements = this.snapshot();
        elements.sort(c);
        this.rebuild(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        return this.snapshot().equals(o);
    }

    @Override
    public int hashCode() {
        return this.snapshot().hashCode();
    }

    @Override
    public E get(int index) {
        return this.map.get(index);
    }

    @Override
    public synchronized E set(int index, E element) {
        return this.map.put(index, element);
    }

    @Override
    public synchronized void add(int index, E element) {
        for (int i = this.map.size(); i > index; i--) {
            this.map.put(i, this.map.get(i - 1));
        }
        this.map.put(index, element);
    }

    @Override
    public synchronized E remove(int index) {
        int size = this.map.size();
        E removed = this.map.get(index);

        if (index < 0 || index >= size) {
            return null;
        }
        for (int i = index; i < size - 1; i++) {
            this.map.put(i, this.map.get(i + 1));
        }
        this.map.remove(size - 1);
        return removed;
    }

    @Override
    public int indexOf(Object o) {
        int size = this.map.size();

        for (int i = 0; i < size; i++) {
            if (Objects.equals(this.map.get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = this.map.size() - 1; i >= 0; i--) {
            if (Objects.equals(this.map.get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return this.snapshot().listIterator();
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return this.snapshot().listIterator(index);
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return this.snapshot().subList(fromIndex, toIndex);
    }
}
