package com.vagdedes.spartan.listeners.bukkit.standalone.chunks;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Event_Chunks implements Listener {

    private static final long enabledPastTime = 60_000L;
    private static long enabled = 0L;
    private static final Map<Long, Long> loaded = new LinkedHashMap<>();
    static final Map<World, Map<Long, ChunkData>> map = new LinkedHashMap<>();
    public static final boolean heightSupport = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    // Separator

    static final class ChunkData {

        private long creation, lastAccess;
        private boolean queued;
        ChunkSnapshot snapshot;

        private ChunkData() {
        }

        void tick() {
            this.lastAccess = System.currentTimeMillis();
        }

        private boolean refresh(Chunk chunk) {
            if (!this.queued) {
                synchronized (this) {
                    this.queued = true;

                    if (System.currentTimeMillis() - this.creation >= 500L) {
                        this.snapshot = chunk.getChunkSnapshot();
                        this.creation = System.currentTimeMillis();
                        this.queued = false;
                        return true;
                    } else {
                        this.queued = false;
                    }
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
                    && SpartanBukkit.hasPlayerCount()) {
                if (isEnabled() && isEmptyQueue()) {
                    for (SpartanProtocol protocol : SpartanBukkit.getProtocols()) {
                        Location location = protocol.getLocation();

                        if (isLoaded(
                                location.getWorld(),
                                SpartanLocation.getChunkPos(location.getBlockX()),
                                SpartanLocation.getChunkPos(location.getBlockZ())
                        )
                                && rawCache(location.getChunk(), true)) {
                            break;
                        }
                    }
                }
            } else {
                enabled = 0L;
            }
        }, 1L, 1L);
    }

    // Separator

    static void enable() {
        enabled = System.currentTimeMillis();
    }

    private static boolean isEnabled() {
        return System.currentTimeMillis() - enabled <= enabledPastTime;
    }

    public static long hashCoordinates(int x, int z) {
        return (31L * x) + z;
    }

    private static long hash(World world, int x, int z) {
        return (hashCoordinates(x, z) * 31L) + world.getName().hashCode();
    }

    // Separator

    public static void clear() {
        map.clear();
        enabled = 0L;
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
        loaded.put(hash, System.currentTimeMillis() + enabledPastTime);
        return false;
    }

    public static Material getBlockType(World world, int x, int y, int z) {
        Event_Chunks.enable();

        if (y < (heightSupport ? world.getMinHeight() : 0)
                || y >= (heightSupport ? world.getMaxHeight() : PlayerUtils.height)) {
            return null;
        }
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            return null;
        }
        ChunkData data = subMap.get(hashCoordinates(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

        if (data == null) {
            return null;
        }
        data.tick();

        if (data.snapshot == null) {
            return null;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10)) {
            return data.snapshot.getBlockType(x & 0xF, y, z & 0xF);
        } else {
            Block block = getBlockAsync(new Location(world, x, y, z));
            return block == null ? Material.AIR : block.getType();
        }
    }

    private static boolean hasPlayers(World world, int x, int z) {
        List<SpartanProtocol> players = SpartanBukkit.getProtocols();

        if (!players.isEmpty()) {
            for (SpartanProtocol protocol : players) {
                if (protocol.spartan.getWorld().equals(world)) {
                    Location location = protocol.getLocation();

                    if (SpartanLocation.getChunkPos(location.getBlockX()) == x
                            && SpartanLocation.getChunkPos(location.getBlockZ()) == z) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean rawCache(Chunk chunk, boolean force) {
        World world = chunk.getWorld();

        if (!force && !hasPlayers(world, chunk.getX(), chunk.getZ())) {
            return false;
        }
        Map<Long, ChunkData> subMap = map.computeIfAbsent(
                world,
                k -> new LinkedHashMap<>()
        );
        ChunkData data = subMap.computeIfAbsent(
                hashCoordinates(chunk.getX(), chunk.getZ()),
                k -> new ChunkData()
        );
        data.tick();
        return data.refresh(chunk);
    }

    public static boolean cache(Chunk chunk, boolean force) {
        if (isEnabled()) {
            return rawCache(chunk, force);
        } else {
            return false;
        }
    }

    private static boolean isEmptyQueue() {
        if (!map.isEmpty()) {
            for (Map<Long, ChunkData> subMap : map.values()) {
                Iterator<ChunkData> iterator = subMap.values().iterator();

                while (iterator.hasNext()) {
                    ChunkData data = iterator.next();

                    if (data.isQueued()) {
                        return false;
                    } else if (System.currentTimeMillis() - data.lastAccess >= enabledPastTime) {
                        iterator.remove();
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
        loaded.put(hash(chunk.getWorld(), chunk.getX(), chunk.getZ()), -1L);

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
        NPCManager.clear(world);

        if (subMap != null) {
            for (ChunkData data : subMap.values()) {
                loaded.remove(hash(world, data.snapshot.getX(), data.snapshot.getZ()));
            }
        }
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
