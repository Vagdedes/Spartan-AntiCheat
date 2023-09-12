package me.vagdedes.spartan.handlers.identifiers.complex.predictable;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;

public class GroundCollision {

    // Liquid is based on the past, so it's not counted here
    // Velocity is far too important to be manipulated
    // Simple ones do not need to be manipulated here

    public static boolean run(SpartanPlayer player) {
        if (player.isOnGroundCustom()
                && player.isOnGround()
                && player.getTicksOnAir() == 0
                && player.getTicksOnGround() > 0
                && player.getNmsVerticalDistance() == 0.0
                && player.getVehicle() == null) {
            Handlers handlers = player.getHandlers();
            handlers.remove(Handlers.HandlerType.BouncingBlocks);
            handlers.remove(Handlers.HandlerType.WaterElevator);

            if (player.getNmsHorizontalDistance() <= MoveUtils.highPrecision
                    && player.getCustomDistance() <= MoveUtils.lowPrecision) {
                handlers.remove(Handlers.HandlerType.ElytraUse);
                handlers.remove(Handlers.HandlerType.Trident);
                handlers.remove(Handlers.HandlerType.Piston);
                handlers.remove(Handlers.HandlerType.Damage);
                handlers.remove(Handlers.HandlerType.FishingHook);
                handlers.remove(Handlers.HandlerType.ExtremeCollision);
                handlers.remove(Handlers.HandlerType.Floor);
            }
            return true;
        }
        return false;
    }
}
