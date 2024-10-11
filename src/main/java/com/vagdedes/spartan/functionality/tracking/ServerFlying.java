package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;

public class ServerFlying {

    static void run(SpartanPlayer p) {
        if (((IrregularMovements) p.getExecutor(Enums.HackType.IrregularMovements)).limitServerFlying.isEnabled()
                && !Permissions.isBypassing(p, Enums.HackType.IrregularMovements)
                && p.movement.isFlying()
                && !p.movement.isGliding()
                && !p.movement.isSwimming()
                && p.getInstance().getVehicle() == null
                && !p.getInstance().isSleeping()
                && !p.getInstance().isDead()
                && !p.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                && !p.trackers.has(PlayerTrackers.TrackerFamily.MOTION)
                && Attributes.getAmount(p, Attributes.GENERIC_FLYING_SPEED) == 0.0) {
            double limit = (p.getInstance().getFlySpeed() * 10.0) + 1.0;
            Double nmsDistance = p.movement.getEventDistance();

            if (nmsDistance != null && nmsDistance >= limit
                    || p.movement.getLocation().distance(
                    p.movement.getSchedulerFromLocation()
            ) >= limit) {
                p.teleport(p.movement.getDetectionLocation());
            }
        }
    }

}
