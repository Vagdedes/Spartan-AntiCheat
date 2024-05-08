package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckExecutor extends DetectionExecutor {

    protected final DetectionExecutor[] detections;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player, int detections) {
        super(null, hackType, player);
        this.detections = new DetectionExecutor[detections];
    }

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player) {
        this(hackType, player, 0);
    }

    protected void addDetections(DetectionExecutor[] detection) {
        for (int i = 0; i < detection.length; i++) {
            this.detections[i] = detection[i];
        }
    }

    // Run detections when no parameters are needed
    public void run(boolean cancelled) {
        if (!cancelled || hackType.getCheck().handleCancelledEvents) {
            runInternal(cancelled);
        }
    }

    protected abstract void runInternal(boolean cancelled);

    public abstract void scheduler();

    // Run handlers or detections when parameters are needed
    public void handle(boolean cancelled, Object object) {
        if (!cancelled || hackType.getCheck().handleCancelledEvents) {
            handleInternal(cancelled, object);
        }
    }

    protected abstract void handleInternal(boolean cancelled, Object object);

    abstract protected boolean canDo();
}
