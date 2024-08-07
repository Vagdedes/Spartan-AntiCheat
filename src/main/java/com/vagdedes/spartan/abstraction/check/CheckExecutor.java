package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;

public abstract class CheckExecutor extends DetectionExecutor {

    protected final DetectionExecutor[] detections;
    private boolean function;
    private Object scheduler;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player, int detections, boolean scheduler) {
        super(null, hackType, player);
        this.detections = new DetectionExecutor[detections];
        this.function = false;

        if (scheduler) {
            this.scheduler = SpartanBukkit.runRepeatingTask(player, () -> {
                if (this.scheduler != null && !player.getInstance().isOnline()) {
                    SpartanBukkit.cancelTask(this.scheduler);
                } else {
                    function = (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                            || player.getInstance().getGameMode() != GameMode.SPECTATOR)
                            && !player.protocol.isOnLoadStatus()
                            && player.getCancellableCompatibility() == null
                            && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName(), player)
                            && canRun();

                    if (canFunctionOrJustImplemented()) {
                        scheduler();
                    } else {
                        cannotSchedule();
                    }
                }
            }, 1L, 1L);
        } else {
            this.scheduler = SpartanBukkit.runRepeatingTask(player, () -> {
                if (this.scheduler != null && !player.getInstance().isOnline()) {
                    SpartanBukkit.cancelTask(this.scheduler);
                } else {
                    function = (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                            || player.getInstance().getGameMode() != GameMode.SPECTATOR)
                            && !player.protocol.isOnLoadStatus()
                            && player.getCancellableCompatibility() == null
                            && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName(), player)
                            && canRun();
                }
            }, 1L, 1L);
        }
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

    protected void cannotSchedule() {

    }

    protected void scheduler() {

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
        return function || player.protocol.timePassed() <= TPS.maximum * TPS.tickTime;
    }

    final boolean canFunction() {
        return function;
    }

}
