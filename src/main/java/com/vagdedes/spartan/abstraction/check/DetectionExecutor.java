package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternStorage;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.probability.ProbabilityPredictor;
import com.vagdedes.spartan.utils.math.probability.ProbabilityRank;
import me.vagdedes.spartan.system.Enums;

import java.util.Iterator;
import java.util.Map;

public abstract class DetectionExecutor {

    public final CheckExecutor executor;
    public final Enums.HackType hackType;
    public final SpartanPlayer player;

    public DetectionExecutor(CheckExecutor executor, Enums.HackType hackType, SpartanPlayer player) {
        this.executor = executor;
        this.hackType = hackType;
        this.player = player;
    }

    protected final void runAsync(Runnable runnable) {
        if (!MultiVersion.folia
                && (hackType.getCheck().isSilent(player.getWorld().getName())
                || TPS.getMillisecondsPassed(player) <= 40L)) {
            SpartanBukkit.detectionThread.executeIfFreeElseHere(runnable);
        } else {
            // If there are less than 10 milliseconds available and the check is not silent,
            // we definitely run the detection on the main thread because we run into the
            // danger of moving the possible prevention in the next tick
            runnable.run();
        }
    }

    protected final void forceAsync(Runnable runnable) {
        SpartanBukkit.detectionThread.execute(runnable);
    }

    // Spearator

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage, int violation) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage, violation);
    }

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, 0.0, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int teleportCooldown) {
        new HackPrevention(player, hackType, verbose, location, teleportCooldown, false, 0.0, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location) {
        new HackPrevention(player, hackType, verbose, location, 0, false, 0.0, 1);
    }

    protected final void cancel(String verbose) {
        new HackPrevention(player, hackType, verbose, null, 0, false, 0.0, 1);
    }

    // Separator

    protected final String linearPredictionToDetection(PatternStorage patternStorage,
                                                       int generalization,
                                                       Number client,
                                                       ProbabilityPredictor probabilityPredictor,
                                                       double minProbability,
                                                       double maxDeviation,
                                                       double minSignificance) {
        if (probabilityPredictor.pie.hasData()) {
            int count = 0;
            StringBuilder sb = new StringBuilder()
                    .append("data: ")
                    .append(patternStorage.key)
                    .append(", generalization: ")
                    .append(generalization)
                    .append(", client: ")
                    .append(client)
                    .append(", pie-slices: ")
                    .append(probabilityPredictor.pie.getSlices())
                    .append(", pie-total: ")
                    .append(probabilityPredictor.pie.getTotal())
                    .append(", significance: ")
                    .append(AlgebraUtils.cut(probabilityPredictor.patternSignificance * 100.0, 2))
                    .append(", min-significance: ")
                    .append(minSignificance)
                    .append(", min-probability: ")
                    .append(minProbability)
                    .append(", max-deviation: ")
                    .append(maxDeviation);
            boolean significantPattern = probabilityPredictor.patternSignificance >= minSignificance,
                    highChanceFound = false,
                    validComparisonMade = false;
            Iterator<Map.Entry<Number, Integer>> entries = probabilityPredictor.pie.getChancesRanked().iterator();

            while (entries.hasNext()) {
                Map.Entry<Number, Integer> entry = entries.next();
                double probability = probabilityPredictor.pie.getProbabilityWithCount(entry.getValue());

                if (probability >= minProbability) {
                    highChanceFound = true;

                    if (Math.abs(entry.getKey().doubleValue() - client.doubleValue()) <= maxDeviation) {
                        validComparisonMade = true;
                    }
                }
                count++;

                if (count <= 10) {
                    sb.append(entry.getKey())
                            .append(": ")
                            .append(AlgebraUtils.cut(probability * 100.0, 2))
                            .append(", ");
                }
            }

            if (!significantPattern
                    || !highChanceFound
                    || !validComparisonMade) {
                sb.append("scenario: ")
                        .append(significantPattern ? 1 : 0)
                        .append(highChanceFound ? 1 : 0)
                        .append(validComparisonMade ? 1 : 0);
                return sb.toString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected final String rankedPredictionToDetection(PatternStorage patternStorage,
                                                       int generalization,
                                                       Number client,
                                                       ProbabilityRank probabilityRank,
                                                       double minProbability) {
        if (probabilityRank.hasData()) {
            double probability = probabilityRank.getChance(client);

            if (probability < minProbability) {
                return "data: " + patternStorage.key
                        + ", generalization: " + generalization
                        + ", client: " + client
                        + ", total: " + probabilityRank.getTotal()
                        + ", probability: " + probability
                        + ", min-probability: " + minProbability;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    // Separator

    protected final ProbabilityPredictor buildPredictor(SpartanPlayer player,
                                                        PatternStorage storage,
                                                        long situation,
                                                        int generalization,
                                                        int count) {
        return new ProbabilityPredictor(
                storage.getAll(player, situation, generalization),
                storage.getSpecific(player.getProfile(), situation, generalization, count)
        );
    }
}
