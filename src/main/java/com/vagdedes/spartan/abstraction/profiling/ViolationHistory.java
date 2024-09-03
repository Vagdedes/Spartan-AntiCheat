package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ViolationHistory {

    private final Map<Long, PlayerViolation> times;

    ViolationHistory() {
        this.times = new TreeMap<>();
    }

    public boolean isEmpty() {
        return this.times.size() > 1;
    }

    public void store(Enums.HackType hackType, PlayerViolation playerViolation) {
        this.times.put(playerViolation.time, playerViolation);
        ResearchEngine.queueToCache(hackType);
    }

    public Double getTimeDifference(Enums.HackType hackType) {
        int size = this.times.size();

        if (size > 1) {
            Iterator<PlayerViolation> iterator = this.times.values().iterator();
            long previous = iterator.next().time;
            double squareSum = 0L;

            while (iterator.hasNext()) {
                PlayerViolation violation = iterator.next();
                double difference = Math.min(
                        violation.time - previous,
                        60_000L
                );
                previous = violation.time;
                difference /= violation.increase + (hackType.violationTimeWorth / difference);
                squareSum += difference * difference;
            }
            return Math.sqrt(squareSum / (size - 1.0));
        } else {
            return null;
        }
    }

}
