package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Event_Chunks implements Listener {

    private static final Map<Long, Boolean> loaded = new ConcurrentHashMap<>();
    private static final Map<World, Map<Long, ChunkData>> map = new ConcurrentHashMap<>();
    private static final boolean heightSupport = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    // Separator

    private static final class ChunkData {

        private long creation;
        private boolean queued;
        private ChunkSnapshot snapshot;

        private ChunkData(ChunkSnapshot snapshot) {
            this.creation = System.currentTimeMillis();
            this.snapshot = snapshot;
        }

        private void refresh(World world) {
            synchronized (this) {
                if (!this.queued
                        && System.currentTimeMillis() - this.creation > 500L) {
                    this.queued = true;

                    SpartanBukkit.chunksThread.execute(() -> {
                        this.creation = System.currentTimeMillis();
                        this.snapshot = world.getChunkAt(this.snapshot.getX(), this.snapshot.getZ()).getChunkSnapshot();
                        this.queued = false;
                    });
                }
            }
        }

    }

    // Separator

    static {
        if (SpartanBukkit.movementPacketsForcedState) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (enabled()) {
                    List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                    if (!players.isEmpty()) {
                        for (SpartanPlayer player : players) {
                            SpartanLocation location = player.movement.getLocation();

                            if (isLoaded(location.world, location.getChunkX(), location.getChunkZ())) {
                                cache(location.getBukkitLocation().getChunk(), true);
                            }
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    // Separator

    public static long hashChunk(int x, int z) {
        return (31L * x) + z;
    }

    private static long totalHash(World world, int x, int z) {
        return (hashChunk(x, z) * 31L) + world.getName().hashCode();
    }

    public static boolean enabled() {
        return SpartanBukkit.packetsEnabled_Movement();
    }

    // Separator

    public static void clear() {
        map.clear();
    }

    public static boolean isLoaded(World world, int x, int z) {
        if (loaded.containsKey(totalHash(world, x, z))) {
            return true;
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                loaded.put(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()), true);
                return true;
            }
        }
        return false;
    }

    public static BlockData get(World world, int x, int y, int z) {
        if (y < (heightSupport ? world.getMinHeight() : 0)
                || y >= (heightSupport ? world.getMaxHeight() : PlayerUtils.height)) {
            return null;
        }
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            return null;
        }
        ChunkData data = subMap.get(hashChunk(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

        if (data == null) {
            return null;
        }
        return data.snapshot.getBlockData(x & 0xF, y, z & 0xF);
    }

    private static boolean hasPlayers(World world, int x, int z) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer player : players) {
                SpartanLocation location = player.movement.getLocation();

                if (location.world.equals(world)
                        && location.getChunkX() == x
                        && location.getChunkZ() == z) {
                    return true;
                }
            }
        }
        return false;
    }

    static void cache(Chunk chunk, boolean force) {
        World world = chunk.getWorld();

        if (!force && !hasPlayers(world, chunk.getX(), chunk.getZ())) {
            return;
        }
        long hash = hashChunk(chunk.getX(), chunk.getZ());
        Map<Long, ChunkData> subMap = map.computeIfAbsent(
                world,
                k -> new ConcurrentHashMap<>()
        );
        ChunkData chunkData = subMap.get(hash);

        if (chunkData == null) {
            subMap.put(hash, new ChunkData(chunk.getChunkSnapshot()));
        } else {
            chunkData.refresh(world);
        }
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.put(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()), true);

        if (enabled()) {
            cache(chunk, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.remove(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()));

        if (enabled()) {
            World world = chunk.getWorld();
            Map<Long, ChunkData> subMap = map.get(world);

            if (subMap != null
                    && subMap.remove(hashChunk(chunk.getX(), chunk.getZ())) != null
                    && subMap.isEmpty()) {
                map.remove(world);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldUnload(WorldUnloadEvent e) {
        World world = e.getWorld();
        Map<Long, ChunkData> subMap = map.remove(world);

        for (ChunkData data : subMap.values()) {
            loaded.remove(totalHash(world, data.snapshot.getX(), data.snapshot.getZ()));
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            loaded.remove(totalHash(world, chunk.getX(), chunk.getZ()));
        }
    }

}
