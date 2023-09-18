package me.vagdedes.spartan.abstraction;

import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
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
    abstract public boolean run();

    // Run handlers or detections when parameters are needed
    abstract public boolean handle(Object object);

    abstract protected boolean canDo();
}
