package com.vagdedes.spartan.abstraction.math.implementation;


import com.vagdedes.spartan.abstraction.math.AbstractKeyValueMath;

import java.util.*;

public class MapMath implements AbstractKeyValueMath {

    private final Map<Float, Map<Integer, Short>> map;
    private float total, specificTotal;

    public MapMath() {
        this(-1);
    }

    public MapMath(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    public MapMath(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    @Override
    public Float removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Float key = null;

            for (Map.Entry<Float, Map<Integer, Short>> entry : this.map.entrySet()) {
                if (entry.getValue().size() < min) {
                    min = entry.getValue().size();
                    key = entry.getKey();

                    if (min == 1) {
                        int specific = 0;

                        for (short count : entry.getValue().values()) {
                            specific += count;
                        }
                        this.specificTotal -= specific;
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
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map == null ? 0 : map.size();
    }

    @Override
    public int getSpecificCount(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map == null ? 0 : map.getOrDefault(hash, (short) 0);
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    public void add(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short value = map.get(hash);

            if (value == null) {
                map.put(hash, (short) 1);
                this.total++;
                this.specificTotal++;
            } else if (value < 32767) {
                map.put(hash, (short) (value + 1));
                this.specificTotal++;
            }
        } else {
            map = new HashMap<>();
            map.put(hash, (short) 1);
            this.map.put(number.floatValue(), map);
            this.total++;
            this.specificTotal++;
        }
    }

    public boolean remove(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short value = map.get(hash);

            if (value != null) {
                this.specificTotal--;

                if (value == 1) {
                    map.remove(hash);

                    if (map.isEmpty()) {
                        this.map.remove(number.floatValue());
                    }
                    this.total--;
                } else {
                    map.put(hash, (short) (value - 1));
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Separator

    @Override
    public double getContribution() {
        return 1.0 - (1.0 / (this.total + 1.0));
    }

    @Override
    public double getContribution(Number ignoreNumber) {
        Map<Integer, Short> map = this.map.get(ignoreNumber.floatValue());
        return 1.0 / (this.total - (map != null ? map.size() : 0) + 1.0);
    }

    @Override
    public double getContribution(Number number, int ignoreHash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            return 1.0 / (this.total + (map.containsKey(ignoreHash) ? -1.0 : 0.0) + 1.0);
        } else {
            return 1.0 / (this.total + 1.0);
        }
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map != null ? map.size() / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null) {
                double average = this.total / (double) this.map.size();
                Set<Double> rank = new TreeSet<>();

                for (Map<Integer, Short> map : this.map.values()) {
                    rank.add(Math.abs(map.size() - average));
                }
                average = Math.abs(selfMap.size() - average);
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
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Map<Integer, Short> map : this.map.values()) {
                    double distance = Math.abs(map.size() - average);
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(Math.abs(selfMap.size() - average)) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getCurveProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Map<Integer, Short> map : this.map.values()) {
                    double distance = map.size() - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(selfMap.size() - average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getSpecificContribution() {
        return 1.0 / (this.specificTotal + 1.0);
    }

    @Override
    public double getSpecificContribution(Number ignoreNumber) {
        Map<Integer, Short> map = this.map.get(ignoreNumber.floatValue());

        if (map != null) {
            int total = 0;

            for (short count : map.values()) {
                total += count;
            }
            return 1.0 / (this.specificTotal - total + 1.0);
        } else {
            return 1.0 / (this.specificTotal + 1.0);
        }
    }

    @Override
    public double getSpecificContribution(Number number, int ignoreHash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short count = map.get(ignoreHash);
            return 1.0 / (this.specificTotal + (count != null ? -count : 0.0) + 1.0);
        } else {
            return 1.0 / (this.specificTotal + 1.0);
        }
    }

    @Override
    public double getSpecificRatio(Number number, int hash, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short count = map.get(hash);
            return count != null ? count / this.specificTotal : defaultValue;
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getSpecificDistance(Number number, int hash, double defaultValue) {
        if (!this.map.isEmpty()) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null && selfMap.size() > 1) {
                Short selfCount = selfMap.get(hash);

                if (selfCount != null) {
                    double average = this.specificTotal / (double) selfMap.size();
                    Set<Double> rank = new TreeSet<>();

                    for (short count : selfMap.values()) {
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
        }
        return defaultValue;
    }

    @Override
    public double getSpecificSlopeProbability(Number number, int hash, double defaultValue) {
        if (!this.map.isEmpty()) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null && selfMap.size() > 1) {
                Short selfCount = selfMap.get(hash);

                if (selfCount != null) {
                    double average = this.specificTotal / (double) selfMap.size();
                    Map<Double, Integer> rank = new HashMap<>();

                    for (short count : selfMap.values()) {
                        double distance = Math.abs(count - average);
                        rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                    }
                    return rank.get(Math.abs(selfCount - average)) / (double) rank.size();
                }
            }
        }
        return defaultValue;
    }

    @Override
    public double getSpecificCurveProbability(Number number, int hash, double defaultValue) {
        if (!this.map.isEmpty()) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null && selfMap.size() > 1) {
                Short selfCount = selfMap.get(hash);

                if (selfCount != null) {
                    double average = this.specificTotal / (double) selfMap.size();
                    Map<Double, Integer> rank = new HashMap<>();

                    for (short count : selfMap.values()) {
                        double distance = count - average;
                        rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                    }
                    return rank.get(selfCount - average) / (double) rank.size();
                }
            }
        }
        return defaultValue;
    }

}

