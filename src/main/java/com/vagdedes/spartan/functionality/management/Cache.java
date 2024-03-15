package com.vagdedes.spartan.functionality.management;

import com.vagdedes.spartan.abstraction.check.Threads;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.AutoUpdater;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.identifiers.simple.DetectionLocation;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

import java.util.*;

public class Cache {

    private static final List<Object> memory = Collections.synchronizedList(new ArrayList<>());

    public static void clearCheckCache(SpartanPlayer p) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            p.getBuffer(hackType).clear();
            p.getTimer(hackType).clear();
            p.getDecimals(hackType).clear();
            p.getCooldowns(hackType).clear();
            p.getTracker(hackType).clear();
        }
    }

    public static void clearStorage(boolean clear) {
        if (!memory.isEmpty()) {
            synchronized (memory) {
                for (Object object : memory) {
                    if (object instanceof Map) {
                        ((Map<?, ?>) object).clear();
                    } else {
                        ((Collection<?>) object).clear();
                    }
                }
                if (clear) {
                    memory.clear();
                }
            }
        }
    }

    public static void clear(SpartanPlayer p, Player n,
                             boolean leave, boolean distance, boolean deep, boolean cancelled,
                             SpartanLocation newLocation) {
        // Utils
        if (!leave) {
            PlayerData.run(p, n, n.getActivePotionEffects());
        }

        // Handlers
        if (distance) {
            if (newLocation != null) {
                DetectionLocation.update(p, newLocation, false);
            } else {
                DetectionLocation.remove(p);
            }
        } else if (leave) {
            DetectionLocation.remove(p);
        }
        p.resetHandlers();
        CombatProcessing.remove(p);

        // Objects
        if (deep) {
            clearCheckCache(p);
        }

        // Detections
        if (newLocation != null) {
            p.getExecutor(Enums.HackType.MorePackets).handle(cancelled, newLocation);
            p.getExecutor(Enums.HackType.IrregularMovements).handle(cancelled, newLocation);
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, newLocation);
        } else {
            p.getExecutor(Enums.HackType.MorePackets).handle(cancelled, null);
            p.getExecutor(Enums.HackType.IrregularMovements).handle(cancelled, null);
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
        }
        if (leave || distance) {
            p.getExecutor(Enums.HackType.KillAura).handle(cancelled, null);
        }
        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, leave);
    }

    public static <K, V> Map<K, V> store(Map<K, V> map) {
        synchronized (memory) {
            memory.add(map);
            return map;
        }
    }

    public static <T> List<T> store(List<T> list) {
        synchronized (memory) {
            memory.add(list);
            return list;
        }
    }

    public static <T> Set<T> store(Set<T> set) {
        synchronized (memory) {
            memory.add(set);
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

        // Configuration
        Config.create();
    }
}
