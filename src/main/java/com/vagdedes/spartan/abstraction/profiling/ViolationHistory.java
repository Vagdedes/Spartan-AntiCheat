package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.performance.ResearchEngine;

public class ViolationHistory {

    private PlayerViolation previous;
    private int violationIncrease, timeDifference;
    private double count;

    ViolationHistory() {
    }

    public boolean isEmpty() {
        return this.previous == null;
    }

    public void store(PlayerViolation playerViolation) {
        this.count++;

        if (playerViolation.level > this.violationIncrease) {
            this.violationIncrease = playerViolation.level;
        }
        if (this.previous != null) {
            int timeDifference = (int) (playerViolation.time - this.previous.time);
            this.timeDifference += timeDifference * timeDifference;
        }
        this.previous = playerViolation;
        ResearchEngine.queueToCache(playerViolation);
    }

    public double getViolationIncrease() {
        return this.violationIncrease;
    }

    public double getTimeDifference() {
        return this.count > 1
                ? Math.sqrt(this.timeDifference / (this.count - 1.0))
                : 0.0;
    }

}
