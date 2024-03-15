package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ProbabilityPie {

    private final Map<Number, Integer> map;
    private double total;

    public ProbabilityPie() {
        this(-1);
    }

    public ProbabilityPie(int capacity) {
        this.map = capacity >= 0 ? new LinkedHashMap<>(capacity) : new LinkedHashMap<>();
        this.total = 0;
    }

    public ProbabilityPie clear() {
        this.map.clear();
        this.total = 0;
        return this;
    }

    // Separator

    public ProbabilityPie addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.add(number);
        }
        return this;
    }

    public ProbabilityPie addMultiplePatterns(Collection<PatternValue> values) {
        for (PatternValue value : values) {
            this.add(value.pattern);
        }
        return this;
    }

    // Separator

    public ProbabilityPie create(Number number, int amount) {
        Integer count = this.map.get(number);

        if (count != null) {
            this.total -= count;
        }
        this.map.put(number, amount);
        this.total += amount;
        return this;
    }

    public ProbabilityPie add(Number number) {
        return this.increase(number, 1);
    }

    public ProbabilityPie increase(Number number, int amount) {
        this.map.put(number, this.map.getOrDefault(number, 0) + amount);
        this.total += amount;
        return this;
    }

    // Separator

    public ProbabilityPie delete(Number number) {
        Integer count = this.map.remove(number);

        if (count != null) {
            this.total -= count;
        }
        return this;
    }

    public ProbabilityPie remove(Number number) {
        return this.decrease(number, 1);
    }

    public ProbabilityPie decrease(Number number, int amount) {
        Integer count = this.map.get(number);

        if (count != null) {
            if (amount >= count) {
                this.map.remove(number);
                this.total -= count;
            } else {
                this.map.put(number, count - amount);
                this.total -= amount;
            }
        }
        return this;
    }

    // Separator

    public double getProbabilityWithCount(int count) {
        return count / this.total;
    }

    public double getProbability(Number number) {
        return this.getProbability(number, 0.0);
    }

    public double getProbability(Number number, double defaultValue) {
        Integer count = this.map.get(number);
        return count != null ? count / this.total : defaultValue;
    }

    public Stream<Map.Entry<Number, Integer>> getChancesRanked() {
        return this.map.entrySet().stream().sorted(
                Map.Entry.comparingByValue(Comparator.reverseOrder())
        );
    }

    // Separator

    public int getCount(Number number) {
        return this.getCount(number, 0);
    }

    public int getCount(Number number, int defaultValue) {
        return this.map.getOrDefault(number, defaultValue);
    }

    // Separator

    public int getSlices() {
        return this.map.size();
    }

    public int getTotal() {
        return (int) this.total;
    }

    public boolean hasData() {
        return this.total > 0;
    }
}
