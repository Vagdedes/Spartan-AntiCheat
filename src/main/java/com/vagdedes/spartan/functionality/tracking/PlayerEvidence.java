package com.vagdedes.spartan.functionality.tracking;

public class PlayerEvidence {

    public static final boolean POSITIVE = false;
    public static final int factorRequirement = 3;
    public static final double
            dataRatio = 2.0 / 3.0,
            preventionProbability = createProbability(0.1),
            punishmentProbability = createProbability(0.01),
            slightestProbability = createProbability(0.9999),
            emptyProbability = createProbability(1.0);

    public static double createProbability(double probability) {
        return POSITIVE ? 1.0 - probability : probability;
    }

    public static double probabilityToCertainty(double probability) {
        return POSITIVE ? probability : 1.0 - probability;
    }

    public static boolean surpassedProbability(double probability, double threshold) {
        return POSITIVE ? probability >= threshold : probability <= threshold;
    }

}
