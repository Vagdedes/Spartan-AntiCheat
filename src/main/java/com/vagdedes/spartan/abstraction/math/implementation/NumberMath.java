package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractMath;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NumberMath implements AbstractMath {

    private final Map<Integer, Short> map;
    private double total;

    public NumberMath() {
        this(2);
    }

    public NumberMath(int capacity) {
        this.map = new HashMap<>(capacity);
        this.total = 0.0f;
    }

    public NumberMath(int capacity, float loadFactor) {
        this.map = new HashMap<>(capacity, loadFactor);
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    public Number removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            short min = Short.MAX_VALUE;
            Integer key = null;

            for (Map.Entry<Integer, Short> entry : this.map.entrySet()) {
                if (entry.getValue() <= min) {
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
        return this.map.getOrDefault(number.hashCode(), (short) 0);
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
                    this.map.getOrDefault(entry.getKey().hashCode(), (short) 0) + entry.getValue().shortValue(),
                    32767
            );
            this.map.put(entry.getKey().hashCode(), newCount);
            this.total += entry.getValue().shortValue();
        }
    }

    // Separator

    public void add(Number number) {
        short current = this.map.getOrDefault(number.hashCode(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.hashCode(),
                    (short) (current + 1)
            );
            this.total++;
        }
    }

    public boolean remove(Number number) {
        Short count = this.map.get(number.hashCode());

        if (count != null) {
            this.total--;

            if (count == 1) {
                this.map.remove(number.hashCode());
                return true;
            } else {
                this.map.put(number.hashCode(), (short) (count - 1));
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
        Short remove = this.map.getOrDefault(ignoreNumber.hashCode(), (short) 0);

        if (this.total > remove) {
            return 1.0 / (this.total - remove);
        } else {
            return this.total > 0
                    ? 1.0 / this.total
                    : 1.0;
        }
    }

    @Override
    public double getPersonalContribution(Number number) {
        Short count = this.map.get(number.hashCode());
        return count != null
                ? 1.0 / count
                : 1.0;
    }

    @Override
    public double getProbability(Number number, double defaultValue) {
        Short count = this.map.get(number.hashCode());
        return count != null ? count / this.total : defaultValue;
    }

    @Override
    public double getCumulativeProbability(Number number, double defaultValue) {
        return StatisticsMath.getCumulativeProbability(this.getZScore(number, defaultValue));
    }

    @Override
    public double getZScore(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.hashCode());

            if (selfCount != null) {
                double mean = this.total / (double) this.map.size(),
                        sumSquaredDifferences = 0.0;

                for (short loopCount : this.map.values()) {
                    double difference = loopCount - mean;
                    sumSquaredDifferences += difference * difference;
                }
                return (selfCount - mean) / Math.sqrt(sumSquaredDifferences / (double) this.map.size());
            }
        }
        return defaultValue;
    }

}

