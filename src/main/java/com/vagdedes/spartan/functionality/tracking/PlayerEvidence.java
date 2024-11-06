package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PlayerEvidence {

    public static final boolean POSITIVE = false;
    public static final double
            notificationProbability = createProbability(0.45),
            preventionProbability = createProbability(0.3),
            punishmentProbability = createProbability(0.1),
            emptyProbability = createProbability(1.0);

    private static double createProbability(double probability) {
        return POSITIVE ? 1.0 - probability : probability;
    }

    public static double probabilityToCertainty(double probability) {
        return POSITIVE ? probability : 1.0 - probability;
    }

    public static boolean surpassedProbability(double probability, double threshold) {
        return POSITIVE ? probability >= threshold : probability <= threshold;
    }

    public static double modifyProbability(double probability, double min, double max) {
        return POSITIVE
                ? probability + max
                : probability - min;
    }

    public static int probabilityToFactors(double probability) {
        return AlgebraUtils.integerCeil(1.0 / createProbability(probability));
    }

}
