package com.vagdedes.spartan.functionality.identifiers.complex.predictable;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class GroundCollision {

    public static boolean run(SpartanPlayer player) {
        if (player.isOnGroundCustom()
                && player.isOnGround()
                && player.movement.getTicksOnAir() == 0
                && player.movement.getTicksOnGround() > 0
                && player.getVehicle() == null) {
            Double nmsVerticalDistance = player.movement.getNmsVerticalDistance();

            if (nmsVerticalDistance != null && nmsVerticalDistance == 0.0) {
                Double nmsHorizontalDistance = player.movement.getNmsHorizontalDistance();
                Handlers handlers = player.getHandlers();

                if (nmsHorizontalDistance != null && nmsHorizontalDistance <= PlayerUtils.highPrecision
                        && player.movement.getCustomDistance() <= PlayerUtils.lowPrecision) {
                    handlers.removeMany(Handlers.HandlerFamily.Motion);
                    handlers.removeMany(Handlers.HandlerFamily.Velocity);
                } else {
                    handlers.remove(Handlers.HandlerType.BouncingBlocks);
                    handlers.remove(Handlers.HandlerType.WaterElevator);
                }

                if (!Liquid.isLocation(player, player.movement.getLocation())) {
                    player.movement.removeLastLiquidTime();
                }
                return true;
            }
        }
        return false;
    }
}
