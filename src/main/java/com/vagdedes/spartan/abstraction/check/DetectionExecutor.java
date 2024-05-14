package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Timer;
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
        this.executor = this instanceof CheckExecutor ? (CheckExecutor) this : executor;
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

    // Separator

    public final Buffer getBuffer() {
        return player.getBuffer(hackType);
    }

    public final Timer getTimer() {
        return player.getTimer(hackType);
    }

    public Decimals getDecimals() {
        return player.getDecimals(hackType);
    }

    public Cooldowns getCooldowns() {
        return player.getCooldowns(hackType);
    }

    // Separator

    protected final void cancel(String information, double violations, SpartanLocation location,
                                int cancelTicks, boolean groundTeleport, double damage) {
        if (executor.canFunction()) { // Refer to 'canFunctionOrJustImplemented' in CheckExecutor
            new HackPrevention(
                    player, hackType,
                    information, violations,
                    location, cancelTicks,
                    groundTeleport, damage
            );
        }
    }

    protected final void cancel(String information, double violations, SpartanLocation location,
                                int cancelTicks, boolean groundTeleport) {
        cancel(information, violations, location, cancelTicks, groundTeleport, 0.0);
    }

    protected final void cancel(String information, double violations, SpartanLocation location,
                                int cancelTicks) {
        cancel(information, violations, location, cancelTicks, false, 0.0);
    }

    protected final void cancel(String information, double violations, SpartanLocation location) {
        cancel(information, violations, location, 0, false, 0.0);
    }

    protected final void cancel(String information, double violations) {
        cancel(information, violations, null, 0, false, 0.0);
    }

    protected final void cancel(String information) {
        cancel(information, 1.0, null, 0, false, 0.0);
    }

}
