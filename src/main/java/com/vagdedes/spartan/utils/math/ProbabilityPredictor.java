package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.math.implementation.NumberMath;

import java.util.Collection;

public class ProbabilityPredictor {

    public final NumberMath beforePie, afterPie;

    public ProbabilityPredictor() {
        this.beforePie = new NumberMath(0);
        this.afterPie = new NumberMath(0);
    }

    public ProbabilityPredictor(Collection<Float> totalPatterns, float currentPattern) {
        this.beforePie = new NumberMath();
        this.afterPie = new NumberMath();

        if (currentPattern != Float.MIN_VALUE
                && totalPatterns.size() > 1) {
            Float found = null;
            float previous = Float.MIN_VALUE;

            for (float pattern : totalPatterns) {
                if (found != null) {
                    this.beforePie.add(found);
                    this.afterPie.add(pattern);

                    if (currentPattern == pattern) {
                        found = previous;
                    } else {
                        found = null;
                    }
                } else if (currentPattern == pattern) {
                    found = previous;
                }
                previous = pattern;
            }
        }
    }

}