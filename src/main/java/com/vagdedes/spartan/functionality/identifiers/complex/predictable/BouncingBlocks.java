package com.vagdedes.spartan.functionality.identifiers.complex.predictable;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;

public class BouncingBlocks {

    private static final int
            slime = 55,
            bed = 25;

    private static final boolean
            v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8),
            v1_12 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12);

    public static void runMove(SpartanPlayer p) {
        if (v1_8) {
            Double vertical = p.movement.getNmsVerticalDistance();

            if (vertical != null && vertical != 0.0 && !p.movement.isJumping(vertical)
                    || p.getBlocksOffGround(3) <= 2) {
                SpartanLocation location = p.movement.getLocation();

                if (BlockUtils.isSlime(p, location, slime)) {
                    add(p, "slime");
                } else if (BlockUtils.isBed(p, location, bed)) {
                    add(p, "bed");
                }
            }
        }
    }

    // Add

    private static void add(SpartanPlayer p, String s) {
        p.getHandlers().add(Handlers.HandlerType.BouncingBlocks, s, 30);
    }

    public static void judge(SpartanPlayer p, SpartanLocation loc) {
        if (BlockUtils.isSlime(p, loc, 3)) {
            add(p, "slime");
        } else if (BlockUtils.isBed(p, loc, 3)) {
            add(p, "bed");
        }
    }

    // Live

    public static boolean isLiveBelow(SpartanPlayer p, SpartanLocation loc) {
        return BlockUtils.isSlime(p, loc, slime) || BlockUtils.isBed(p, loc, bed);
    }

    // Cached

    public static boolean isSlimeBelow(SpartanPlayer p) {
        return v1_8 && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks, "slime");
    }

    public static boolean isBedBelow(SpartanPlayer p) {
        return v1_12 && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks, "bed");
    }

    public static boolean isBelow(SpartanPlayer p) {
        return isSlimeBelow(p) || isBedBelow(p);
    }
}
