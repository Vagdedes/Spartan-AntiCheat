package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.objects.statistics.PatternValue;

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

    public void clear() {
        this.map.clear();
        this.total = 0;
    }

    // Separator

    public void create(Number number, int amount) {
        Integer count = this.map.get(number);

        if (count != null) {
            this.total -= count;
        }
        this.map.put(number, amount);
        this.total += amount;
    }

    public void add(Number number) {
        this.increase(number, 1);
    }

    public void addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.add(number);
        }
    }

    public void addMultiplePatterns(Collection<PatternValue> values) {
        for (PatternValue value : values) {
            this.add(value.number);
        }
    }

    public void increase(Number number, int amount) {
        this.map.put(number, this.map.getOrDefault(number, 0) + amount);
        this.total += amount;
    }

    // Separator

    public void delete(Number number) {
        Integer count = this.map.remove(number);

        if (count != null) {
            this.total -= count;
        }
    }

    public void remove(Number number) {
        this.decrease(number, 1);
    }

    public void decrease(Number number, int amount) {
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
    }

    // Separator

    public double getChance(Number number) {
        return this.getChance(number, 0.0);
    }

    public double getChance(Number number, double defaultValue) {
        Integer count = this.map.get(number);
        return count != null ? count / this.total : defaultValue;
    }

    public Stream<Map.Entry<Number, Integer>> getChancesRanked() {
        return this.map.entrySet().stream().sorted(
                Map.Entry.comparingByValue(Comparator.reverseOrder()));
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
        return (int) this.map.size();
    }

    public int getTotal() {
        return (int) this.total;
    }
}
