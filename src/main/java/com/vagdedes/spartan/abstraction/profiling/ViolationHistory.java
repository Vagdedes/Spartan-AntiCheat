package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class ViolationHistory {

    private final Map<Long, PlayerViolation> data;

    ViolationHistory() {
        this.data = new TreeMap<>();
    }

    boolean isEmpty() {
        return this.data.size() <= 1;
    }

    public void store(Enums.HackType hackType, PlayerViolation playerViolation) {
        synchronized (this.data) {
            this.data.put(playerViolation.time, playerViolation);
        }
        ResearchEngine.queueToCache(hackType);
    }

    public Double getTimeDifference(Enums.HackType hackType) {
        if (!this.isEmpty()) {
            Map<Integer, double[]> squareSum = new LinkedHashMap<>();

            synchronized (this.data) {
                Iterator<PlayerViolation> iterator = this.data.values().iterator();
                long previous = iterator.next().time;

                while (iterator.hasNext()) {
                    PlayerViolation violation = iterator.next();
                    double difference = Math.min(
                            violation.time - previous,
                            60_000L
                    );
                    previous = violation.time;
                    difference /= violation.increase * (hackType.violationTimeWorth / difference);
                    double[] data = squareSum.getOrDefault(violation.hash, new double[]{0.0, 0.0});
                    data[0] += difference * difference;
                    data[1] += 1.0;
                    squareSum.put(violation.hash, data);
                }
            }
            double finalSquareSum = 0.0;

            for (Map.Entry<Integer, double[]> entry : squareSum.entrySet()) {
                double value = Math.sqrt(entry.getValue()[0] / entry.getValue()[1]);
                finalSquareSum += value * value;
            }
            return Math.sqrt(finalSquareSum / (this.data.size() - 1.0));
        } else {
            return null;
        }
    }

}
