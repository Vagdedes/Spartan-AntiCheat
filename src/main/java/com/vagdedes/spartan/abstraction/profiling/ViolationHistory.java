package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.performance.ResearchEngine;

public class ViolationHistory {

    private PlayerViolation previous;
    private int increaseSum, timeDifferenceSum;

    ViolationHistory() {
        this.previous = null;
    }

    public boolean isEmpty() {
        return this.previous == null;
    }

    public void store(PlayerViolation playerViolation) {
        this.increaseSum += playerViolation.increase;

        if (this.previous != null) {
            int timeDifference = (int) (playerViolation.time - this.previous.time);
            this.timeDifferenceSum += timeDifference;
        }
        this.previous = playerViolation;
        ResearchEngine.queueToCache(playerViolation);
    }

    public int getIncreaseSum() {
        return this.increaseSum;
    }

    public int getTimeDifferenceSum() {
        return this.timeDifferenceSum;
    }

}
