package com.vagdedes.spartan.functionality.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Threads;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.packet.ProtocolLib;
import com.vagdedes.spartan.functionality.connection.cloud.IDs;
import com.vagdedes.spartan.functionality.connection.cloud.JarVerification;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class SpartanBukkit {

    public static final boolean
            testMode = !JarVerification.enabled && !IDs.isBuiltByBit() && !IDs.isPolymart()
            && Bukkit.getMotd().contains(Register.plugin.getName()),
            supportedFork = MultiVersion.fork().equals("Spigot") || MultiVersion.fork().equals("Paper"),
            canAdvertise = !JarVerification.enabled || IDs.isBuiltByBit() || IDs.isPolymart();

    public static final Threads.ThreadPool
            connectionThread = new Threads.ThreadPool(TPS.tickTime),
            dataThread = new Threads.ThreadPool(1L),
            analysisThread = new Threads.ThreadPool(1L),
            chunkThread = MultiVersion.folia ? null : new Threads.ThreadPool(1L),
            detectionThread = MultiVersion.folia ? null : new Threads.ThreadPool(1L);

    public static final int hashCodeMultiplier = 31;
    private static final Map<UUID, SpartanPlayer> players =
            Collections.synchronizedMap(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final Map<UUID, Player> realPlayers =
            Collections.synchronizedMap(new LinkedHashMap<>(Config.getMaxPlayers()));
    public static final Class<?> craftPlayer = ReflectionUtils.getClass(
            ReflectionUtils.class.getPackage().getName().substring(0, 19) // Package
                    + "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().substring(23) + ".entity.CraftPlayer" // Version
    );

    public static void clear() {
        synchronized (players) {
            players.clear();
        }
        synchronized (realPlayers) {
            realPlayers.clear();
        }
    }

    public static boolean isPlayer(UUID uuid) {
        synchronized (players) {
            return players.containsKey(uuid);
        }
    }

    public static SpartanPlayer getPlayer(String name) {
        if (!players.isEmpty()) {
            name = name.toLowerCase();

            synchronized (players) {
                for (SpartanPlayer p : players.values()) {
                    if (p.name.toLowerCase().equals(name)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public static SpartanPlayer getPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? null : getPlayer(player);
    }

    public static Player getRealPlayer(UUID uuid) {
        Player player = realPlayers.get(uuid);
        return player != null && player.isOnline() ? player : null;
    }

    public static SpartanPlayer getPlayer(Player real) {
        if (!ProtocolLib.isTemporaryPLayer(real)) { // Temporary players have no UUIDs
            UUID uuid = real.getUniqueId();

            synchronized (players) {
                SpartanPlayer player = players.get(uuid);

                if (player == null && real.getAddress() != null) {
                    players.put(uuid, player = new SpartanPlayer(real, uuid));
                }
                return player;
            }
        }
        return null;
    }

    public static SpartanPlayer removePlayer(Player real) {
        synchronized (players) {
            return players.remove(real.getUniqueId());
        }
    }

    public static int getPlayerCount() {
        return players.size();
    }

    public static Set<UUID> getUUIDs() {
        synchronized (players) {
            return new HashSet<>(players.keySet());
        }
    }

    public static List<SpartanPlayer> getPlayers() {
        synchronized (players) {
            return new ArrayList<>(players.values());
        }
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

    public static void runTask(SpartanPlayer player, Runnable runnable) {
        SpartanScheduler.run(player, runnable, false);
    }

    public static void runTask(World world, int x, int z, Runnable runnable) {
        SpartanScheduler.run(world, x, z, runnable, false);
    }

    public static void transferTask(SpartanPlayer player, Runnable runnable) {
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
}
