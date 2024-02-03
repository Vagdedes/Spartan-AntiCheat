package com.vagdedes.spartan.utils.java.math;

import java.util.*;

public class WaveProbability {

    private final List<Double> numbers;
    private boolean sorted;

    public WaveProbability() {
        this.numbers = new ArrayList<>();
        this.sorted = false;
    }

    public void clear() {
        this.numbers.clear();
    }

    public void addNumbers(Collection<Number> numbers) {
        for (Number number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
    }

    public void addNumber(Number number) {
        this.numbers.add(number.doubleValue());
        this.sorted = false;
    }

    public void addDoubles(Collection<Double> numbers) {
        this.numbers.addAll(numbers);
        this.sorted = false;
    }

    public void addFloats(Collection<Float> numbers) {
        for (Float number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
    }

    public void addIntegers(Collection<Integer> numbers) {
        for (Integer number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
    }

    public void addLongs(Collection<Long> numbers) {
        for (Long number : numbers) {
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
    }

    // Separator

    public double getPosition(Number number) {
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
