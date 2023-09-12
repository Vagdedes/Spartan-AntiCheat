package me.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class Velocity {

    static final int blocksOffGround = AlgebraUtils.integerRound(MoveUtils.height / 4.0);

    public static void run(SpartanPlayer p) {
        p.getHandlers().add(Handlers.HandlerType.Velocity, 80);
    }

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
