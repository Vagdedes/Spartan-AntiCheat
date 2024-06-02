package com.vagdedes.spartan.functionality.management;

import com.vagdedes.spartan.abstraction.check.Threads;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.AutoUpdater;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class Cache {

    public static final int
            maxBytes = AlgebraUtils.integerRound(Runtime.getRuntime().maxMemory() * 0.05),
            maxRows = maxBytes / 1024;
    private static final Collection<Object> memoryList = Collections.synchronizedList(new ArrayList<>());

    public static void clearStorage(boolean clear) {
        if (!memoryList.isEmpty()) {
            synchronized (memoryList) {
                for (Object object : memoryList) {
                    if (object instanceof Map) {
                        ((Map<?, ?>) object).clear();
                    } else {
                        ((Collection<?>) object).clear();
                    }
                }
                if (clear) {
                    memoryList.clear();
                }
            }
        }
    }

    public static <K, V> Map<K, V> store(Map<K, V> map) {
        synchronized (memoryList) {
            memoryList.add(map);
            return map;
        }
    }

    public static <T> List<T> store(List<T> list) {
        synchronized (memoryList) {
            memoryList.add(list);
            return list;
        }
    }

    public static <T> Set<T> store(Set<T> set) {
        synchronized (memoryList) {
            memoryList.add(set);
            return set;
        }
    }

    public static void disable() {
        AutoUpdater.complete();

        // System
        TPS.clear();
        Threads.disable();
        SpartanBukkit.clear();

        // Features
        DetectionNotifications.clear();
        CrossServerInformation.clear();
        PlayerLimitPerIP.clear();
        NPCManager.clear();

        // Configuration
        Config.create();
    }
}
