package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvidence {

    public static final boolean POSITIVE = false;
    public static final double
            notificationProbability = createProbability(0.5),
            preventionProbability = createProbability(1.0 / 3.0),
            punishmentProbability = createProbability(0.1);

    private static double createProbability(double probability) {
        return POSITIVE ? 1.0 - probability : probability;
    }

    public static double probabilityToCertainty(double probability) {
        return POSITIVE ? probability : 1.0 - probability;
    }

    public static boolean surpassedProbability(double probability, double threshold) {
        return POSITIVE ? probability >= threshold : probability <= threshold;
    }

    public static double modifyProbability(double probability, double min, double max) {
        return POSITIVE
                ? probability + max
                : probability - min;
    }

    private static int probabilityToPlayers(double probability) {
        return AlgebraUtils.integerCeil(1.0 / createProbability(probability));
    }

    public static int getRequiredPlayers(Enums.HackType hackType, double probability) {
        List<PlayerProfile> profiles = ResearchEngine.getPlayerProfiles();
        int requirement = probabilityToPlayers(probability);

        if (profiles.size() >= requirement) {
            int count = 0;

            for (PlayerProfile profile : profiles) {
                if (profile.hasData(hackType)) {
                    count++;

                    if (count >= requirement) {
                        return 0;
                    }
                }
            }
            return requirement - count;
        } else {
            return requirement - profiles.size();
        }
    }

    // Separator

    private final Map<Enums.HackType, Double> probability; // Live object is used for synchronization

    PlayerEvidence() {
        this.probability = new ConcurrentHashMap<>(2, 1.0f);
    }

    public boolean surpassed(Enums.HackType hackType, double threshold) {
        return surpassedProbability(
                this.getProbability(hackType),
                threshold
        );
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

    public double getProbability(Enums.HackType hackType) {
        return this.probability.getOrDefault(
                hackType,
                POSITIVE ? 0.0 : 1.0
        );
    }

    // Separator

    public boolean has(double threshold) {
        if (threshold == 0.0) {
            return !this.probability.keySet().isEmpty();
        } else {
            for (double probability : this.probability.values()) {
                if (surpassedProbability(
                        probability,
                        threshold
                )) {
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
                if (surpassedProbability(
                        entry.getValue(),
                        threshold
                )) {
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
                if (surpassedProbability(
                        entry.getValue(),
                        threshold
                )) {
                    set.add(entry);
                }
            }
            return set;
        }
    }

}
