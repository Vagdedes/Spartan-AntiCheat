package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;

public class ServerFlying {

    static void run(SpartanProtocol p) {
        if (((IrregularMovements) p.spartan.getRunner(Enums.HackType.IrregularMovements)).limitServerFlying.isEnabled()
                && !Permissions.isBypassing(p.bukkit, Enums.HackType.IrregularMovements)
                && p.spartan.movement.isFlying()
                && !p.spartan.movement.isGliding()
                && !p.spartan.movement.isSwimming()
                && p.spartan.getVehicle() == null
                && !p.bukkit.isSleeping()
                && !p.bukkit.isDead()
                && !p.spartan.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                && !p.spartan.trackers.has(PlayerTrackers.TrackerFamily.MOTION)
                && Attributes.getAmount(p, Attributes.GENERIC_FLYING_SPEED) == 0.0) {
            double limit = (p.bukkit.getFlySpeed() * 10.0) + 1.0;
            Double nmsDistance = p.spartan.movement.getEventDistance();

            if (nmsDistance != null && nmsDistance >= limit
                    || p.spartan.movement.getLocation().distance(
                    p.spartan.movement.getSchedulerFromLocation()
            ) >= limit) {
                p.spartan.teleport(p.spartan.movement.getDetectionLocation());
            }
        }
    }

}
