package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.abstraction.math.implementation.NumberRank;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class ViolationAnalysis {

    private static final Map<Integer, Map<Integer, Integer>> violationProbabilityPie
            = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Map<PlayerProfile, Double>> profileAverageProbability
            = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Map<Double, Integer>> profileProbabilityWave
            = new LinkedHashMap<>();

    // Separator

    public static class TimePeriod {

        private final Map<Integer, Integer> violations;
        private final Map<Integer, Set<Integer>> players;

        public TimePeriod() {
            this.violations = new LinkedHashMap<>();
            this.players = new LinkedHashMap<>();
        }

        public void add(PlayerProfile profile, PlayerViolation playerViolation) {
            this.violations.put(
                    playerViolation.identity,
                    this.violations.getOrDefault(playerViolation.identity, 0) + 1
            );
            this.players.computeIfAbsent(
                    playerViolation.identity,
                    k -> new HashSet<>()
            ).add(profile.hashCode());
        }

        public Map<Integer, Double> getAverageViolations() {
            int size = this.violations.size();

            if (size > 0) {
                Map<Integer, Double> violations = new LinkedHashMap<>(size);

                for (Map.Entry<Integer, Integer> entry : this.violations.entrySet()) {
                    violations.put(
                            entry.getKey(),
                            entry.getValue() / ((double) this.players.get(entry.getKey()).size())
                    );
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
                playerViolation.identity
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
                               Enums.DataType dataType) {
        for (PlayerProfile profile : playerProfiles) {
            if (profile.getDataType() == dataType) {
                profile.evidence.remove(hackType, true, true);
            }
        }
    }

    // Separator

    public static void calculateData(Collection<PlayerProfile> playerProfiles) {
        clear();

        if (!playerProfiles.isEmpty() && ResearchEngine.enoughData()) {
            playerProfiles = new ArrayList<>(playerProfiles);
            Enums.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);

            for (Enums.HackType hackType : Enums.HackType.values()) {
                for (Enums.DataType dataType : dataTypes) {
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

                        if (totalViolations >= (ResearchEngine.requiredProfiles * ResearchEngine.requiredProfiles)) {
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
                profile.evidence.clear(true, true);
            }
        }
    }

    private static void calculateProfileProbability(Collection<PlayerProfile> playerProfiles,
                                                    Enums.HackType hackType,
                                                    Enums.DataType dataType,
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
                        long time = AlgebraUtils.floorToNearest(violation.time, 60_000);
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
                                         Enums.DataType dataType,
                                         int majorHash) {
        synchronized (profileProbabilityWave) {
            Map<Double, Integer> probabilityData = profileProbabilityWave.get(majorHash);

            if (probabilityData != null) {
                Map<PlayerProfile, Double> probabilityAverage = profileAverageProbability.get(majorHash);
                NumberRank wave = new NumberRank();
                wave.addMultiple(probabilityData);

                for (PlayerProfile profile : playerProfiles) {
                    if (profile.getDataType() == dataType) {
                        Double probability = probabilityAverage.get(profile);

                        if (probability != null) {
                            probability = wave.getPosition(probability, 1.0);

                            if (probability <= 0.1) {
                                profile.evidence.add(
                                        hackType,
                                        "Probability: " + AlgebraUtils.cut(probability * 100.0, 2) + "%",
                                        true,
                                        false
                                );
                            } else {
                                profile.evidence.remove(hackType, true, false);

                                if (probability < 1.0) {
                                    profile.evidence.add(
                                            hackType,
                                            "Probability: " + AlgebraUtils.cut(probability * 100.0, 2) + "%",
                                            false,
                                            true
                                    );
                                } else {
                                    profile.evidence.add(
                                            hackType,
                                            "Probability: Insufficient",
                                            false,
                                            true
                                    );
                                }
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