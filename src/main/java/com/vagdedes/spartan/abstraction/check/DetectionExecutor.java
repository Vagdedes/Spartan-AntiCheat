package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

public abstract class DetectionExecutor {

    private static final long
            potentialProcessing = 10L,
            tMinus = TPS.tickTime - potentialProcessing;

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
                || TPS.getMillisecondsPassed(player) <= tMinus)) {
            SpartanBukkit.detectionThread.executeIfFreeElseHere(runnable);
        } else {
            // If there are less than X milliseconds available and the check is not silent,
            // we definitely run the detection on the main thread because we run into the
            // danger of moving the possible prevention in the next tick
            runnable.run();
        }
    }

    protected final void forceAsync(Runnable runnable) {
        SpartanBukkit.detectionThread.execute(runnable);
    }

    // Separator

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage);
    }

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, 0.0);
    }

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, false, 0.0);
    }

    protected final void cancel(String verbose, SpartanLocation location) {
        new HackPrevention(player, hackType, verbose, location, 0, false, 0.0);
    }

    protected final void cancel(String verbose) {
        new HackPrevention(player, hackType, verbose, null, 0, false, 0.0);
    }

    // Separator

    protected final void machineLearningDetection(PatternGeneralization generalization, int situation,
                                                  int repetitions, double min) {
        if (false) {
            PatternGeneralization.Comparison comparison = generalization.comparison(
                    player.getProfile(),
                    situation
            );
            if (repetitions > 1) {
                Decimals decimals = player.getDecimals(hackType);

                if (decimals.getCount(generalization.parent.key) == repetitions) {
                    player.debug(
                            false,
                            true,
                            decimals.get(generalization.parent.key, 1.0, Decimals.CALCULATE_AVERAGE)
                    );
                    decimals.removeOldest(generalization.parent.key);
                }
                decimals.add(generalization.parent.key, 0.0);
            } else {
                player.debug(
                        false,
                        true,
                        comparison.probability,
                        comparison.significance,
                        comparison.contribution,
                        comparison.similarity,
                        comparison.outcome
                );
            }
        }
    }

}
