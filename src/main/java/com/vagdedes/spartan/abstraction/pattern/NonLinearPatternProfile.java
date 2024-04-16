package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NonLinearPatternProfile extends PatternProfile {

    final Map<Integer, Float> correlations;

    NonLinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
        this.correlations = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    void deleteCustomData() {
        synchronized (this.correlations) {
            this.correlations.clear();
        }
    }

}
