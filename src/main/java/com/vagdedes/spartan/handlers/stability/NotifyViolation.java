package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

public class NotifyViolation {

    public static int get(SpartanPlayer staff, SpartanPlayer detectedPlayer,
                          Enums.HackType hackType, int playerCount) {
        if (TestServer.isIdentified()) {
            return 1;
        } else {
            Integer commandDivisor = DetectionNotifications.getDivisor(staff, true);

            if (commandDivisor != null && commandDivisor != 0) {
                return commandDivisor;
            } else {
                double multiplier = Math.max(playerCount / 25.0, 1.0);
                int defaultDivisor = Math.min(
                        AlgebraUtils.integerRound(hackType.getCheck().cancelViolation * multiplier),
                        Check.maximumDefaultCancelViolation
                );

                if (detectedPlayer != null
                        && defaultDivisor < Check.maximumDefaultCancelViolation) {
                    if (staff.uuid.equals(detectedPlayer.uuid)
                            || staff.getWorld().equals(detectedPlayer.getWorld())
                            && AlgebraUtils.getHorizontalDistance(staff.getLocation(), detectedPlayer.getLocation()) <= MoveUtils.chunk) {
                        return 1;
                    } else {
                        return defaultDivisor;
                    }
                } else {
                    return defaultDivisor;
                }
            }
        }
    }

}
