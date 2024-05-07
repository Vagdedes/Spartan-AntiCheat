package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public class Piston {

    private static final double
            horizontalDistance = 3.0,
            verticalDistance = 2.0;

    public static void run(Block block, List<Block> blocks) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            boolean runBlocks = !blocks.isEmpty();
            World world = block.getWorld();

            for (SpartanPlayer p : players) {
                if (p.getWorld().equals(world)) {
                    SpartanLocation location = p.movement.getLocation();
                    double preX = AlgebraUtils.getSquare(location.getX(), block.getX()),
                            diffY = location.getY() - block.getY(),
                            preZ = AlgebraUtils.getSquare(location.getZ(), block.getZ());

                    if (!run(p, preX, diffY, preZ) // Check if the player is nearby to the piston
                            && runBlocks
                            && Math.sqrt(preX + (diffY * diffY) + preZ) <= PlayerUtils.chunk) { // Check if the player is nearby to the piston affected blocks
                        for (Block affected : blocks) {
                            preX = AlgebraUtils.getSquare(location.getX(), affected.getX());
                            diffY = location.getY() - block.getY();
                            preZ = AlgebraUtils.getSquare(location.getZ(), affected.getZ());

                            if (run(p, preX, diffY, preZ)) { // Check if the player is nearby to the piston affected block
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
            player.getTrackers().add(Trackers.TrackerType.PISTON, (int) TPS.maximum);
            return true;
        }
        return false;
    }
}
