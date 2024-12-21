package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.functionality.server.MultiVersion;
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
import java.util.concurrent.ConcurrentHashMap;

public class Event_Chunks implements Listener {

    private static final Map<Long, Long> loaded = new ConcurrentHashMap<>();
    public static final boolean heightSupport = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    private static long hash(World world, int x, int z) {
        return (((31L * x) + z) * 31L) + world.getName().hashCode();
    }

    public static boolean isLoaded(World world, int x, int z) {
        long hash = hash(world, x, z);
        Long time = loaded.get(hash);

        if (time != null) {
            if (time == -1L) {
                return true;
            } else if (time > System.currentTimeMillis()) {
                return false;
            }
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                loaded.put(hash, -1L);
                return true;
            }
        }
        loaded.put(hash, System.currentTimeMillis() + 10_000L);
        return false;
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.put(hash(chunk.getWorld(), chunk.getX(), chunk.getZ()), -1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.remove(hash(chunk.getWorld(), chunk.getX(), chunk.getZ()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldUnload(WorldUnloadEvent e) {
        World world = e.getWorld();
        NPCManager.clear(world);

        for (Chunk chunk : world.getLoadedChunks()) {
            loaded.remove(hash(world, chunk.getX(), chunk.getZ()));
        }
    }

    public static Block getBlockAsync(Location location) {
        if (Event_Chunks.isLoaded(
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
