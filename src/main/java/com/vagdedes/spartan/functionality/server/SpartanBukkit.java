package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Threads;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpartanBukkit {

    public static final Threads.ThreadPool
            connectionThread = new Threads.ThreadPool(TPS.tickTime),
            sqlThread = new Threads.ThreadPool(TPS.tickTime),
            dataThread = new Threads.ThreadPool(1L),
            analysisThread = new Threads.ThreadPool(1L);

    public static final int
            hashCodeMultiplier = 31,
            maxBytes = AlgebraUtils.integerRound(Runtime.getRuntime().maxMemory() * 0.05),
            maxSQLRows = maxBytes / 1024;
    private static final Map<UUID, SpartanProtocol> playerProtocol = new ConcurrentHashMap<>();
    public static final Class<?> craftPlayer = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
            ? null
            : ReflectionUtils.getClass(
            ReflectionUtils.class.getPackage().getName().substring(0, 19) // Package
                    + "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().substring(23) + ".entity.CraftPlayer" // Version
    );

    public static int getPlayerCount() {
        return playerProtocol.size();
    }

    public static boolean hasPlayerCount() {
        return !playerProtocol.isEmpty();
    }

    public static List<SpartanProtocol> getProtocols() {
        return new ArrayList<>(playerProtocol.values());
    }

    public static Set<Map.Entry<UUID, SpartanProtocol>> getPlayerEntries() {
        return playerProtocol.entrySet();
    }

    // Separator

    public static boolean isSynchronised() {
        return Bukkit.isPrimaryThread()
                || !Register.isPluginEnabled();
    }

    // Separator

    public static Object getCraftPlayerMethod(Player p, String path) {
        if (craftPlayer != null) {
            try {
                Object handle = craftPlayer.getMethod("getHandle", new Class[0]).invoke(p, new Object[0]);
                return handle.getClass().getDeclaredField(path).get(handle);
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    // Separator

    public static boolean packetsEnabled() {
        return Compatibility.CompatibilityType.PROTOCOL_LIB.isFunctional();
    }

    public static SpartanProtocol getProtocol(Player player) {
        if (ProtocolLib.isTemporary(player)) {
            return new SpartanProtocol(player);
        } else {
            return playerProtocol.computeIfAbsent(
                    player.getUniqueId(),
                    k -> new SpartanProtocol(player)
            );
        }
    }

    public static SpartanProtocol getProtocol(String name) {
        if (!playerProtocol.isEmpty()) {
            for (SpartanProtocol protocol : playerProtocol.values()) {
                if (protocol.bukkit.getName().equals(name)) {
                    return protocol;
                }
            }
        }
        return null;
    }

    public static SpartanProtocol getAnyCaseProtocol(String name) {
        if (!playerProtocol.isEmpty()) {
            for (SpartanProtocol protocol : playerProtocol.values()) {
                if (protocol.bukkit.getName().equalsIgnoreCase(name)) {
                    return protocol;
                }
            }
        }
        return null;
    }

    public static SpartanProtocol getProtocol(int entityID) {
        for (SpartanProtocol protocol : playerProtocol.values()) {
            if (protocol.spartan.getEntityId() == entityID) {
                return protocol;
            }
        }
        return null;
    }

    public static SpartanProtocol getProtocol(UUID uuid) {
        if (isSynchronised()) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                return getProtocol(player);
            } else {
                return playerProtocol.get(uuid);
            }
        } else {
            return playerProtocol.get(uuid);
        }
    }

    public static SpartanProtocol deleteProtocol(Player player) {
        if (ProtocolLib.isTemporary(player)) {
            return null;
        } else {
            return playerProtocol.remove(player.getUniqueId());
        }
    }

    // Separator

    public static Object runDelayedTask(SpartanPlayer player, Runnable runnable, long start) {
        return SpartanScheduler.schedule(player, runnable, start, -1L);
    }

    public static Object runRepeatingTask(SpartanPlayer player, Runnable runnable, long start, long repetition) {
        return SpartanScheduler.schedule(player, runnable, start, repetition);
    }

    public static Object runDelayedTask(Runnable runnable, long start) {
        return SpartanScheduler.schedule(null, runnable, start, -1L);
    }

    public static Object runRepeatingTask(Runnable runnable, long start, long repetition) {
        return SpartanScheduler.schedule(null, runnable, start, repetition);
    }

    public static void runTask(Player player, Runnable runnable) {
        SpartanScheduler.run(player, runnable, false);
    }

    public static void runTask(SpartanPlayer player, Runnable runnable) {
        SpartanScheduler.run(player, runnable, false);
    }

    public static void runTask(World world, int x, int z, Runnable runnable) {
        SpartanScheduler.run(world, x, z, runnable, false);
    }

    public static void transferTask(SpartanPlayer player, Runnable runnable) {
        SpartanScheduler.run(player, runnable, true);
    }

    public static void transferTask(Player player, Runnable runnable) {
        SpartanScheduler.run(player, runnable, true);
    }

    public static void transferTask(World world, int x, int z, Runnable runnable) {
        SpartanScheduler.run(world, x, z, runnable, true);
    }

    public static void transferTask(Runnable runnable) {
        SpartanScheduler.transfer(runnable);
    }

    public static void cancelTask(Object task) {
        SpartanScheduler.cancel(task);
    }

    // Separator

    public static void disable() {
        playerProtocol.clear();
        Threads.disable();
        NPCManager.clear();
        Config.create();
    }
}