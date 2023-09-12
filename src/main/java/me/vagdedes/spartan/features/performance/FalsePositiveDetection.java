package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.*;

public class FalsePositiveDetection {

    public static class TimePeriod {

        private final long ticks;
        private final Set<Integer> players;
        private final Map<Integer, Integer> violations;

        public TimePeriod(double duration) {
            this.ticks = (int) (duration / TPS.tickTimeDecimal);
            this.players = new HashSet<>();
            this.violations = new HashMap<>();
        }

        public void add(PlayerViolation playerViolation) {
            players.add(playerViolation.getPlayerIdentity());
            violations.put(
                    playerViolation.getSimilarityIdentity(),
                    violations.getOrDefault(playerViolation.getSimilarityIdentity(), 0) + 1
            );
        }

        public Map<Integer, Double> getAverageViolations() {
            int size = this.violations.size();

            if (size > 0) {
                Map<Integer, Double> violations = new HashMap<>(size);
                double players = this.players.size();

                for (Map.Entry<Integer, Integer> entry : this.violations.entrySet()) {
                    violations.put(entry.getKey(), entry.getValue() / players / ticks);
                }
                return violations;
            } else {
                return new HashMap<>(0);
            }
        }
    }

    private static final int
            integerNearest = 5,
            decimalPoint = 1;

    public static boolean canFunction() {
        return Settings.getBoolean("Performance.enable_false_positive_detection");
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
