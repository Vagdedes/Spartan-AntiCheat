package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Chunks {

    private static final Map<World, List<Integer>> loadedChunks = new ConcurrentHashMap<>();
    private static boolean looping = false;

    public static void reload(boolean enabledPlugin) {
        if (!MultiVersion.folia) {
            loadedChunks.clear();

            if (enabledPlugin) {
                looping = true;

                for (World world : Bukkit.getWorlds()) {
                    load(world);
                }
                looping = false;
            }
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

    private static void add(List<Integer> list, boolean isNull, World world, int hash) {
        if (isNull) {
            list = loadedChunks.get(world);

            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                loadedChunks.put(world, list);
            }
        }
        if (looping) {
            ((CopyOnWriteArrayList<Integer>) list).addIfAbsent(hash);
        } else {
            list.add(hash);
        }
    }

    // Separator

    public static boolean isLoaded(World world, int x, int z) {
        List<Integer> list = loadedChunks.get(world);
        boolean isNull = list == null;
        int hash = chunkIdentifier(x, z);

        if (!isNull && list.contains(hash)) {
            return true;
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                add(list, isNull, world, hash);
                return true;
            }
        }
        return false;
    }

    // Separator

    public static void load(World world, Chunk chunk) {
        List<Integer> list = loadedChunks.get(world);

        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            loadedChunks.put(world, list);
        }
        list.add(chunkIdentifier(chunk.getX(), chunk.getZ()));
    }

    private static void load(World world) {
        List<Integer> list = new CopyOnWriteArrayList<>();
        loadedChunks.put(world, list);

        for (Chunk chunk : world.getLoadedChunks()) {
            list.add(chunkIdentifier(chunk.getX(), chunk.getZ()));
        }
    }

    // Separator

    public static void unload(World world, Chunk chunk) {
        List<Integer> list = loadedChunks.get(world);

        if (list != null) {
            list.remove((Object) chunkIdentifier(chunk.getX(), chunk.getZ()));
        }
    }
}
