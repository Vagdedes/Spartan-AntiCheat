package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {

    public final Number pattern;
    public final long situation;
    final long time;

    PatternValue(long situation, Number number, PatternGeneralization generalization, long time) {
        this.time = time;
        this.pattern = generalization.generalization == 0
                ? number
                : AlgebraUtils.cut(number.doubleValue(), generalization.generalization);
        this.situation = situation;
    }
}
