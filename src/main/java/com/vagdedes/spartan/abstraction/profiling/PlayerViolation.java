package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;

public class PlayerViolation {

    public final Enums.HackType hackType;
    public final long time;
    public final int level, increase;

    public PlayerViolation(long time,
                           Enums.HackType hackType,
                           int level,
                           int increase) {
        this.hackType = hackType;
        this.time = time;
        this.level = level;
        this.increase = increase;
    }

    public int sum() {
        return this.level + this.increase;
    }

}
