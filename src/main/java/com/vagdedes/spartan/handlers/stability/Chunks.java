package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.*;

public class Chunks {

    private static final Map<World, List<Integer>> loadedChunks
            = Collections.synchronizedMap(new LinkedHashMap<>());
    private static boolean looping = false;

    public static void reload(boolean enabledPlugin) {
        if (enabledPlugin && !MultiVersion.folia && !looping) {
            looping = true;
            SpartanBukkit.chunkThread.pause();
            Map<World, List<Integer>> map = new LinkedHashMap<>();

            for (World world : Bukkit.getWorlds()) {
                List<Integer> list = new ArrayList<>();

                for (Chunk chunk : world.getLoadedChunks()) {
                    list.add(chunkIdentifier(chunk.getX(), chunk.getZ()));
                }
                map.put(world, list);
            }
            synchronized (loadedChunks) {
                loadedChunks.clear();
                loadedChunks.putAll(map);
            }
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
        synchronized (loadedChunks) {
            List<Integer> list = loadedChunks.computeIfAbsent(world, k -> new ArrayList<>());
            int hash = chunkIdentifier(x, z);

            if (list.contains(hash)) {
                return true;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk.getX() == x && chunk.getZ() == z) {
                    if (!list.contains(hash)) {
                        list.add(hash);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // Separator

    public static void load(World world, Chunk chunk) {
        synchronized (loadedChunks) {
            List<Integer> list = loadedChunks.computeIfAbsent(world, k -> new ArrayList<>());
            int hash = chunkIdentifier(chunk.getX(), chunk.getZ());

            if (!list.contains(hash)) {
                list.add(hash);
            }
        }
    }

    public static void unload(World world, Chunk chunk) {
        synchronized (loadedChunks) {
            List<Integer> list = loadedChunks.get(world);

            if (list != null) {
                list.remove((Object) chunkIdentifier(chunk.getX(), chunk.getZ()));
            }
        }
    }

    public static void unload(World world) {
        Runnable runnable = () -> {
            synchronized (loadedChunks) {
                loadedChunks.remove(world);
            }
        };

        if (looping) {
            SpartanBukkit.chunkThread.execute(() -> SpartanBukkit.transferTask(runnable));
        } else {
            runnable.run();
        }
    }
}
