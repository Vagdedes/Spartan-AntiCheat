package com.vagdedes.spartan.utils.minecraft.world;

import org.bukkit.Location;
import org.bukkit.block.Block;

public final class BlockUtil {

    private BlockUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Block getBlockAsync(final Location location) {
        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return location.getWorld().getBlockAt(location);
        } else {
            return null;
        }
    }
}
