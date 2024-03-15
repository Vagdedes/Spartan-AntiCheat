package com.vagdedes.spartan.abstraction.pattern;

public abstract class PatternCache {

    private long time;

    protected PatternCache() {
        refresh();
    }

    protected void refresh() {
        this.time = System.currentTimeMillis() + 1_000L;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > this.time;
    }
}
