package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvidence {

    public static final class EvidenceDetails {

        public final double probability;
        private final double mean, self;
        private final boolean positive;

        private EvidenceDetails(double probability, double mean, double self, boolean positive) {
            this.probability = probability;
            this.mean = mean;
            this.self = self;
            this.positive = positive;
        }

        public boolean surpassesMeanByRatio(double required) {
            double ratio = this.self / this.mean;
            return positive
                    ? ratio >= required
                    : ratio <= (1 / required);
        }
    }

    public static double
            notificationProbability = 0.5,
            notificationRatio = 0.9,

    preventionProbability = 0.75,
            preventionRatio = 1.5,

    punishmentProbability = 0.95,
            punishmentRatio = 2.0;

    private final Map<Enums.HackType, EvidenceDetails> probability; // Live object is used for synchronization

    PlayerEvidence() {
        this.probability = new ConcurrentHashMap<>(2, 1.0f);
    }

    public EvidenceDetails get(Enums.HackType hackType) {
        return this.probability.getOrDefault(
                hackType,
                new EvidenceDetails(0.0, 0.0, 0.0, false)
        );
    }

    public void clear() {
        this.probability.clear();
    }

    public void remove(Enums.HackType hackType) {
        this.probability.remove(hackType);
    }

    public void add(Enums.HackType hackType, double probability, double mean, double self, boolean positive) {
        this.probability.put(hackType, new EvidenceDetails(
                probability,
                mean,
                self,
                positive
        ));
    }

    // Separator

    public boolean has(double threshold, double meanRatio) {
        if (threshold == 0.0 && meanRatio == 0.0) {
            return !this.probability.keySet().isEmpty();
        } else {
            for (Map.Entry<Enums.HackType, EvidenceDetails> entry : this.probability.entrySet()) {
                if (entry.getValue().probability >= threshold
                        && entry.getValue().surpassesMeanByRatio(meanRatio)) {
                    return true;
                }
            }
            return false;
        }
    }

    public Collection<Enums.HackType> getKnowledgeList(double threshold, double meanRatio) {
        if (threshold == 0.0 && meanRatio == 0.0) {
            return new HashSet<>(this.probability.keySet());
        } else {
            Collection<Enums.HackType> set = new HashSet<>();

            for (Map.Entry<Enums.HackType, EvidenceDetails> entry : this.probability.entrySet()) {
                if (entry.getValue().probability >= threshold
                        && entry.getValue().surpassesMeanByRatio(meanRatio)) {
                    set.add(entry.getKey());
                }
            }
            return set;
        }
    }

    public Set<Map.Entry<Enums.HackType, EvidenceDetails>> getKnowledgeEntries(double threshold, double meanRatio) {
        if (threshold == 0.0 && meanRatio == 0.0) {
            return new HashSet<>(this.probability.entrySet());
        } else {
            Set<Map.Entry<Enums.HackType, EvidenceDetails>> set = new HashSet<>();

            for (Map.Entry<Enums.HackType, EvidenceDetails> entry : this.probability.entrySet()) {
                if (entry.getValue().probability >= threshold
                        && entry.getValue().surpassesMeanByRatio(meanRatio)) {
                    set.add(entry);
                }
            }
            return set;
        }
    }

}
