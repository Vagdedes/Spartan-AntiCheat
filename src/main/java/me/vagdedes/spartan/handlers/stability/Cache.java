package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.checks.combat.VelocityCheck;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.moderation.Debug;
import me.vagdedes.spartan.functionality.moderation.Spectate;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.functionality.protections.PlayerLimitPerIP;
import me.vagdedes.spartan.functionality.protections.ReconnectCooldown;
import me.vagdedes.spartan.functionality.synchronicity.AutoUpdater;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Threads;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Cache {

    private static final List<Object> memory = new CopyOnWriteArrayList<>();
    public static final int gradeDivisor = 1024;
    public static final boolean enoughRAM = (Runtime.getRuntime().maxMemory() / gradeDivisor / gradeDivisor) >= 900;

    public static void clearCheckCache(SpartanPlayer p) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            p.getBuffer(hackType).clear();
            p.getTimer(hackType).clear();
            p.getDecimals(hackType).clear();
            p.getCooldowns(hackType).clear();
            p.getTracker(hackType).clear();
        }
    }

    public static void clearCheckCache(SpartanPlayer p, Iterable<Enums.HackType> hackTypes) {
        for (Enums.HackType hackType : hackTypes) {
            p.getBuffer(hackType).clear();
            p.getTimer(hackType).clear();
            p.getDecimals(hackType).clear();
            p.getCooldowns(hackType).clear();
            p.getTracker(hackType).clear();
        }
    }

    public static void clearStorage(boolean clear) {
        if (!memory.isEmpty()) {
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

    public static void clear(SpartanPlayer p, Player n,
                             boolean leave, boolean distance, boolean deep,
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
        MovementProcessing.resetVehicleTicks(p);
        CombatProcessing.remove(p);

        // Objects
        if (distance) {
            p.getProfile().getCombat().endFights();
        }
        if (deep) {
            clearCheckCache(p);
        }

        // Features
        if (leave) {
            Spectate.remove(p, true);
        }

        // Detections
        if (newLocation != null) {
            p.getExecutor(Enums.HackType.MorePackets).handle(newLocation);
            p.getExecutor(Enums.HackType.IrregularMovements).handle(newLocation);
            p.getExecutor(Enums.HackType.Exploits).handle(newLocation);
        } else {
            p.getExecutor(Enums.HackType.MorePackets).handle(null);
            p.getExecutor(Enums.HackType.IrregularMovements).handle(null);
            p.getExecutor(Enums.HackType.Exploits).handle(null);
        }
        if (leave || distance) {
            p.getExecutor(Enums.HackType.KillAura).handle(null);
        }
        p.getExecutor(Enums.HackType.Criticals).handle(leave);
        p.getExecutor(Enums.HackType.Velocity).handle(VelocityCheck.REFRESH);
    }

    public static <K, V> Map<K, V> store(Map<K, V> map) {
        memory.add(map);
        return map;
    }

    public static <T> List<T> store(List<T> list) {
        memory.add(list);
        return list;
    }

    public static <T> Set<T> store(Set<T> set) {
        memory.add(set);
        return set;
    }

    public static void disable() {
        if (CloudFeature.hasToken()) {
            AutoUpdater.complete();
        }

        // System
        TPS.clear();
        Threads.disable();
        SpartanBukkit.clear();

        // Utilities
        SpartanBukkit.cooldowns.clear();

        // Features
        DetectionNotifications.clear();
        CrossServerInformation.clear();
        PlayerLimitPerIP.clear();
        Spectate.clear();
        ReconnectCooldown.clear();
        LagLeniencies.clear();
        AwarenessNotifications.clear();
        Debug.clear();

        // Configuration
        Config.create();
    }
}
