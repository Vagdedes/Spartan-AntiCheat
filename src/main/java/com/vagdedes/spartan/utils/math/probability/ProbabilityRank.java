package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;

import java.util.*;

public class ProbabilityRank {

    private final List<Double> numbers;
    private boolean sorted;

    public ProbabilityRank() {
        this(-1);
    }

    public ProbabilityRank(int capacity) {
        this.numbers = capacity >= 0 ? new ArrayList<>(capacity) : new ArrayList<>();
        this.sorted = false;
    }

    public ProbabilityRank clear() {
        this.numbers.clear();
        this.sorted = false;
        return this;
    }

    public ProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    public ProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                this.numbers.add(entry.getKey().doubleValue());
            }
        }
        this.sorted = false;
        return this;
    }

    public ProbabilityRank addMultiplePatterns(Collection<PatternValue> values) {
        for (PatternValue value : values) {
            this.numbers.add(value.pattern.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    public ProbabilityRank add(Number number) {
        this.numbers.add(number.doubleValue());
        this.sorted = false;
        return this;
    }

    // Separator

    public double getChance(Number number) {
        if (!this.sorted) {
            Collections.sort(this.numbers);
            this.sorted = true;
        }
        Iterator<Double> iterator = this.numbers.iterator();
        int start = -1, end = 0, position = 0;
        double dbl = number.doubleValue();

        while (iterator.hasNext()) {
            position++;

            if (iterator.next() == dbl) {
                end = position;

                if (start == -1) {
                    start = position;
                }
            } else if (start != -1) {
                break;
            }
        }
        return ((start + end) / 2.0) / this.numbers.size();
    }

    // Separator

    public boolean hasData() {
        return !this.numbers.isEmpty();
    }

    public int getTotal() {
        return this.numbers.size();
    }
}
