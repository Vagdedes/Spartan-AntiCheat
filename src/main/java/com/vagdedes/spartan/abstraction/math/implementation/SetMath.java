package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyMath;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;

import java.util.*;

public class SetMath implements AbstractKeyMath {

    private final Map<Integer, Set<Integer>> map;
    private double total;

    public SetMath() {
        this(2);
    }

    public SetMath(int capacity) {
        this.map = new HashMap<>(capacity);
        this.total = 0.0f;
    }

    public SetMath(int capacity, float loadFactor) {
        this.map = new HashMap<>(capacity, loadFactor);
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    @Override
    public Number removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Integer key = null;

            for (Map.Entry<Integer, Set<Integer>> entry : this.map.entrySet()) {
                if (entry.getValue().size() < min) {
                    min = entry.getValue().size();
                    key = entry.getKey();

                    if (min == 1) {
                        break;
                    }
                }
            }
            this.map.remove(key);
            this.total -= min;
            return key;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public int getParts() {
        return this.map.size();
    }

    @Override
    public int getTotal() {
        return (int) this.total;
    }

    @Override
    public int getCount(Number number) {
        Set<Integer> set = this.map.get(number.hashCode());
        return set == null ? 0 : set.size();
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    public void add(Number number, int hash) {
        Set<Integer> set = this.map.get(number.hashCode());

        if (set != null) {
            if (set.add(hash)) {
                this.total++;
            }
        } else {
            set = new HashSet<>(2);
            set.add(hash);
            this.map.put(number.hashCode(), set);
            this.total++;
        }
    }

    public boolean remove(Number number, int hash) {
        Set<Integer> set = this.map.get(number.hashCode());

        if (set != null && set.remove(hash)) {
            if (set.isEmpty()) {
                this.map.remove(number.hashCode());
            }
            this.total--;
            return true;
        } else {
            return false;
        }
    }

    // Separator

    @Override
    public double getGeneralContribution() {
        return this.total > 0
                ? 1.0 / this.total
                : 1.0;
    }

    @Override
    public double getUniqueContribution() {
        return this.map.isEmpty()
                ? 1.0
                : 1.0 / this.map.size();
    }

    @Override
    public double getGeneralContribution(Number ignoreNumber) {
        Set<Integer> set = this.map.get(ignoreNumber.hashCode());
        return set != null && set.size() > this.total
                ? 1.0 / (this.total - set.size())
                : 1.0;
    }

    @Override
    public double getGeneralContribution(Number number, int ignoreHash) {
        Set<Integer> set = this.map.get(number.hashCode());

        if (set != null) {
            int remove = set.contains(ignoreHash) ? 1 : 0;
            return this.total > remove
                    ? 1.0 / (this.total - remove)
                    : 1.0;
        } else {
            return this.total > 0
                    ? 1.0 / this.total
                    : 1.0;
        }
    }

    @Override
    public double getPersonalContribution(Number number) {
        Set<Integer> set = this.map.get(number.hashCode());
        return set != null
                ? 1.0 / set.size()
                : 1.0;
    }

    @Override
    public double getPersonalContribution(Number number, int ignoreHash) {
        Set<Integer> set = this.map.get(number.hashCode());

        if (set != null) {
            int remove = set.contains(ignoreHash) ? 1 : 0;
            return set.size() > remove
                    ? 1.0 / (set.size() - remove)
                    : 1.0;
        } else {
            return 1.0;
        }
    }

    @Override
    public double getProbability(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.hashCode());
        return set != null ? set.size() / this.total : defaultValue;
    }

    @Override
    public double getCumulativeProbability(Number number, double defaultValue) {
        return StatisticsMath.getCumulativeProbability(this.getZScore(number, defaultValue));
    }

    @Override
    public double getZScore(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.hashCode());

            if (selfSet != null) {
                double mean = this.total / (double) this.map.size(),
                        sumSquaredDifferences = 0.0;

                for (Set<Integer> loopSet : this.map.values()) {
                    double difference = loopSet.size() - mean;
                    sumSquaredDifferences += difference * difference;
                }
                return (selfSet.size() - mean) / Math.sqrt(sumSquaredDifferences / (double) this.map.size());
            }
        }
        return defaultValue;
    }

}

