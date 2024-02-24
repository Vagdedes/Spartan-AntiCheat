package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class Clicks {

    // Static

    public static final int pastStoredTicks = 20;
    public static final long pastStoredTime = pastStoredTicks * TPS.tickTime;
    private final Map<Long, SingleClickData> storage;

    // Object
    private long time, update;
    public Clicks() {
        this.time = 0L;
        this.update = 0L;
        this.storage = new LinkedHashMap<>(pastStoredTicks + 1); // Plus one for millisecond inaccuracies
    }

    public void reset() {
        this.time = 0L;
        this.update = 0L;
        this.storage.clear();
    }

    public void calculate() {
        long time = System.currentTimeMillis();

        if (!storage.isEmpty()) {
            Iterator<SingleClickData> iterator = storage.values().iterator();

            while (iterator.hasNext()) {
                SingleClickData clickData = iterator.next();

                if ((time - clickData.time) > pastStoredTime) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }

        // Separator

        long timePassed;
        double pattern;

        if (this.time != 0L) {
            timePassed = time - this.time;

            if (timePassed <= pastStoredTime) {
                pattern = timePassed / TPS.tickTimeDecimal;
            } else {
                timePassed = pastStoredTime;
                pattern = pastStoredTicks * TPS.tickTimeDecimal;
            }
        } else {
            timePassed = pastStoredTime;
            pattern = pastStoredTicks * TPS.tickTimeDecimal;
        }
        this.time = time;

        // Separator

        long tick = AlgebraUtils.integerCeil(time / TPS.tickTimeDecimal);
        SingleClickData clickData = storage.get(tick);

        if (clickData == null) {
            storage.put(tick, new SingleClickData(pattern, time, timePassed));
        } else {
            clickData.timePassed += timePassed;
            clickData.count++;
            clickData.pattern += pattern;
        }
    }

    public long getLastCalculation() {
        return this.time == 0L ? 0L : System.currentTimeMillis() - this.time;
    }

    public int getCount() {
        LinkedList<SingleClickData> data = getRecentData();

        if (!data.isEmpty()) {
            int sum = 0;

            for (SingleClickData clickData : data) {
                sum += clickData.count;
            }
            return sum;
        }
        return 0;
    }

    public LinkedList<Integer> getCounts() {
        LinkedList<SingleClickData> data = getRecentData();

        if (!data.isEmpty()) {
            LinkedList<Integer> list = new LinkedList<>();

            for (SingleClickData clickData : data) {
                list.addFirst((int) clickData.count);
            }
            return list;
        }
        return new LinkedList<>();
    }

    public double getLastPattern() {
        LinkedList<SingleClickData> data = getRecentData();

        if (!data.isEmpty()) {
            SingleClickData clickData = data.getLast();
            return clickData.pattern / clickData.count;
        }
        return 0;
    }

    public double getPatternDifference() {
        LinkedList<SingleClickData> data = getRecentData();
        int size = data.size();

        if (size > 1) {
            SingleClickData last = data.getLast(), beforeLast = data.get(size - 2);
            return (beforeLast.pattern / beforeLast.count) - (last.pattern / last.count);
        }
        return 0.0;
    }

    public double getAveragePattern() {
        LinkedList<SingleClickData> data = getRecentData();
        int size = data.size();

        if (size > 0) {
            double average = 0;

            for (SingleClickData clickData : data) {
                average += (clickData.pattern / clickData.count);
            }
            return average / ((double) size);
        }
        return 0;
    }

    public LinkedList<Double> getPatterns() {
        LinkedList<SingleClickData> data = getRecentData();

        if (!data.isEmpty()) {
            LinkedList<Double> list = new LinkedList<>();

            for (SingleClickData clickData : data) {
                list.addFirst(clickData.pattern / clickData.count);
            }
            return list;
        }
        return new LinkedList<>();
    }

    public long getLastPassedTime() {
        LinkedList<SingleClickData> data = getRecentData();

        if (!data.isEmpty()) {
            SingleClickData clickData = data.getLast();
            return clickData.timePassed / (int) clickData.count;
        }
        return 0L;
    }

    public Long getPassedTimeDifference() {
        LinkedList<SingleClickData> data = getRecentData();
        int size = data.size();

        if (size > 1) {
            SingleClickData last = data.getLast(), beforeLast = data.get(size - 2);
            return Math.abs((beforeLast.timePassed / (int) beforeLast.count) - (last.timePassed / (int) last.count));
        }
        return 0L;
    }

    public long getAveragePassedTime() {
        LinkedList<SingleClickData> data = getRecentData();
        int size = data.size();

        if (size > 0) {
            long average = 0;

            for (SingleClickData clickData : data) {
                average += (clickData.timePassed / (int) clickData.count);
            }
            return average / size;
        }
        return 0;
    }

    public LinkedList<Long> getPassedTimes() {
        LinkedList<SingleClickData> data = getRecentData();

        if (!data.isEmpty()) {
            LinkedList<Long> list = new LinkedList<>();

            for (SingleClickData clickData : data) {
                list.addFirst(clickData.timePassed / (int) clickData.count);
            }
            return list;
        }
        return new LinkedList<>();
    }

    public boolean canDistributeInformation() {
        long time = System.currentTimeMillis();

        if (time - update > 1_000L) {
            this.update = time;
            return true;
        }
        return false;
    }

    private LinkedList<SingleClickData> getRecentData() {
        if (!storage.isEmpty()) {
            LinkedList<SingleClickData> data = new LinkedList<>();
            long time = System.currentTimeMillis();
            boolean add = false;
            Iterator<SingleClickData> iterator = storage.values().iterator();

            while (iterator.hasNext()) {
                SingleClickData clickData = iterator.next();

                if (add) {
                    data.add(clickData);
                } else if ((time - clickData.time) <= pastStoredTime) {
                    add = true;
                    data.add(clickData);
                } else {
                    iterator.remove();
                }
            }
            return data;
        }
        return new LinkedList<>();
    }

    private static class SingleClickData {

        private final long time;
        private double count, pattern;
        private long timePassed;

        private SingleClickData(double pattern, long time, long timePassed) {
            this.time = time;
            this.timePassed = timePassed;
            this.count = 1.0;
            this.pattern = pattern;
        }
    }
}
