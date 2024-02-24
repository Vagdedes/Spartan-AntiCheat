package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.profiling.PlayerViolation;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.Collection;

public class FalsePositiveDetection {

    public static boolean canFunction() {
        return !TestServer.isIdentified()
                && Config.settings.getBoolean("Performance.enable_false_positive_detection");
    }

    public static int getSimplifiedNumber(Enums.HackType hackType, String detection, Collection<Number> numbers) {
        int hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();

        if (!numbers.isEmpty()) {
            for (Number number : numbers) {
                if (number instanceof Double) {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier)
                            + Double.hashCode(AlgebraUtils.cut(number.doubleValue(), GroundUtils.maxHeightLength));
                } else {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier) + number.intValue();
                }
            }
        }
        return hash;
    }

    public static boolean canCorrect(PlayerViolation playerViolation, LiveViolation liveViolation) {
        return canFunction()
                && !liveViolation.hasMaxCancelledLevel(playerViolation.similarityIdentity);
    }
}
