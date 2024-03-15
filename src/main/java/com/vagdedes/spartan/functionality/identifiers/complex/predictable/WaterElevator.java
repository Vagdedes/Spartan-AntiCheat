package com.vagdedes.spartan.functionality.identifiers.complex.predictable;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import org.bukkit.Material;

public class WaterElevator {

    static void runMove(SpartanPlayer p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            SpartanLocation location = p.getLocation();
            int blockY = location.getBlockY(), minY = BlockUtils.getMinHeight(p.getWorld());

            if (blockY > minY && isSoulSand(location, blockY, minY)) {
                p.getHandlers().add(Handlers.HandlerType.WaterElevator, 20);
            }
        }
    }

    static void remove(SpartanPlayer p) {
        p.getHandlers().remove(Handlers.HandlerType.WaterElevator);
    }

    private static boolean isSoulSand(SpartanLocation location, int blockY, int minY) {
        SpartanLocation locationModified = location.clone();

        for (int i = 0; i <= (blockY - minY); i++) {
            int nonLiquid = 0;
            SpartanLocation[] locations = locationModified.clone().add(0, -i, 0).getSurroundingLocations(BlockUtils.hitbox, 0, BlockUtils.hitbox);

            for (SpartanLocation loc : locations) {
                SpartanBlock block = loc.getBlock();
                Material type = block.material;

                if (type == Material.SOUL_SAND) {
                    return true;
                } else if (BlockUtils.isSolid(type) && !block.waterLogged) {
                    nonLiquid++;

                    if (nonLiquid == locations.length) {
                        break;
                    }
                }
            }
        }
        return false;
    }
}
