package me.vagdedes.spartan.objects.data;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.utils.java.MemoryUtils;
import me.vagdedes.spartan.utils.java.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Decimals {

    public static final int CALCULATE_AVERAGE = -1,
            CALCULATE_SUMMARY = -2,
            CALCULATE_MAX = -3,
            CALCULATE_MIN = -4;
    private final Map<String, List<Double>> hm;
    private final boolean async;

    public Decimals(boolean async) {
        this.hm = (async && !MultiVersion.folia ? new ConcurrentHashMap<>() : new LinkedHashMap<>());
        this.async = async;
    }

    public void clear() {
        hm.clear();
    }

    public double get(String name, double def, int newestToOldestIndex) {
        List<Double> list;

        if (async) {
            list = new ArrayList<>(hm.getOrDefault(name, new ArrayList<>()));
        } else {
            list = hm.get(name);

            if (list == null) {
                return def;
            }
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
            default:
                if (newestToOldestIndex >= size) {
                    return def;
                } else {
                    return list.get(MemoryUtils.getNewestToOldestPosition(size, newestToOldestIndex));
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
        return hm.get(name) != null;
    }

    public void set(String name, double value) {
        List<Double> list = hm.get(name);

        if (list != null) {
            list.clear();
            list.add(value);
        } else {
            list = async ? new CopyOnWriteArrayList<>() : new LinkedList<>();
            list.add(value);
            hm.put(name, list);
        }
    }

    public double add(String name, double value, int maxSize) {
        List<Double> list = hm.get(name);

        if (list != null) {
            list.add(value);

            if (maxSize > 0) {
                int size = list.size();

                if (size > maxSize) {
                    size -= maxSize;
                    Iterator<Double> iterator = list.iterator();

                    while (iterator.hasNext() && size > 0) {
                        if (async) {
                            double dbl = iterator.next();
                            list.remove(dbl);
                        } else {
                            iterator.next();
                            iterator.remove();
                        }
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
            list = async ? new CopyOnWriteArrayList<>() : new LinkedList<>();
            list.add(value);
            hm.put(name, list);
            return value;
        }
    }

    public double add(String name, double value) {
        return add(name, value, 0);
    }

    public void remove(String name) {
        hm.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            remove(name);
        }
    }

    public void remove(String name, double value) {
        List<Double> list = hm.get(name);

        if (list != null && list.remove(value) && list.size() == 0) {
            hm.remove(name);
        }
    }

    public int getCount(String name) {
        List<Double> list = hm.get(name);
        return list == null ? 0 : list.size();
    }

    public List<Double> getOldestToNewestList(String name) {
        List<Double> list = hm.get(name);
        return list == null ? new ArrayList<>(0) : list;
    }

    public void clear(String[] ignore) {
        if (hm.size() > 0) {
            List<String> internal = new ArrayList<>();

            for (String name : hm.keySet()) {
                if (!StringUtils.stringContainsPartOfArray(ignore, name)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }

    public void clear(String s) {
        if (hm.size() > 0) {
            List<String> internal = new ArrayList<>();

            for (String name : hm.keySet()) {
                if (name.contains(s)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }
}
