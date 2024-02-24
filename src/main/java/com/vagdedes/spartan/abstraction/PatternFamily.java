package com.vagdedes.spartan.abstraction;

import com.vagdedes.spartan.objects.statistics.PatternStorage;

public abstract class PatternFamily {

    protected final long storeFrequency;
    protected long lastStored;
    protected final PatternStorage[] patternValues;

    public PatternFamily(long storeFrequency, PatternStorage[] patternValues) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
        this.patternValues = patternValues;
    }

    public void store() {
        if (System.currentTimeMillis() - this.lastStored >= this.storeFrequency) {
            this.lastStored = System.currentTimeMillis();

            for (PatternStorage patternValue : this.patternValues) {
                patternValue.store();
            }
        }
    }
}
