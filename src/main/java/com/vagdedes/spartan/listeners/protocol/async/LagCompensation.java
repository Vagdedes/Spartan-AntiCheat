package com.vagdedes.spartan.listeners.protocol.async;

import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.Location;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LagCompensation {

    private static final Map<Integer, LinkedList<Location>> cache =
            new OverflowMap<>(new ConcurrentHashMap<>(), 1_000);
    private static final Map<Integer, Long> lastPacket =
            new OverflowMap<>(new ConcurrentHashMap<>(), 1_000);
    private static final Map<Integer, List<Integer>> delays =
            new OverflowMap<>(new ConcurrentHashMap<>(), 1_000);

    public static Map<Integer, LinkedList<Location>> getCache() {
        return cache;
    }

    public static void add(int id, Location location) {
        cache.computeIfAbsent(
                id,
                k -> new LinkedList<>()
        ).addFirst(location);
        List<Integer> delaysList = delays.computeIfAbsent(
                id,
                k -> new LinkedList<>()
        );
        delaysList.add((int) (System.currentTimeMillis() - lastPacket.getOrDefault(id, System.currentTimeMillis())));
        lastPacket.put(id, System.currentTimeMillis());
        newPacket(id);

        if (delaysList.size() == 10) {
            LinkedList<Integer> l = new LinkedList<>();
            l.add(Collections.max(delaysList) / 2);
            delays.put(id, l);
        }
    }

    public static void newPacket(int id) {
        lastPacket.put(id, System.currentTimeMillis());
    }

    public static Location getLocationWithTickRollback(int id, int ticks) {
        LinkedList<Location> locationStack = cache.get(id);

        if (locationStack == null || locationStack.isEmpty()) {
            return null;
        } else {
            return locationStack.get((ticks >= locationStack.size()) ? locationStack.size() - 1 : ticks);
        }
    }

    public static int getPlayerTicksDelay(int id) {
        List<Integer> delaysList = delays.get(id);
        return delaysList != null
                ? Collections.max(delaysList) / TPS.tickTimeInteger
                : 0;
    }

}
