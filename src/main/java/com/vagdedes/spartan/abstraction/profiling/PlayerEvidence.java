package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvidence {

    public static double
            notification = 0.25,
            prevention = 2.0 / 3.0,
            punishment = 0.9;

    private final Map<Enums.HackType, Double> probability; // Live object is used for synchronization

    PlayerEvidence() {
        this.probability = new ConcurrentHashMap<>(2, 1.0f);
    }

    public double get(Enums.HackType hackType) {
        return this.probability.getOrDefault(hackType, 0.0);
    }

    public void clear() {
        this.probability.clear();
    }

    public void remove(Enums.HackType hackType) {
        this.probability.remove(hackType);
    }

    public void add(Enums.HackType hackType, double probability) {
        this.probability.put(hackType, probability);
    }

    // Separator

    public boolean has(double threshold) {
        if (threshold == 0.0) {
            return !this.probability.keySet().isEmpty();
        } else {
            for (Map.Entry<Enums.HackType, Double> entry : this.probability.entrySet()) {
                if (entry.getValue() >= threshold) {
                    return true;
                }
            }
            return false;
        }
    }

    public Collection<Enums.HackType> getKnowledgeList(double threshold) {
        if (threshold == 0.0) {
            return new HashSet<>(this.probability.keySet());
        } else {
            Collection<Enums.HackType> set = new HashSet<>();

            for (Map.Entry<Enums.HackType, Double> entry : this.probability.entrySet()) {
                if (entry.getValue() >= threshold) {
                    set.add(entry.getKey());
                }
            }
            return set;
        }
    }

    public Set<Map.Entry<Enums.HackType, Double>> getKnowledgeEntries(double threshold) {
        if (threshold == 0.0) {
            return new HashSet<>(this.probability.entrySet());
        } else {
            Set<Map.Entry<Enums.HackType, Double>> set = new HashSet<>();

            for (Map.Entry<Enums.HackType, Double> entry : this.probability.entrySet()) {
                if (entry.getValue() >= threshold) {
                    set.add(entry);
                }
            }
            return set;
        }
    }

}
