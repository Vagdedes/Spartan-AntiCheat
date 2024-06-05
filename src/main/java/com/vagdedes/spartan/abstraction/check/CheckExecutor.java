package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
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

    public final void run(boolean cancelled) {
        if (canFunctionOrJustImplemented() && (!cancelled || hackType.getCheck().handleCancelledEvents)) {
            runInternal(cancelled);
        } else {
            cannotRun(cancelled);
        }
    }

    protected void cannotRun(boolean cancelled) {

    }

    protected void runInternal(boolean cancelled) {

    }

    public final void scheduler() {
        function = !TPS.areLow(player)
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                || player.getGameMode() != GameMode.SPECTATOR)
                && player.getCancellableCompatibility() == null
                && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName(), player)
                && canRun();

        if (canFunctionOrJustImplemented()) {
            schedulerInternal();
        } else {
            cannotSchedule();
        }
    }

    protected void cannotSchedule() {

    }

    protected void schedulerInternal() {

    }

    public final void handle(boolean cancelled, Object object) {
        if (canFunctionOrJustImplemented() && (!cancelled || hackType.getCheck().handleCancelledEvents)) {
            handleInternal(cancelled, object);
        } else {
            cannotHandle(cancelled, object);
        }
    }

    protected void cannotHandle(boolean cancelled, Object object) {

    }

    protected void handleInternal(boolean cancelled, Object object) {

    }

    protected boolean canRun() {
        return true;
    }

    private boolean canFunctionOrJustImplemented() {
        return function || player.ticksPassed() <= TPS.maximum;
    }

    final boolean canFunction() {
        return function;
    }
}
