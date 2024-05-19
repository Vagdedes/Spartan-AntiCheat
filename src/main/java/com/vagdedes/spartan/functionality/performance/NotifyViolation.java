package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;

public class NotifyViolation {

    public static int get(SpartanPlayer staff,
                          SpartanPlayer detectedPlayer,
                          int def) {
        Integer commandDivisor = DetectionNotifications.getDivisor(staff, true);

        if (commandDivisor != null && commandDivisor != 0) {
            return commandDivisor;
        } else if (staff.uuid.equals(detectedPlayer.uuid)
                || staff.getWorld().equals(detectedPlayer.getWorld())
                && AlgebraUtils.getHorizontalDistance(staff.movement.getLocation(), detectedPlayer.movement.getLocation()) <= PlayerUtils.chunk) {
            return 1;
        } else {
            return def;
        }
    }

}
