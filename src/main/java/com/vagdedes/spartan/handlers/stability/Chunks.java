package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Chunks {

    private static final Map<World, CopyOnWriteArrayList<Integer>> loadedChunks = MultiVersion.folia ? null : new ConcurrentHashMap<>();
    private static boolean looping = false;

    public static void reload(boolean enabledPlugin) {
        if (enabledPlugin && !MultiVersion.folia && !looping) {
            looping = true;
            SpartanBukkit.chunkThread.pause();
            Map<World, CopyOnWriteArrayList<Integer>> map = new LinkedHashMap<>();

            for (World world : Bukkit.getWorlds()) {
                CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();

                for (Chunk chunk : world.getLoadedChunks()) {
                    list.add(chunkIdentifier(chunk.getX(), chunk.getZ()));
                }
                map.put(world, list);
            }
            loadedChunks.clear();
            loadedChunks.putAll(map);
            looping = false;
            SpartanBukkit.chunkThread.resume();
        }
    }

    // Separator

    public static int locationIdentifier(int world, int x, int y, int z) {
        world = (SpartanBukkit.hashCodeMultiplier * world) + x;
        world = (SpartanBukkit.hashCodeMultiplier * world) + y;
        return (SpartanBukkit.hashCodeMultiplier * world) + z;
    }

    public static int positionIdentifier(double x, double y, double z) {
        int hash = SpartanBukkit.hashCodeMultiplier + Double.hashCode(x);
        hash = (SpartanBukkit.hashCodeMultiplier * hash) + Double.hashCode(y);
        return (SpartanBukkit.hashCodeMultiplier * hash) + Double.hashCode(z);
    }

    private static int chunkIdentifier(int x, int z) {
        return (SpartanBukkit.hashCodeMultiplier * x) + z;
    }

    // Separator

    public static boolean isLoaded(World world, int x, int z) {
        CopyOnWriteArrayList<Integer> list = loadedChunks.computeIfAbsent(world, k -> new CopyOnWriteArrayList<>());
        int hash = chunkIdentifier(x, z);

        if (list.contains(hash)) {
            return true;
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                list.addIfAbsent(hash);
                return true;
            }
        }
        return false;
    }

    // Separator

    public static void load(World world, Chunk chunk) {
        CopyOnWriteArrayList<Integer> list = loadedChunks.computeIfAbsent(world, k -> new CopyOnWriteArrayList<>());
        list.addIfAbsent(chunkIdentifier(chunk.getX(), chunk.getZ()));
    }

    public static void unload(World world, Chunk chunk) {
        List<Integer> list = loadedChunks.get(world);

        if (list != null) {
            list.remove((Object) chunkIdentifier(chunk.getX(), chunk.getZ()));
        }
    }

    public static void unload(World world) {
        Runnable runnable = () -> loadedChunks.remove(world);

        if (looping) {
            SpartanBukkit.chunkThread.execute(() -> SpartanBukkit.transferTask(runnable));
        } else {
            runnable.run();
        }
    }
}
