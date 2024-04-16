package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractMath;

import java.util.*;

public class NumberMath implements AbstractMath {

    private final Map<Float, Short> map;
    private float total;

    public NumberMath() {
        this(-1);
    }

    public NumberMath(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public NumberMath(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    public Float removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Float key = null;

            for (Map.Entry<Float, Short> entry : this.map.entrySet()) {
                if (entry.getValue() < min) {
                    min = entry.getValue();
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
        return this.map.getOrDefault(number.floatValue(), (short) 0);
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.add(number);
        }
    }

    public void addMultiple(Map<? extends Number, ? extends Number> map) {
        for (Map.Entry<? extends Number, ? extends Number> entry : map.entrySet()) {
            short newCount = (short) Math.min(
                    this.map.getOrDefault(entry.getKey().floatValue(), (short) 0) + entry.getValue().shortValue(),
                    32767
            );
            this.map.put(entry.getKey().floatValue(), newCount);
            this.total += entry.getValue().shortValue();
        }
    }

    // Separator

    public void add(Number number) {
        short current = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.floatValue(),
                    (short) (current + 1)
            );
            this.total++;
        }
    }

    public boolean remove(Number number) {
        Short count = this.map.get(number.floatValue());

        if (count != null) {
            this.total--;

            if (count == 1) {
                this.map.remove(number.floatValue());
                return true;
            } else {
                this.map.put(number.floatValue(), (short) (count - 1));
                return false;
            }
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
        short count = this.map.getOrDefault(ignoreNumber.floatValue(), (short) 0);
        return 1.0 / (this.total - count + 1.0);
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Short count = this.map.get(number.floatValue());
        return count != null ? count / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.floatValue());

            if (selfCount != null) {
                double average = this.total / (double) this.map.size();
                Set<Double> rank = new TreeSet<>();

                for (short count : this.map.values()) {
                    rank.add(Math.abs(count - average));
                }
                average = Math.abs(selfCount - average);
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
            Short selfCount = this.map.get(number.floatValue());

            if (selfCount != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (short count : this.map.values()) {
                    double distance = Math.abs(count - average);
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(Math.abs(selfCount - average)) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getCurveProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.floatValue());

            if (selfCount != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (short count : this.map.values()) {
                    double distance = count - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(selfCount - average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

}

