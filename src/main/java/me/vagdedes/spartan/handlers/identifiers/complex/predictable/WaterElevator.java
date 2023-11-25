package me.vagdedes.spartan.handlers.identifiers.complex.predictable;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
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
                Material type = block.getType();

                if (type == Material.SOUL_SAND) {
                    return true;
                } else if (BlockUtils.isSolid(type) && !block.isWaterLogged()) {
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
