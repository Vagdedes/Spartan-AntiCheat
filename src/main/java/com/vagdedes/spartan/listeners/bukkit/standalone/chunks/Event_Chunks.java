package com.vagdedes.spartan.listeners.bukkit.standalone.chunks;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.Event_World;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Event_Chunks implements Listener {

    private static final Map<Long, Boolean> loaded = new LinkedHashMap<>();
    static final Map<World, Map<Long, ChunkData>> map = new LinkedHashMap<>();
    public static final boolean heightSupport = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    // Separator

    static final class ChunkData {

        private long creation;
        private boolean queued;
        ChunkSnapshot snapshot;

        private ChunkData() {

        }

        private boolean refresh(Chunk chunk) {
            synchronized (this) {
                if (!this.queued
                        && System.currentTimeMillis() - this.creation > 500L) {
                    this.queued = true;
                    this.creation = System.currentTimeMillis();
                    this.snapshot = chunk.getChunkSnapshot();
                    this.queued = false;
                    return true;
                }
            }
            return false;
        }

        private boolean isQueued() {
            synchronized (this) {
                return this.queued;
            }
        }

    }

    // Separator

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            if (SpartanBukkit.packetsEnabled()
                    && SpartanBukkit.hasPlayerCount()
                    && isEmptyQueue()) {
                for (SpartanProtocol protocol : SpartanBukkit.getProtocols()) {
                    SpartanLocation location = protocol.spartanPlayer.movement.getLocation();

                    if (isLoaded(location.world, location.getChunkX(), location.getChunkZ())
                            && cache(location.getBukkitLocation().getChunk(), true)) {
                        break;
                    }
                }
            }
        }, 1L, 1L);
    }

    // Separator

    public static long hashCoordinates(int x, int z) {
        return (31L * x) + z;
    }

    public static long hash(World world, int x, int z) {
        return (hashCoordinates(x, z) * 31L) + world.getName().hashCode();
    }

    // Separator

    public static void clear() {
        map.clear();
    }

    public static boolean isLoaded(World world, int x, int z) {
        if (loaded.containsKey(hash(world, x, z))) {
            return true;
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                loaded.put(hash(chunk.getWorld(), chunk.getX(), chunk.getZ()), true);
                return true;
            }
        }
        return false;
    }

    public static Material getBlockType(World world, int x, int y, int z) {
        if (y < (heightSupport ? world.getMinHeight() : 0)
                || y >= (heightSupport ? world.getMaxHeight() : PlayerUtils.height)) {
            return null;
        }
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            return null;
        }
        ChunkData data = subMap.get(hashCoordinates(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

        if (data == null || data.snapshot == null) {
            return null;
        }
        return (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10))
                        ? data.snapshot.getBlockType(x & 0xF, y, z & 0xF)
                        : (getBlockAsync(new Location(world, x, y, z)) == null)
                        ? Material.AIR : getBlockAsync(new Location(world, x, y, z)).getType();
    }

    private static boolean hasPlayers(World world, int x, int z) {
        Map<Long, List<Entity>> perChunk = Event_World.getEntities(world);

        if (perChunk != null) {
            List<Entity> list = perChunk.get(Event_Chunks.hashCoordinates(x, z));

            if (list != null) {
                for (Entity entity : list) {
                    if (entity instanceof Player) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean cache(Chunk chunk, boolean force) {
        World world = chunk.getWorld();

        if (!force && !hasPlayers(world, chunk.getX(), chunk.getZ())) {
            return false;
        }
        Map<Long, ChunkData> subMap = map.computeIfAbsent(
                world,
                k -> new LinkedHashMap<>()
        );
        return subMap.computeIfAbsent(
                hashCoordinates(chunk.getX(), chunk.getZ()),
                k -> new ChunkData()
        ).refresh(chunk);
    }

    private static boolean isEmptyQueue() {
        if (!map.isEmpty()) {
            for (Map<Long, ChunkData> subMap : map.values()) {
                for (ChunkData data : subMap.values()) {
                    if (data.isQueued()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.put(hash(chunk.getWorld(), chunk.getX(), chunk.getZ()), true);

        if (SpartanBukkit.packetsEnabled()) {
            cache(chunk, false);
        } else {
            map.remove(chunk.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.remove(hash(chunk.getWorld(), chunk.getX(), chunk.getZ()));

        if (SpartanBukkit.packetsEnabled()) {
            World world = chunk.getWorld();
            Map<Long, ChunkData> subMap = map.get(world);

            if (subMap != null
                    && subMap.remove(hashCoordinates(chunk.getX(), chunk.getZ())) != null
                    && subMap.isEmpty()) {
                map.remove(world);
            }
        } else {
            map.remove(chunk.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldUnload(WorldUnloadEvent e) {
        World world = e.getWorld();
        Map<Long, ChunkData> subMap = map.remove(world);

        if (subMap != null) {
            for (ChunkData data : subMap.values()) {
                loaded.remove(hash(world, data.snapshot.getX(), data.snapshot.getZ()));
            }
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            loaded.remove(hash(world, chunk.getX(), chunk.getZ()));
        }
    }

    public static Block getBlockAsync(final Location location) {
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
