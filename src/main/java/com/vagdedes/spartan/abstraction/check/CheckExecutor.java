package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;

public abstract class CheckExecutor extends DetectionExecutor {

    protected final DetectionExecutor[] detections;
    private boolean function;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player, int detections) {
        super(null, hackType, player);
        this.detections = new DetectionExecutor[detections];
        this.function = false;
    }

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player) {
        this(hackType, player, 0);
    }

    protected final void addDetections(DetectionExecutor[] detections) {
        for (int i = 0; i < detections.length; i++) {
            this.detections[i] = detections[i];
        }
    }

    // Run detections when no parameters are needed
    public final void run(boolean cancelled) {
        if (function && (!cancelled || hackType.getCheck().handleCancelledEvents)) {
            runInternal(cancelled);
        }
    }

    protected abstract void runInternal(boolean cancelled);

    public final void scheduler() {
        function = !TPS.areLow(player)
                && MaximumCheckedPlayers.isChecked(player.uuid)
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                || player.getGameMode() != GameMode.SPECTATOR)
                && player.getCancellableCompatibility() == null
                && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName(), player);
        schedulerInternal();
    }

    protected abstract void schedulerInternal();

    // Run handlers or detections when parameters are needed
    public final void handle(boolean cancelled, Object object) {
        if (function && (!cancelled || hackType.getCheck().handleCancelledEvents)) {
            handleInternal(cancelled, object);
        }
    }

    protected abstract void handleInternal(boolean cancelled, Object object);

    abstract protected boolean canDo();

    protected final boolean canFunction() {
        return function;
    }
}
