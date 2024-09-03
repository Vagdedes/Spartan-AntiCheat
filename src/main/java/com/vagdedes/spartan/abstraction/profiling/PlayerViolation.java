package com.vagdedes.spartan.abstraction.profiling;

public class PlayerViolation {

    public final long time;
    public final int level, increase;

    public PlayerViolation(long time,
                           int level,
                           int increase) {
        this.time = time;
        this.level = level;
        this.increase = increase;
    }

    public int sum() {
        return this.level + this.increase;
    }

}
