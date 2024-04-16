package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyMath;

import java.util.*;

public class SetMath implements AbstractKeyMath {

    private final Map<Float, Set<Integer>> map;
    private float total;

    public SetMath() {
        this(-1);
    }

    public SetMath(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public SetMath(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    @Override
    public Float removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Float key = null;

            for (Map.Entry<Float, Set<Integer>> entry : this.map.entrySet()) {
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
        Set<Integer> set = this.map.get(number.floatValue());
        return set == null ? 0 : set.size();
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    public void add(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            if (set.add(hash)) {
                this.total++;
            }
        } else {
            set = new HashSet<>();
            set.add(hash);
            this.map.put(number.floatValue(), set);
            this.total++;
        }
    }

    public boolean remove(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null && set.remove(hash)) {
            if (set.isEmpty()) {
                this.map.remove(number.floatValue());
            }
            this.total--;
            return true;
        } else {
            return false;
        }
    }

    // Separator

    @Override
    public double getContribution() {
        return 1.0 / (this.total + 1.0);
    }

    @Override
    public double getContribution(Number ignoreNumber) {
        Set<Integer> set = this.map.get(ignoreNumber.floatValue());
        return 1.0 / (this.total - (set != null ? set.size() : 0) + 1.0);
    }

    @Override
    public double getContribution(Number number, int ignoreHash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            return 1.0 / (this.total + (set.contains(ignoreHash) ? -1.0 : 0.0) + 1.0);
        } else {
            return 1.0 / (this.total + 1.0);
        }
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set != null ? set.size() / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Set<Double> rank = new TreeSet<>();

                for (Set<Integer> set : this.map.values()) {
                    rank.add(Math.abs(set.size() - average));
                }
                average = Math.abs(selfSet.size() - average);
                int position = 0;

                for (double distance : rank) {
                    if (distance == average) {
                        return position / (double) rank.size();
                    } else {
                        position++;
                    }
                }
            }
        }
        return defaultValue;
    }

    @Override
    public double getSlopeProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Set<Integer> set : this.map.values()) {
                    double distance = Math.abs(set.size() - average);
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(Math.abs(selfSet.size() - average)) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getCurveProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Set<Integer> set : this.map.values()) {
                    double distance = set.size() - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(selfSet.size() - average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

}

