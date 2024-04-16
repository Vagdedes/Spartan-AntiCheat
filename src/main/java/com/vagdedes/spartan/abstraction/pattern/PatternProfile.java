package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

public abstract class PatternProfile {

    final int hash;
    private final PatternGeneralization generalization;
    final PatternRepetition repetition;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.hash = Pattern.hash(profile);
        this.generalization = patternGeneralization;
        this.repetition = new PatternRepetition();
    }

    abstract void deleteCustomData();

}
