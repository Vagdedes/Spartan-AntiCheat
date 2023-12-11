package me.vagdedes.spartan.functionality.protections;

import me.vagdedes.spartan.checks.movement.irregularmovements.IrregularMovements;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Velocity;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class ServerFlying {

    public static void run(SpartanPlayer p) {
        if (IrregularMovements.check.getCheck().getBooleanOption("limit_server_flying", false)
                && !Permissions.isBypassing(p, IrregularMovements.check)
                && p.isFlying()
                && p.getVehicle() == null
                && !p.isSleeping()
                && !p.isGliding()
                && !p.isDead()
                && !Velocity.hasCooldown(p)
                && !BouncingBlocks.isBelow(p)
                && !p.isSwimming() && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)
                && !p.getHandlers().has(Handlers.HandlerType.Trident)
                && !p.getHandlers().has(Handlers.HandlerType.ExtremeCollision)
                && !LagLeniencies.hasInconsistencies(p, "tps")
                && !Attributes.has(p, Attributes.GENERIC_FLYING_SPEED)) {
            double limit = (p.getFlySpeed() * 10.0) + 1.0;
            Double nmsDistance = p.getNmsDistance();

            if (nmsDistance != null && nmsDistance >= limit
                    || p.getCustomDistance() >= limit) {
                p.safeTeleport(DetectionLocation.get(p, true));
            }
        }
    }

}
