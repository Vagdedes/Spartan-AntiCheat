package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Inflater;

public class WorldListener extends PacketAdapter {

    private static final Map<UUID, Map<Integer, StructureModifier<WrappedLevelChunkData.ChunkData>>>
            chunkContainer = new ConcurrentHashMap<>();
    protected static final Map<UUID, Map<Long, ChunkData>> chunkDataMap = new ConcurrentHashMap<>();

    private static final int SECTION_SIZE = 4096 + 2048 + 2048 + 2048;

    public WorldListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.MAP_CHUNK,
                PacketType.Play.Server.CHUNK_BATCH_START,
                PacketType.Play.Server.UNLOAD_CHUNK
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer container = event.getPacket();

        if (container.getType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            int chunkX = container.getChunkCoordIntPairs().read(0).getChunkX(),
                    chunkZ = container.getChunkCoordIntPairs().read(0).getChunkZ();
            Map<Integer, StructureModifier<WrappedLevelChunkData.ChunkData>> playerChunks =
                    chunkContainer.get(event.getPlayer().getUniqueId());

            if (playerChunks != null) {
                playerChunks.remove(hash(chunkX, chunkZ));
                chunkDataMap.get(event.getPlayer().getUniqueId()).remove(computeChunkKey(chunkX, chunkZ));
            }
        } else {
            int chunkX = container.getIntegers().read(0),
                    chunkZ = container.getIntegers().read(1);
            StructureModifier<WrappedLevelChunkData.ChunkData> chunk = container.getLevelChunkData();
            chunkContainer.computeIfAbsent(
                    event.getPlayer().getUniqueId(),
                    k -> new LinkedHashMap<>()
            ).put(hash(chunkX, chunkZ), chunk);
            handleMapChunkPacket(event, event.getPlayer());
        }
    }

    public static StructureModifier<WrappedLevelChunkData.ChunkData> getChunkStructure(Player player) {
        int x = SpartanLocation.getChunkPos(player.getLocation().getBlockX()),
                z = SpartanLocation.getChunkPos(player.getLocation().getBlockZ());
        Map<Integer, StructureModifier<WrappedLevelChunkData.ChunkData>> playerChunks =
                chunkContainer.get(player.getUniqueId());
        return playerChunks == null
                ? null
                : playerChunks.get(hash(x, z));
    }

    private static int hash(int x, int z) {
        return (x * SpartanBukkit.hashCodeMultiplier) + z;
    }

    public static void handleMapChunkPacket(PacketEvent event, Player player) {
        byte[] compressedData = event.getPacket().getLevelChunkData().read(0).getBuffer();
        int chunkX = event.getPacket().getIntegers().read(0);
        int chunkZ = event.getPacket().getIntegers().read(1);
        int primaryBitMask = event.getPacket().getIntegers().read(1);

        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        byte[] chunkData = new byte[SECTION_SIZE * 16];
        inflater.end();

        ChunkData chunk = parseChunkData(chunkData, primaryBitMask);
        chunkDataMap.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashMap<>()).put(computeChunkKey(chunkX, chunkZ), chunk);
    }

    private static ChunkData parseChunkData(byte[] chunkData, int primaryBitMask) {
        ChunkData chunk = new ChunkData();
        int offset = 0;

        for (int i = 0; i < 16; i++) {
            if ((primaryBitMask & (1 << i)) != 0) {
                byte[] sectionData = new byte[SECTION_SIZE];
                System.arraycopy(chunkData, offset, sectionData, 0, SECTION_SIZE);
                offset += SECTION_SIZE;

                chunk.sections[i] = parseSection(sectionData);
            }
        }

        return chunk;
    }

    private static SectionData parseSection(byte[] sectionData) {
        SectionData section = new SectionData();
        System.arraycopy(sectionData, 0, section.blocks, 0, 4096);
        System.arraycopy(sectionData, 4096, section.metadata, 0, 2048);
        System.arraycopy(sectionData, 6144, section.blockLight, 0, 2048);
        System.arraycopy(sectionData, 8192, section.skyLight, 0, 2048);
        return section;
    }

    private static long computeChunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static BlockData getBlock(int chunkX, int chunkZ, int x, int y, int z, Player player) {
        ChunkData chunk = chunkDataMap.computeIfAbsent(
                        player.getUniqueId(), k -> new LinkedHashMap<>()).get(computeChunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return null;
        }

        int sectionIndex = y / 16;
        int sectionY = y % 16;
        SectionData section = chunk.sections[sectionIndex];
        if (section == null) {
            return null;
        }

        int index = sectionY * 256 + z * 16 + x;
        int blockId = section.blocks[index] & 0xFF;
        int meta = (section.metadata[index / 2] >> ((index % 2) * 4)) & 0xF;

        return new BlockData(blockId, meta);
    }

    public static class ChunkData {
        SectionData[] sections = new SectionData[16];
    }

    public static class SectionData {
        byte[] blocks = new byte[4096];
        byte[] metadata = new byte[2048];
        byte[] blockLight = new byte[2048];
        byte[] skyLight = new byte[2048];
    }

    public static class BlockData {
        int blockId;
        int metadata;

        BlockData(int blockId, int metadata) {
            this.blockId = blockId;
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            return "BlockData{blockId=" + blockId + ", metadata=" + metadata + '}';
        }
    }

}
