package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public class Piston {

    private static final double
            horizontalDistance = 3.0,
            verticalDistance = 2.0;

    public static void run(Block block, List<Block> blocks) {
        List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

        if (!protocols.isEmpty()) {
            boolean runBlocks = !blocks.isEmpty();
            World world = block.getWorld();

            for (SpartanProtocol protocol : protocols) {
                if (protocol.spartanPlayer.getWorld().equals(world)) {
                    SpartanLocation location = protocol.spartanPlayer.movement.getLocation();
                    double preX = AlgebraUtils.getSquare(location.getX(), block.getX()),
                            diffY = location.getY() - block.getY(),
                            preZ = AlgebraUtils.getSquare(location.getZ(), block.getZ());

                    if (!run(protocol.spartanPlayer, preX, diffY, preZ) // Check if the player is nearby to the piston
                            && runBlocks
                            && Math.sqrt(preX + (diffY * diffY) + preZ) <= PlayerUtils.chunk) { // Check if the player is nearby to the piston affected blocks
                        for (Block affected : blocks) {
                            preX = AlgebraUtils.getSquare(location.getX(), affected.getX());
                            diffY = location.getY() - block.getY();
                            preZ = AlgebraUtils.getSquare(location.getZ(), affected.getZ());

                            if (run(protocol.spartanPlayer, preX, diffY, preZ)) { // Check if the player is nearby to the piston affected block
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean run(SpartanPlayer player, double preX, double diffY, double preZ) {
        if (Math.sqrt(preX + preZ) <= horizontalDistance
                && Math.abs(diffY) <= verticalDistance) {
            player.trackers.add(Trackers.TrackerType.PISTON, (int) TPS.maximum);
            return true;
        }
        return false;
    }
}
