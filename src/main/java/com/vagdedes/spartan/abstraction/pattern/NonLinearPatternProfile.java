package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

public class NonLinearPatternProfile extends PatternProfile {

    float lastPattern;

    NonLinearPatternProfile(PlayerProfile profile) {
        super(profile);
        this.lastPattern = Float.MIN_VALUE;
    }

}
