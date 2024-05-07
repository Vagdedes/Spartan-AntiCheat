package com.vagdedes.spartan.abstraction.math;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public interface AbstractMath {

    default double erf(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x)),
                tau = t * Math.exp(-(x * x) - 1.26551223 +
                        t * (1.00002368 + t * (0.37409196 + t * (0.09678418 +
                                t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 +
                                        t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
        return x >= 0 ? 1 - tau : tau - 1;
    }

    default double getCumulativeProbability(double zScore) {
        return 0.5 * (1 + erf(zScore / AlgebraUtils.SQUARE_ROOT_2));
    }

    void clear();

    Number removeLeastSignificant();

    boolean isEmpty();

    int getParts();

    int getTotal();

    int getCount(Number number);

    // Separator

    double getGeneralContribution();

    double getUniqueContribution();

    double getGeneralContribution(Number ignoreNumber);

    double getPersonalContribution(Number number);

    double getProbability(Number number, double defaultValue);

    double getCumulativeProbability(Number number, double defaultValue);

    double getZScore(Number number, double defaultValue);

}

