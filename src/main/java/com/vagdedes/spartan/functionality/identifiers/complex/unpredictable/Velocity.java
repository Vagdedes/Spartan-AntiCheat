package com.vagdedes.spartan.functionality.identifiers.complex.unpredictable;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TestServer;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class Velocity {

    static final int blocksOffGround = AlgebraUtils.integerRound(MoveUtils.height / 4.0);

    public static boolean hasCooldown(SpartanPlayer p) {
        if (p.getHandlers().has(Handlers.HandlerType.Velocity)) {
            int blockY = p.getLocation().getBlockY();

            if (blockY <= 0 || p.getTicksOnAir() <= 20) {
                return true;
            }
            int offGroundBlocks = p.getBlocksOffGround(blocksOffGround + 1, true, true); // Do not use limit because of the 'blockY' comparison
            return offGroundBlocks <= blocksOffGround || offGroundBlocks == blockY;
        }
        return false;
    }

    public static void addCooldown(SpartanPlayer p, int ticks) {
        if (ticks <= 0 || TestServer.isIdentified() || !Compatibility.CompatibilityType.CustomKnockback.isFunctional()) {
            p.getHandlers().add(Handlers.HandlerType.Velocity, Math.abs(ticks));
        }
    }
}
