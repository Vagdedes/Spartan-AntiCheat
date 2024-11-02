package com.vagdedes.spartan.abstraction.check;

public class PlayerViolation {

    public final long time;
    public final double increase;

    public PlayerViolation(long time, double increase) {
        this.time = time;
        this.increase = increase;
    }

}
