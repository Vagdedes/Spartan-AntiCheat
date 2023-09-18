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

    private static boolean isSoulSand(SpartanLocation location, int blockY, int minY) {
        SpartanLocation locationModified = location.clone();

        for (int i = blockY; i >= minY; i--) {
            locationModified.setY(i);

            for (SpartanLocation loc : locationModified.getSurroundingLocations(BlockUtils.hitbox, 0, BlockUtils.hitbox)) {
                SpartanBlock block = loc.getBlock();
                Material type = block.getType();

                if (type == Material.SOUL_SAND) {
                    return true;
                } else if (BlockUtils.isSolid(type) && !block.isWaterLogged()) {
                    break;
                }
            }
        }
        return false;
    }
}
