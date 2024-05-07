package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

public abstract class PatternProfile {

    final int hash;

    PatternProfile(PlayerProfile profile) {
        this.hash = Pattern.hashProfile(profile);
    }

}
