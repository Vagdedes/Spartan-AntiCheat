package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;
import com.vagdedes.spartan.utils.java.HashHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProbabilityPredictor {

    public final ProbabilityPie pie;
    public final double patternSignificance;

    public ProbabilityPredictor(Collection<Collection<PatternValue>> totalPatterns,
                                Collection<PatternValue> currentPattern) {
        this.pie = new ProbabilityPie();

        if (!totalPatterns.isEmpty()) {
            long currentPatternHash = HashHelper.collection(currentPattern);
            int currentPatternSize = currentPattern.size(),
                    totalPatternSize = 0;

            for (Collection<PatternValue> allPatterns : totalPatterns) {
                int allPatternsSize = allPatterns.size();

                if (currentPatternSize <= allPatternsSize) {
                    totalPatternSize += allPatternsSize;
                    boolean next = false;
                    Iterator<PatternValue> iterator = allPatterns.iterator();
                    LinkedList<PatternValue> list = new LinkedList<>();

                    for (int i = 0; i < currentPatternSize; i++) {
                        list.add(iterator.next());
                    }

                    while (iterator.hasNext()) {
                        PatternValue pattern = iterator.next();
                        list.add(pattern);
                        list.removeFirst();

                        if (next) {
                            next = false;
                            this.pie.add(pattern.pattern);
                        }
                        if (currentPatternHash == HashHelper.collection(list)) {
                            next = true;
                        }
                    }
                }
            }
            if (totalPatternSize > 0) {
                this.patternSignificance = (this.pie.getTotal() * currentPatternSize) / (double) totalPatternSize;
            } else {
                this.patternSignificance = 0.0;
            }
        } else {
            this.patternSignificance = 0.0;
        }
    }
}