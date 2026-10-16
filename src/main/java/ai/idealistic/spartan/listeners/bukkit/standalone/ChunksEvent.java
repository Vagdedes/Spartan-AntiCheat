package ai.idealistic.spartan.listeners.bukkit.standalone;

import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.listeners.NPCManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunksEvent implements Listener {

    private static final Map<UUID, Map<Long, Long>> loaded = new ConcurrentHashMap<>();
    public static final boolean heightSupport = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    private static long getChunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static Map<Long, Long> getWorldCache(World world) {
        return loaded.computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>());
    }

    public static boolean isLoaded(World world, int x, int z) {
        if (world == null) {
            return false;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            return world.isChunkLoaded(x, z);
        }
        Map<Long, Long> worldCache = getWorldCache(world);
        long key = getChunkKey(x, z);
        Long time = worldCache.get(key);

        if (time != null) {
            if (time == -1L) {
                return true;
            } else if (time > System.currentTimeMillis()) {
                return false;
            }
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                worldCache.put(key, -1L);
                return true;
            }
        }
        worldCache.put(key, System.currentTimeMillis() + 10_000L);
        return false;
    }

    public static boolean isLoaded(Block block) {
        return isLoaded(
                block.getWorld(),
                block.getX() >> 4,
                block.getZ() >> 4
        );
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        getWorldCache(chunk.getWorld()).put(getChunkKey(chunk.getX(), chunk.getZ()), -1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        getWorldCache(chunk.getWorld()).remove(getChunkKey(chunk.getX(), chunk.getZ()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldUnload(WorldUnloadEvent e) {
        World world = e.getWorld();
        NPCManager.clear(world);
        loaded.remove(world.getUID()); // Prevent memory leaks on world deletion
    }

    public static Block getBlockAsync(Location location) {
        if (ChunksEvent.isLoaded(
                location.getWorld(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
        )) {
            return location.getWorld().getBlockAt(location);
        } else {
            return null;
        }
    }

}