package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
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
