package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.profiling.PlayerViolation;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.probability.ProbabilityRank;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class ViolationStatistics { // todo use pie equation

    private static final Map<Integer, Map<Integer, Integer>> violationProbabilityPie
            = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Map<PlayerProfile, Double>> profileAverageProbability
            = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Map<Double, Integer>> profileProbabilityWave
            = new LinkedHashMap<>();

    // Separator

    public static class TimePeriod {

        private final Set<Integer> players;
        private final Map<Integer, Integer> violations;

        public TimePeriod() {
            this.players = new HashSet<>();
            this.violations = new LinkedHashMap<>();
        }

        public void add(PlayerProfile profile, PlayerViolation playerViolation) {
            players.add(profile.hashCode());
            violations.put(
                    playerViolation.similarityIdentity,
                    violations.getOrDefault(playerViolation.similarityIdentity, 0) + 1
            );
        }

        public Map<Integer, Double> getAverageViolations() {
            int size = this.violations.size();

            if (size > 0) {
                Map<Integer, Double> violations = new LinkedHashMap<>(size);
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
                playerViolation.level,
                playerViolation.similarityIdentity
        );
    }

    // Separator

    public static void clear() {
        synchronized (violationProbabilityPie) {
            violationProbabilityPie.clear();
            profileAverageProbability.clear();
            profileProbabilityWave.clear();
        }
    }

    private static void remove(Collection<PlayerProfile> playerProfiles,
                               Enums.HackType hackType,
                               ResearchEngine.DataType dataType) {
        for (PlayerProfile profile : playerProfiles) {
            if (profile.getDataType() == dataType) {
                profile.getEvidence().remove(hackType, false, true, false);
            }
        }
    }

    // Separator

    public static void calculateData(Collection<PlayerProfile> playerProfiles) {
        clear();

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
                                Collection<PlayerViolation> violations = profile.getViolationHistory(hackType).getCollection();

                                if (!violations.isEmpty()) {
                                    totalViolations += violations.size();

                                    synchronized (profileProbabilityWave) {
                                        for (PlayerViolation violation : violations) {
                                            violationProbabilityPie.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).merge(hash(violation), 1, Integer::sum);
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
                profile.getEvidence().clear(false, true, false);
            }
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
                Collection<PlayerViolation> violations = profile.getViolationHistory(hackType).getCollection();
                double probabilityTotal = 0.0;
                int probabilityCount = 0;

                if (!violations.isEmpty()) {
                    for (PlayerViolation violation : violations) {
                        long time = AlgebraUtils.floorToNearest(violation.time, (int) Check.violationCycleMilliseconds);
                        PlayerViolation highest = highestViolation.get(time);

                        if (highest == null || violation.level > highest.level) {
                            highestViolation.put(time, violation);
                        }
                    }

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
                ProbabilityRank wave = new ProbabilityRank();

                for (Map.Entry<Double, Integer> entry : probabilityData.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        wave.add(entry.getKey());
                    }
                }
                for (PlayerProfile profile : playerProfiles) {
                    if (profile.getDataType() == dataType) {
                        Double probability = probabilityAverage.get(profile);

                        if (probability != null) {
                            probability = wave.getChance(probability);

                            if (probability <= 0.1) {
                                profile.getEvidence().add(
                                        hackType,
                                        "Probability: " + AlgebraUtils.cut(probability * 100.0, 2) + "%",
                                        false,
                                        true,
                                        false
                                );
                            } else {
                                profile.getEvidence().remove(hackType, false, true, false);
                            }
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