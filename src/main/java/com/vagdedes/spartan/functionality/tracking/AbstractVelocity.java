package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class AbstractVelocity {

    static final int blocksOffGround = AlgebraUtils.integerRound(PlayerUtils.height / 4.0);

    public static boolean hasCooldown(SpartanPlayer p) {
        if (p.getTrackers().has(Trackers.TrackerType.ABSTRACT_VELOCITY)) {
            int blockY = p.movement.getLocation().getBlockY();

            if (blockY <= 0 || p.movement.getTicksOnAir() <= 20) {
                return true;
            }
            int offGroundBlocks = p.getBlocksOffGround(blocksOffGround + 1); // Do not use limit because of the 'blockY' comparison
            return offGroundBlocks <= blocksOffGround || offGroundBlocks == blockY;
        }
        return false;
    }
}
