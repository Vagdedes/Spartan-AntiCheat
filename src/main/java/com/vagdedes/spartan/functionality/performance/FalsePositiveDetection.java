package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.objects.profiling.PlayerViolation;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.Collection;

public class FalsePositiveDetection {

    private static final int
            integerNearest = 5,
            decimalPoint = 2;

    public static boolean canFunction() {
        return Config.settings.getBoolean("Performance.enable_false_positive_detection");
    }

    public static int getSimplifiedNumber(Enums.HackType hackType, String detection, Collection<Number> numbers) {
        int hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();

        if (!numbers.isEmpty()) {
            for (Number number : numbers) {
                if (number instanceof Double) {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier)
                            + Double.hashCode(AlgebraUtils.cut(number.doubleValue(), decimalPoint));
                } else {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier)
                            + AlgebraUtils.roundToNearest(number.intValue(), integerNearest);
                }
            }
        }
        return hash;
    }

    public static boolean canCorrect(PlayerViolation playerViolation, LiveViolation liveViolation) {
        return canFunction()
                && !liveViolation.hasMaxCancelledLevel(playerViolation.getSimilarityIdentity());
    }
}
