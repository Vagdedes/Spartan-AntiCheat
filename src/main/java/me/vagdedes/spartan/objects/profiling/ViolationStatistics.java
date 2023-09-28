package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class ViolationStatistics {

    // Check -> Profile -> Day -> Set of Data
    private static final Map<Enums.HackType, Map<PlayerProfile, List<Map.Entry<String, Double>>>>
            statistics = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, List<Double>> individualWarmup = new LinkedHashMap<>();
    private static final GlobalWarmup[] globalWarmups = new GlobalWarmup[Enums.HackType.values().length];

    static {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            globalWarmups[hackType.ordinal()] = new GlobalWarmup(false, 0, null);
        }
    }

    public static void clear() {
        synchronized (statistics) {
            statistics.clear();
        }
    }

    static boolean has() {
        return !statistics.isEmpty();
    }

    public static boolean has(Enums.HackType hackType, int profileRequirement) {
        if (globalWarmups[hackType.ordinal()].has) {
            if (profileRequirement == 0) {
                return true;
            }
            Map<PlayerProfile, List<Map.Entry<String, Double>>> map = statistics.get(hackType);
            return map != null && map.size() >= profileRequirement;
        }
        return false;
    }

    public static void remove(PlayerProfile playerProfile) {
        synchronized (statistics) {
            if (statistics.size() > 0) {
                for (Map<PlayerProfile, List<Map.Entry<String, Double>>> map : statistics.values()) {
                    map.remove(playerProfile);
                }
            }
        }
    }

    public static void remove(Enums.HackType hackType) {
        synchronized (statistics) {
            statistics.remove(hackType);
        }
    }

    static IndividualWarmup getComparables(String detection, Collection<ViolationStatistics> data) {
        List<Double> list = individualWarmup.get(detection);

        if (list == null) {
            list = new ArrayList<>();

            for (ViolationStatistics violationStatistics : data) {
                Calculator calculator = violationStatistics.memory.get(detection);

                if (calculator != null) {
                    list.add(violationStatistics.getAverageTime(calculator));
                }
            }
            individualWarmup.put(detection, list);
            return new IndividualWarmup(!list.isEmpty(), false, list);
        }
        return new IndividualWarmup(!list.isEmpty(), true, list);
    }

    static GlobalWarmup get(Enums.HackType hackType) {
        individualWarmup.clear();
        return globalWarmups[hackType.ordinal()];
    }

    public static void calculate(Collection<PlayerProfile> playerProfiles) {
        if (!ResearchEngine.enoughData()) {
            synchronized (statistics) {
                statistics.clear();
            }
        } else {
            synchronized (statistics) {
                boolean hasNoStatistics = statistics.isEmpty();
                Map<Enums.HackType, Map<PlayerProfile, List<Map.Entry<String, Double>>>> newStatistics = new LinkedHashMap<>(statistics);

                // Create Existence

                if (hasNoStatistics) {
                    for (Enums.HackType hackType : Enums.HackType.values()) {
                        newStatistics.computeIfAbsent(hackType, map -> new LinkedHashMap<>());
                    }
                }

                // Create Cache or Maintain Existing

                for (PlayerProfile playerProfile : playerProfiles) {
                    ResearchEngine.DataType dataType = playerProfile.getDataType();

                    for (ViolationHistory violationHistory : playerProfile.getViolationHistory()) {
                        Enums.HackType hackType = violationHistory.getHackType();

                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            if (hasNoStatistics || playerProfile.shouldCalculateEvidence()) {
                                List<PlayerViolation> data = violationHistory.getViolationsList();
                                int size = data.size();

                                if (size > 0) {
                                    Map<String, ViolationStatistics> containedMap = new LinkedHashMap<>(size);

                                    for (PlayerViolation playerViolation : data) {
                                        if (playerViolation.isDetectionEnabled()) {
                                            String date = playerViolation.getDate();
                                            ViolationStatistics statistics = containedMap.get(date);

                                            if (statistics == null) {
                                                statistics = new ViolationStatistics();
                                                containedMap.put(date, statistics);
                                            }
                                            statistics.count(playerViolation);
                                        }
                                    }

                                    // Get Results
                                    List<Map.Entry<String, Double>> list = new ArrayList<>(containedMap.size());

                                    for (ViolationStatistics violationStatistics : containedMap.values()) {
                                        list.addAll(violationStatistics.getAverageTimes());
                                    }
                                    newStatistics.get(hackType).put(playerProfile, list);
                                } else if (!hasNoStatistics) {
                                    newStatistics.get(hackType).remove(playerProfile);
                                }
                            }
                        } else {
                            newStatistics.get(hackType).remove(playerProfile);
                        }
                    }
                }

                // Transfer To Access List

                for (Map.Entry<Enums.HackType, Map<PlayerProfile, List<Map.Entry<String, Double>>>> entry : newStatistics.entrySet()) {
                    GlobalWarmup globalWarmup;
                    Map<PlayerProfile, List<Map.Entry<String, Double>>> map = entry.getValue();
                    int size = map.size();

                    if (size > 0) {
                        Collection<Map.Entry<String, Double>> list = new ArrayList<>(size);

                        for (List<Map.Entry<String, Double>> containedMap : map.values()) {
                            list.addAll(containedMap);
                        }
                        globalWarmup = new GlobalWarmup(true, playerProfiles.size() - size, list);
                    } else {
                        globalWarmup = new GlobalWarmup(false, 0, null);
                    }
                    globalWarmups[entry.getKey().ordinal()] = globalWarmup;
                }
                statistics.putAll(newStatistics);
            }
        }
    }

    // Objects

    private static class Calculator {

        private final List<Long> averageTime;
        private double violations;

        private Calculator() {
            violations = 0.0;
            averageTime = new ArrayList<>();
        }
    }

    static class IndividualWarmup {

        final boolean cached, has;
        final List<Double> loop;

        private IndividualWarmup(boolean has, boolean cached, List<Double> list) {
            this.has = has;
            this.cached = cached;
            this.loop = list;
        }
    }

    static class GlobalWarmup {

        final boolean has;
        final int unrecordedProfiles;
        final Collection<Map.Entry<String, Double>> loop;

        private GlobalWarmup(boolean has, int unrecordedProfiles, Collection<Map.Entry<String, Double>> data) {
            this.has = has;
            this.unrecordedProfiles = unrecordedProfiles;
            this.loop = data;
        }
    }

    // Main Object

    private final Map<String, Calculator> memory;

    ViolationStatistics() {
        memory = new LinkedHashMap<>();
    }

    void count(PlayerViolation playerViolation) {
        String detection = playerViolation.getDetection();
        Calculator subStatistics = memory.get(detection);

        if (subStatistics == null) {
            subStatistics = new Calculator();
            memory.put(detection, subStatistics);
        }
        subStatistics.violations++;
        subStatistics.averageTime.add(playerViolation.getTime());
    }

    Set<Map.Entry<String, Double>> getAverageTimes() {
        int size = memory.size();

        if (size > 0) {
            Map<String, Double> map = new LinkedHashMap<>(size);

            for (Map.Entry<String, Calculator> entry : memory.entrySet()) {
                map.put(entry.getKey(), getAverageTime(entry.getValue()));
            }
            return map.entrySet();
        }
        return new HashSet<>(0);
    }

    private double getAverageTime(Calculator calculator) {
        double counter = calculator.violations - 1.0;

        if (counter > 0.0) {
            long averageTime = 0L, previousTime = 0L;
            Collections.sort(calculator.averageTime); // Set it in order, smallest to biggest
            Iterator<Long> iterator = calculator.averageTime.iterator();

            if (iterator.hasNext()) {
                previousTime = iterator.next();
            }
            while (iterator.hasNext()) {
                long time = iterator.next();
                averageTime += (time - previousTime);
                previousTime = time;
            }
            return averageTime / counter;
        }
        return 0.0;
    }

}
