package com.vagdedes.spartan.listeners.bukkit.standalone.chunks;

import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.Map;

public class Event_Chunks_v1_13 {

    public static BlockData getBlockData(World world, int x, int y, int z) {
        Event_Chunks.enable();

        if (y < (Event_Chunks.heightSupport ? world.getMinHeight() : 0)
                || y >= (Event_Chunks.heightSupport ? world.getMaxHeight() : PlayerUtils.height)) {
            return null;
        }
        Map<Long, Event_Chunks.ChunkData> subMap = Event_Chunks.map.get(world);

        if (subMap == null) {
            return null;
        }
        Event_Chunks.ChunkData data = subMap.get(
                Event_Chunks.hashCoordinates(
                        SpartanLocation.getChunkPos(x),
                        SpartanLocation.getChunkPos(z)
                )
        );

        if (data == null) {
            return null;
        }
        data.tick();

        if (data.snapshot == null) {
            return null;
        }
        return data.snapshot.getBlockData(x & 0xF, y, z & 0xF);
    }

}
