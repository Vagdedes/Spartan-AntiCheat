package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.objects.statistics.PatternValue;

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

    public void clear() {
        this.numbers.clear();
    }

    public void addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
    }

    public void addMultiplePatterns(Collection<PatternValue> values) {
        for (PatternValue value : values) {
            this.numbers.add(value.number.doubleValue());
        }
        this.sorted = false;
    }

    public void add(Number number) {
        this.numbers.add(number.doubleValue());
        this.sorted = false;
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
}
