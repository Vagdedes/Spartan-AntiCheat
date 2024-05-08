package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;

public class ServerFlying {

    static void run(SpartanPlayer p) {
        if (Enums.HackType.IrregularMovements.getCheck().getBooleanOption("limit_server_flying", false)
                && !Permissions.isBypassing(p, Enums.HackType.IrregularMovements)
                && p.movement.isFlying()
                && !p.movement.isGliding()
                && !p.movement.isSwimming()
                && p.getVehicle() == null
                && !p.isSleeping()
                && !p.isDead()
                && !p.getTrackers().has(Trackers.TrackerFamily.VELOCITY)
                && !p.getTrackers().has(Trackers.TrackerFamily.MOTION)
                && !Attributes.has(p, Attributes.GENERIC_FLYING_SPEED)) {
            double limit = (p.getFlySpeed() * 10.0) + 1.0;
            Double nmsDistance = p.movement.getNmsDistance();

            if (nmsDistance != null && nmsDistance >= limit
                    || p.movement.getCustomDistance() >= limit) {
                p.safeTeleport(p.movement.getDetectionLocation());
            }
        }
    }

}
