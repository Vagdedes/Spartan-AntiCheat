package com.vagdedes.spartan.abstraction.math.implementation;


import com.vagdedes.spartan.abstraction.math.AbstractKeyValueMath;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapMath implements AbstractKeyValueMath {

    private final Map<Integer, Map<Integer, Short>> map;
    private double total, specificTotal;

    public MapMath() {
        this(2);
    }

    public MapMath(int capacity) {
        this.map = new HashMap<>(capacity);
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    public MapMath(int capacity, float loadFactor) {
        this.map = new HashMap<>(capacity, loadFactor);
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
    public Number removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Integer key = null;

            for (Map.Entry<Integer, Map<Integer, Short>> entry : this.map.entrySet()) {
                if (entry.getValue().size() < min) {
                    min = entry.getValue().size();
                    key = entry.getKey();

                    if (min == 1) {
                        break;
                    }
                }
            }
            Map<Integer, Short> map = this.map.remove(key);
            int specific = 0;

            for (short count : map.values()) {
                specific += count;
            }
            this.specificTotal -= specific;
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
    public int getSpecificTotal() {
        return (int) this.specificTotal;
    }

    @Override
    public int getCount(Number number) {
        Map<Integer, Short> map = this.map.get(number.hashCode());
        return map == null ? 0 : map.size();
    }

    @Override
    public int getSpecificCount(Number number) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            int total = 0;

            for (short count : map.values()) {
                total += count;
            }
            return total;
        } else {
            return 0;
        }
    }

    @Override
    public int getSpecificCount(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());
        return map == null ? 0 : map.getOrDefault(hash, (short) 0);
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    public void add(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

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
            map = new HashMap<>(2, 1.0f);
            map.put(hash, (short) 1);
            this.map.put(number.hashCode(), map);
            this.total++;
            this.specificTotal++;
        }
    }

    public boolean remove(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            Short value = map.get(hash);

            if (value != null) {
                this.specificTotal--;

                if (value == 1) {
                    map.remove(hash);

                    if (map.isEmpty()) {
                        this.map.remove(number.hashCode());
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
        Map<Integer, Short> map = this.map.get(ignoreNumber.hashCode());
        return map != null && map.size() > this.total
                ? 1.0 / this.total - map.size()
                : 1.0;
    }

    @Override
    public double getGeneralContribution(Number number, int ignoreHash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            int remove = map.containsKey(ignoreHash) ? 1 : 0;
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
        Map<Integer, Short> map = this.map.get(number.hashCode());
        return map != null
                ? 1.0 / map.size()
                : 1.0;
    }

    @Override
    public double getPersonalContribution(Number number, int ignoreHash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            int remove = map.containsKey(ignoreHash) ? 1 : 0;
            return map.size() > remove
                    ? 1.0 / (map.size() - remove)
                    : 1.0;
        } else {
            return 1.0;
        }
    }

    @Override
    public double getProbability(Number number, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            return map.size() / this.total;
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getSpecificProbability(Number number, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            int total = 0;

            for (short count : map.values()) {
                total += count;
            }
            return total / this.specificTotal;
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getPersonalProbability(Number number, int hash, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            Short selfCount = map.get(hash);

            if (selfCount != null) {
                int total = 0;

                for (short count : map.values()) {
                    total += count;
                }
                return total > 0
                        ? selfCount / (double) total
                        : defaultValue;
            } else {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getCumulativeProbability(Number number, double defaultValue) {
        return StatisticsMath.getCumulativeProbability(this.getZScore(number, defaultValue));
    }

    @Override
    public double getZScore(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Map<Integer, Short> selfMap = this.map.get(number.hashCode());

            if (selfMap != null) {
                int selfCount = 0;

                for (short count : selfMap.values()) {
                    selfCount += count;
                }
                double mean = this.specificTotal / (double) this.map.size(),
                        sumSquaredDifferences = 0.0;

                for (Map<Integer, Short> loopMap : this.map.values()) {
                    int loopCount = 0;

                    for (short count : loopMap.values()) {
                        loopCount += count;
                    }
                    double difference = loopCount - mean;
                    sumSquaredDifferences += difference * difference;
                }
                return (selfCount - mean) / Math.sqrt(sumSquaredDifferences / this.map.size());
            }
        }
        return defaultValue;
    }

    @Override
    public double getSpecificGeneralContribution() {
        if (this.specificTotal > 0) {
            return 1.0 / this.specificTotal;
        } else {
            return 1.0;
        }
    }

    @Override
    public double getSpecificGeneralContribution(Number ignoreNumber) {
        Map<Integer, Short> map = this.map.get(ignoreNumber.hashCode());

        if (map != null) {
            int total = 0;

            for (short count : map.values()) {
                total += count;
            }
            return total > this.specificTotal
                    ? 1.0 / (this.specificTotal - total)
                    : 1.0;
        } else {
            return this.specificTotal > 0
                    ? 1.0 / this.specificTotal
                    : 1.0;
        }
    }

    @Override
    public double getSpecificGeneralContribution(Number number, int ignoreHash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            Short remove = map.get(ignoreHash);
            return this.specificTotal > remove
                    ? 1.0 / (this.specificTotal - remove)
                    : 1.0;
        } else {
            return this.specificTotal > 0
                    ? 1.0 / this.specificTotal
                    : 1.0;
        }
    }

    @Override
    public double getSpecificPersonalContribution(Number number) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            int total = 0;

            for (short count : map.values()) {
                total += count;
            }
            return 1.0 / total;
        } else {
            return 1.0;
        }
    }

    @Override
    public double getSpecificPersonalContribution(Number number, int ignoreHash) {
        Map<Integer, Short> map = this.map.get(number.hashCode());

        if (map != null) {
            Short count = map.get(ignoreHash);

            if (count != null) {
                int total = 0;

                for (short loopCount : map.values()) {
                    total += loopCount;
                }
                return total > count
                        ? 1.0 / (total - count)
                        : 1.0;
            } else {
                return 1.0;
            }
        } else {
            return 1.0;
        }
    }

}

