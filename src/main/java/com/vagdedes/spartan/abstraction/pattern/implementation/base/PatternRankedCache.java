package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternCache;
import com.vagdedes.spartan.utils.math.probability.ProbabilityRank;

public class PatternRankedCache extends PatternCache {

    ProbabilityRank data;

    PatternRankedCache(ProbabilityRank cached) {
        super();
        this.data = cached;
    }

    ProbabilityRank update(ProbabilityRank cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
