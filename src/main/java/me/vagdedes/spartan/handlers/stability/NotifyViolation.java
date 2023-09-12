package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.features.moderation.Debug;
import me.vagdedes.spartan.features.moderation.Spectate;
import me.vagdedes.spartan.features.notifications.DetectionNotifications;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class NotifyViolation {

    public static int get(SpartanPlayer staff, SpartanPlayer detectedPlayer,
                          int preferredCancelViolation, int playerCount,
                          boolean testServer) {
        if (TestServer.isIdentified()) {
            return 1;
        }
        Integer commandDivisor = DetectionNotifications.getDivisor(staff, true);

        if (commandDivisor != null && commandDivisor != 0) {
            return commandDivisor;
        }
        double multiplier = Math.max(playerCount / 25.0, 1.0);
        int defaultDivisor = Math.min(AlgebraUtils.integerRound(preferredCancelViolation * multiplier), Check.maximumDefaultCancelViolation);

        if (detectedPlayer != null && defaultDivisor < Check.maximumDefaultCancelViolation) {
            if (staff.getUniqueId().equals(detectedPlayer.getUniqueId())) {
                return testServer || !SpartanBukkit.isProductionServer() ? 1 : defaultDivisor;
            }
            return Spectate.isTarget(staff, detectedPlayer)
                    || Debug.has(staff, detectedPlayer)

                    || staff.getWorld().equals(detectedPlayer.getWorld())
                    && AlgebraUtils.getHorizontalDistance(staff.getLocation(), detectedPlayer.getLocation()) <= MoveUtils.chunk ? 1 : defaultDivisor;
        }
        return defaultDivisor;
    }
}
