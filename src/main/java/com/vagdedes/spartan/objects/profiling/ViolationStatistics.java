package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import com.vagdedes.spartan.utils.java.math.WaveProbability;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class ViolationStatistics {

    private static final Map<Integer, Map<Integer, Integer>> violationProbabilityPie = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Map<PlayerProfile, Double>> profileAverageProbability = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Map<Double, Integer>> profileProbabilityWave = new LinkedHashMap<>();
    private static boolean running = false;

    // Separator

    public static class TimePeriod {

        private final Set<Integer> players;
        private final Map<Integer, Integer> violations;

        public TimePeriod() {
            this.players = new HashSet<>();
            this.violations = new HashMap<>();
        }

        public void add(PlayerViolation playerViolation) {
            players.add(playerViolation.getPlayerIdentity());
            violations.put(
                    playerViolation.getSimilarityIdentity(),
                    violations.getOrDefault(playerViolation.getSimilarityIdentity(), 0) + 1
            );
        }

        public Map<Integer, Double> getAverageViolations() {
            int size = this.violations.size();

            if (size > 0) {
                Map<Integer, Double> violations = new HashMap<>(size);
                double players = this.players.size();

                for (Map.Entry<Integer, Integer> entry : this.violations.entrySet()) {
                    violations.put(entry.getKey(), entry.getValue() / players);
                }
                return violations;
            } else {
                return new HashMap<>(0);
            }
        }
    }

    // Separator

    private static int hash(PlayerViolation playerViolation) {
        return Objects.hash(
                playerViolation.getLevel(),
                playerViolation.getSimilarityIdentity()
        );
    }

    // Separator

    private static void rawClear() {
        violationProbabilityPie.clear();
        profileAverageProbability.clear();
        profileProbabilityWave.clear();
    }

    public static void clear() {
        synchronized (violationProbabilityPie) {
            rawClear();
        }
    }

    private static void remove(Collection<PlayerProfile> playerProfiles,
                               Enums.HackType hackType,
                               ResearchEngine.DataType dataType) {
        for (PlayerProfile profile : playerProfiles) {
            if (profile.getDataType() == dataType) {
                synchronized (profile.getEvidence().live) {
                    profile.getEvidence().historical.remove(hackType);
                }
            }
        }
    }

    // Separator

    public static void calculateData(Collection<PlayerProfile> playerProfiles) {
        if (running) {
            return;
        }
        running = true;

        synchronized (violationProbabilityPie) {
            rawClear();

            if (!playerProfiles.isEmpty() && ResearchEngine.enoughData()) {
                playerProfiles = new ArrayList<>(playerProfiles);
                ResearchEngine.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);

                for (Enums.HackType hackType : Enums.HackType.values()) {
                    for (ResearchEngine.DataType dataType : dataTypes) {
                        int majorHash = Objects.hash(hackType, dataType);

                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            int totalViolations = 0;

                            for (PlayerProfile profile : playerProfiles) {
                                if (profile.getDataType() == dataType) {
                                    Collection<PlayerViolation> violations = profile.getViolationHistory(hackType).getViolationsList();

                                    if (!violations.isEmpty()) {
                                        for (PlayerViolation violation : violations) {
                                            if (violation.isDetectionEnabled()) {
                                                synchronized (profileProbabilityWave) {
                                                    violationProbabilityPie.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).merge(hash(violation), 1, Integer::sum);
                                                }
                                                totalViolations++;
                                            } else {
                                                synchronized (profileProbabilityWave) {
                                                    Map<Integer, Integer> map = violationProbabilityPie.get(majorHash);

                                                    if (map != null) {
                                                        map.remove(hash(violation));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (totalViolations >= ResearchEngine.logRequirement) {
                                calculateProfileProbability(
                                        playerProfiles,
                                        hackType,
                                        dataType,
                                        majorHash,
                                        totalViolations
                                );
                            } else {
                                synchronized (profileProbabilityWave) {
                                    violationProbabilityPie.remove(majorHash);
                                    profileAverageProbability.remove(majorHash);
                                    profileProbabilityWave.remove(majorHash);
                                }
                                remove(playerProfiles, hackType, dataType);
                            }
                        } else {
                            synchronized (profileProbabilityWave) {
                                violationProbabilityPie.remove(majorHash);
                                profileAverageProbability.remove(majorHash);
                                profileProbabilityWave.remove(majorHash);
                            }
                            remove(playerProfiles, hackType, dataType);
                        }
                    }
                }
            } else if (!playerProfiles.isEmpty()) {
                for (PlayerProfile profile : playerProfiles) {
                    profile.getEvidence().clear();
                }
            }
            running = false;
        }
    }

    private static void calculateProfileProbability(Collection<PlayerProfile> playerProfiles,
                                                    Enums.HackType hackType,
                                                    ResearchEngine.DataType dataType,
                                                    int majorHash,
                                                    int totalViolations) {
        Map<Integer, Integer> violationProbability = violationProbabilityPie.get(majorHash);
        Map<Long, PlayerViolation> highestViolation = new LinkedHashMap<>();

        for (PlayerProfile profile : playerProfiles) {
            if (profile.getDataType() == dataType) {
                Collection<PlayerViolation> violations = profile.getViolationHistory(hackType).getViolationsList();
                double probabilityTotal = 0.0;
                int probabilityCount = 0;

                if (!violations.isEmpty()) {
                    for (PlayerViolation violation : violations) {
                        if (violation.isDetectionEnabled()) {
                            long time = AlgebraUtils.floorToNearest(violation.getTime(), (int) Check.violationCycleSeconds);
                            PlayerViolation highest = highestViolation.get(time);

                            if (highest == null || violation.getLevel() > highest.getLevel()) {
                                highestViolation.put(time, violation);
                            }
                        }
                    }

                    if (!highestViolation.isEmpty()) {
                        for (PlayerViolation violation : highestViolation.values()) {
                            Integer count = violationProbability.get(hash(violation));

                            if (count != null) {
                                probabilityTotal += count / (double) totalViolations;
                                probabilityCount++;
                            }
                        }

                        if (probabilityCount > 0) {
                            double averageProbability = probabilityTotal / (double) probabilityCount;

                            synchronized (profileProbabilityWave) {
                                profileAverageProbability.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).put(profile, averageProbability);
                                profileProbabilityWave.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).merge(averageProbability, 1, Integer::sum);
                            }
                        } else {
                            Map<PlayerProfile, Double> map = profileAverageProbability.get(majorHash);

                            if (map != null) {
                                map.remove(profile);
                            }
                        }
                    }
                }
            }
        }
        judgeProbability(
                playerProfiles,
                hackType,
                dataType,
                majorHash
        );
    }

    private static void judgeProbability(Collection<PlayerProfile> playerProfiles,
                                         Enums.HackType hackType,
                                         ResearchEngine.DataType dataType,
                                         int majorHash) {
        synchronized (profileProbabilityWave) {
            Map<Double, Integer> probabilityData = profileProbabilityWave.get(majorHash);

            if (probabilityData != null) {
                Map<PlayerProfile, Double> probabilityAverage = profileAverageProbability.get(majorHash);
                WaveProbability wave = new WaveProbability();

                for (Map.Entry<Double, Integer> entry : probabilityData.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        wave.addNumber(entry.getKey());
                    }
                }
                for (PlayerProfile profile : playerProfiles) {
                    if (profile.getDataType() == dataType) {
                        Double probability = probabilityAverage.get(profile);

                        if (probability != null) {
                            probability = wave.getPosition(probability);

                            if (probability <= 0.1) {
                                profile.getEvidence().historical.put(hackType, "Probability: " + AlgebraUtils.cut(probability * 100.0, 2) + "%");
                            } else {
                                profile.getEvidence().historical.remove(hackType);
                            }
                        } else {
                            profile.getEvidence().historical.remove(hackType);
                        }
                    }
                }
            } else {
                profileAverageProbability.remove(majorHash);
                remove(playerProfiles, hackType, dataType);
            }
        }
    }
}