package com.vagdedes.spartan.functionality.identifiers.complex.predictable;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;

public class GroundCollision {

    public static boolean run(SpartanPlayer player) {
        if (player.isOnGroundCustom()
                && player.isOnGround()
                && player.getTicksOnAir() == 0
                && player.getTicksOnGround() > 0
                && player.getVehicle() == null) {
            Double nmsVerticalDistance = player.getNmsVerticalDistance();

            if (nmsVerticalDistance != null && nmsVerticalDistance == 0.0) {
                Double nmsHorizontalDistance = player.getNmsHorizontalDistance();
                Handlers handlers = player.getHandlers();

                if (nmsHorizontalDistance != null && nmsHorizontalDistance <= MoveUtils.highPrecision
                        && player.getCustomDistance() <= MoveUtils.lowPrecision) {
                    handlers.removeMany(Handlers.HandlerFamily.Motion);
                    handlers.removeMany(Handlers.HandlerFamily.Velocity);
                } else {
                    handlers.remove(Handlers.HandlerType.BouncingBlocks);
                    handlers.remove(Handlers.HandlerType.WaterElevator);
                }

                if (!Liquid.isLocation(player, player.getLocation())) {
                    player.removeLastLiquidTime();
                }
                return true;
            }
        }
        return false;
    }
}
