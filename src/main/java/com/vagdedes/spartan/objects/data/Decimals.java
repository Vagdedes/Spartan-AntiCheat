package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.utils.java.MemoryUtils;

import java.util.*;

public class Decimals {

    public static final int CALCULATE_AVERAGE = -1,
            CALCULATE_SUMMARY = -2,
            CALCULATE_MAX = -3,
            CALCULATE_MIN = -4,
            CALCULATE_REMAINING = -5;
    private final Map<String, List<Double>> storage;

    public Decimals() {
        this.storage = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    public double get(String name, double def, int newestToOldestIndex) {
        synchronized (storage) {
            List<Double> list = storage.get(name);

            if (list == null) {
                return def;
            }
            int size = list.size();

            if (size == 0) {
                return def;
            }
            switch (newestToOldestIndex) {
                case CALCULATE_AVERAGE:
                    double sum = 0.0;

                    for (double dbl : list) {
                        sum += dbl;
                    }
                    return sum / ((double) size);
                case CALCULATE_SUMMARY:
                    sum = 0.0;

                    for (double dbl : list) {
                        sum += dbl;
                    }
                    return sum;
                case CALCULATE_MAX:
                    sum = 0.0;

                    for (double dbl : list) {
                        sum = Math.max(sum, dbl);
                    }
                    return sum;
                case CALCULATE_MIN:
                    sum = Double.MAX_VALUE;

                    for (double dbl : list) {
                        sum = Math.min(sum, dbl);
                    }
                    return sum;
                case CALCULATE_REMAINING:
                    if (size == 1) {
                        return def;
                    }
                    sum = 0.0;
                    double previous = 0.0;
                    Iterator<Double> iterator = list.iterator();

                    if (iterator.hasNext()) {
                        previous = iterator.next();
                    }
                    while (iterator.hasNext()) {
                        double dbl = iterator.next();
                        sum += Math.abs(dbl - previous);
                        previous = dbl;
                    }
                    return sum;
                default:
                    if (newestToOldestIndex >= size) {
                        return def;
                    } else {
                        return list.get(MemoryUtils.getNewestToOldestPosition(size, newestToOldestIndex));
                    }
            }
        }
    }

    public double get(String name, double def) {
        return get(name, def, 0);
    }

    public double get(String name, int newestToOldestIndex) {
        return get(name, Double.MAX_VALUE, newestToOldestIndex);
    }

    public double get(String name) {
        return get(name, Double.MAX_VALUE, 0);
    }

    public boolean has(double d) {
        return d != Double.MAX_VALUE;
    }

    public boolean has(String name) {
        synchronized (storage) {
            return storage.get(name) != null;
        }
    }

    public void set(String name, double value) {
        synchronized (storage) {
            List<Double> list = storage.get(name);

            if (list != null) {
                list.clear();
                list.add(value);
            } else {
                list = new LinkedList<>();
                list.add(value);
                storage.put(name, list);
            }
        }
    }

    public double add(String name, double value, int maxSize) {
        synchronized (storage) {
            List<Double> list = storage.get(name);

            if (list != null) {
                list.add(value);

                if (maxSize > 0) {
                    int size = list.size();

                    if (size > maxSize) {
                        size -= maxSize;
                        Iterator<Double> iterator = list.iterator();

                        while (iterator.hasNext() && size > 0) {
                            iterator.next();
                            iterator.remove();
                            size--;
                        }
                    }
                }
                double sum = 0.0;

                for (double dbl : list) {
                    sum += dbl;
                }
                return sum;
            } else {
                list = new LinkedList<>();
                list.add(value);
                storage.put(name, list);
                return value;
            }
        }
    }

    public double add(String name, double value) {
        return add(name, value, 0);
    }

    public void remove(String name) {
        synchronized (storage) {
            storage.remove(name);
        }
    }

    public void remove(String[] names) {
        synchronized (storage) {
            for (String name : names) {
                storage.remove(name);
            }
        }
    }

    public void remove(String name, double value) {
        synchronized (storage) {
            List<Double> list = storage.get(name);

            if (list != null && list.remove(value) && list.isEmpty()) {
                storage.remove(name);
            }
        }
    }

    public int getCount(String name) {
        List<Double> list;

        synchronized (storage) {
            list = storage.get(name);
        }
        return list == null ? 0 : list.size();
    }

    public List<Double> getOldestToNewestList(String name) {
        List<Double> list;

        synchronized (storage) {
            list = storage.get(name);
        }
        return list == null ? new ArrayList<>(0) : list;
    }
}
