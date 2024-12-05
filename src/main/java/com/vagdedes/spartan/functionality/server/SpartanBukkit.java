package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Threads;
import com.vagdedes.spartan.compatibility.Compatibility;
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
            SpartanProtocol protocol = playerProtocol.get(player.getUniqueId());

            if (protocol == null) {
                protocol = new SpartanProtocol(player);
                playerProtocol.put(player.getUniqueId(), protocol);
            }
            protocol.spartan.setLastInteraction();
            return protocol;
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

    public static boolean isOnline(UUID uuid) {
        return playerProtocol.containsKey(uuid);
    }

    public static SpartanProtocol deleteProtocol(Player player) {
        if (ProtocolLib.isTemporary(player)) {
            return null;
        } else {
            SpartanProtocol protocol = playerProtocol.remove(player.getUniqueId());

            if (protocol != null) {
                protocol.spartan.setLastInteraction();
                protocol.getProfile().setOnlineFor(
                        System.currentTimeMillis(),
                        protocol.getTimePlayed(),
                        true
                );
            }
            return protocol;
        }
    }

    // Separator

    public static Object runDelayedTask(SpartanProtocol protocol, Runnable runnable, long start) {
        return SpartanScheduler.schedule(protocol, runnable, start, -1L);
    }

    public static Object runRepeatingTask(SpartanProtocol protocol, Runnable runnable, long start, long repetition) {
        return SpartanScheduler.schedule(protocol, runnable, start, repetition);
    }

    public static Object runDelayedTask(Runnable runnable, long start) {
        return SpartanScheduler.schedule(null, runnable, start, -1L);
    }

    public static Object runRepeatingTask(Runnable runnable, long start, long repetition) {
        return SpartanScheduler.schedule(null, runnable, start, repetition);
    }

    public static void runTask(SpartanProtocol protocol, Runnable runnable) {
        SpartanScheduler.run(protocol, runnable, false);
    }

    public static void runTask(World world, int x, int z, Runnable runnable) {
        SpartanScheduler.run(world, x, z, runnable, false);
    }

    public static void transferTask(SpartanProtocol protocol, Runnable runnable) {
        SpartanScheduler.run(protocol, runnable, true);
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

    public static void runCommand(String command) {
        Bukkit.getScheduler().runTask(
                Register.plugin,
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        );
    }

    // Separator

    public static void disable() {
        playerProtocol.clear();
        Threads.disable();
        NPCManager.clear();
        Config.create();
    }
}