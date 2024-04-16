package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.functionality.server.TPS;

public class PatternRepetition {

    private float number;
    private short count;

    PatternRepetition() {
        this.number = Float.MIN_VALUE;
        this.count = 0;
    }

    boolean canUse(float number) {
        if (this.number != number) {
            this.count = 1;
            this.number = number;
            return true;
        } else if (this.count < TPS.maximum) {
            this.count++;
            return true;
        } else {
            return false;
        }
    }
}
