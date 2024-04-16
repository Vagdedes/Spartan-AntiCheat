package com.vagdedes.spartan.abstraction.pattern;

public abstract class PatternFamily {

    private final long storeFrequency;
    private long lastStored;
    private final Pattern[] patterns;

    protected PatternFamily(long storeFrequency, int patterns) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
        this.patterns = new Pattern[patterns];
    }

    protected final void addPatterns(Pattern[] patterns) {
        for (int i = 0; i < patterns.length; i++) {
            this.patterns[i] = patterns[i];
        }
    }

    public final void store() {
        if (System.currentTimeMillis() - this.lastStored >= this.storeFrequency) {
            this.lastStored = System.currentTimeMillis();

            for (Pattern patternValue : this.patterns) {
                patternValue.storeFiles();
            }
        }
    }
}
