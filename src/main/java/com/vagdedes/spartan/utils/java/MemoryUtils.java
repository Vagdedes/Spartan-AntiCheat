package com.vagdedes.spartan.utils.java;

import com.vagdedes.spartan.system.SpartanBukkit;

import java.util.Collection;

public class MemoryUtils {

    public static int getNewestToOldestPosition(int size, int index) {
        return size - index - 1;
    }

    public static <E> int fastHashCode(Collection<E> collection) {
        int size = collection.size();
        return size > 0 ? (size * SpartanBukkit.hashCodeMultiplier) + collection.iterator().next().hashCode() : 1;
    }
}
