package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternStorage;

public abstract class PatternFamily {

    protected final long storeFrequency;
    protected long lastStored;
    protected final PatternStorage[] patterns;

    protected PatternFamily(long storeFrequency, int patterns) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
        this.patterns = new PatternStorage[patterns];
    }

    protected void addPatterns(PatternStorage[] patterns) {
        for (int i = 0; i < patterns.length; i++) {
            this.patterns[i] = patterns[i];
        }
    }

    public void store() {
        if (System.currentTimeMillis() - this.lastStored >= this.storeFrequency) {
            this.lastStored = System.currentTimeMillis();

            for (PatternStorage patternValue : this.patterns) {
                patternValue.store();
            }
        }
    }
}
