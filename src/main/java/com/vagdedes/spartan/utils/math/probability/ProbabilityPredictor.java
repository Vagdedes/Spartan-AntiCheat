package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.objects.statistics.PatternValue;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.HashHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProbabilityPredictor {

    private final Map<Long, ProbabilityPredictorOutcome> outcomes;

    private static class ProbabilityPredictorOutcome {
        public final ProbabilityPie pie;
        public final double patternSignificance;

        public ProbabilityPredictorOutcome(ProbabilityPie pie,
                                           int patternCount,
                                           int patternSize,
                                           int allPatternsCount) {
            this.pie = pie;
            this.patternSignificance = (patternCount * patternSize) / (double) allPatternsCount;
        }
    }

    public ProbabilityPredictor() {
        this(-1);
    }

    public ProbabilityPredictor(int capacity) {
        this.outcomes = capacity >= 0 ? new LinkedHashMap<>(capacity) : new LinkedHashMap<>();
    }

    public ProbabilityPredictorOutcome calculate(Collection<PatternValue> allPatterns,
                                                 Collection<PatternValue> currentPattern) {
        int allPatternsSize = allPatterns.size();

        if (allPatternsSize > 0) {
            int patternSize = currentPattern.size();
            long patternHash = HashHelper.collection(currentPattern);

            if (patternSize <= allPatternsSize) {
                long hash = (HashHelper.fastCollection(allPatterns) * SpartanBukkit.hashCodeMultiplierLong)
                        + patternHash;
                ProbabilityPredictorOutcome outcome = this.outcomes.get(hash);

                if (outcome != null) {
                    return outcome;
                } else {
                    ProbabilityPie pie = new ProbabilityPie();
                    boolean next = false;
                    int pos = 0, patternCount = 0;

                    for (PatternValue pattern : allPatterns) {
                        if (next) {
                            next = false;
                            patternCount++;
                            pie.add(pattern.number);
                        }
                        if (patternHash == HashHelper.collection(allPatterns, pos - patternSize, pos)) {
                            next = true;
                        }
                        pos++;
                    }
                    outcome = new ProbabilityPredictorOutcome(
                            pie,
                            patternCount,
                            patternSize,
                            allPatternsSize
                    );
                    this.outcomes.put(hash, outcome);
                    return outcome;
                }
            }
        }
        return null;
    }
}
