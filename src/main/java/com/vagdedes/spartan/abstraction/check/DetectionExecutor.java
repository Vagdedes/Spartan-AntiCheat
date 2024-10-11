package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;

public abstract class DetectionExecutor extends CheckDetection {

    private final String name;
    private final boolean def;

    public DetectionExecutor(CheckExecutor executor, String name, boolean def) {
        super(executor);
        this.name = name;
        this.def = def;
        this.isEnabled();
    }

    public DetectionExecutor(DetectionExecutor executor, String name, boolean def) {
        super(executor.executor);
        this.name = name;
        this.def = def;
        this.isEnabled();
    }

    // Separator

    public boolean isEnabled() {
        return this.name == null
                || this.hackType.getCheck().getBooleanOption(
                "check_" + this.name,
                this.def
        );
    }

    // Separator

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks, boolean groundTeleport, double damage) {
        if (this.isEnabled()) {
            long time = System.currentTimeMillis();

            if (PlayerDetectionSlots.isChecked(this.player)
                    && this.executor.canFunction()
                    && !CloudBase.isInformationCancelled(this.hackType, information)) {
                this.executor.violate(
                        new HackPrevention(
                                location,
                                cancelTicks,
                                groundTeleport,
                                damage
                        ),
                        information,
                        violations,
                        time
                );
            }
        }
    }

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks, boolean groundTeleport) {
        cancel(information, violations, location, cancelTicks, groundTeleport, 0.0);
    }

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks) {
        cancel(information, violations, location, cancelTicks, false, 0.0);
    }

    public final void cancel(String information, double violations, SpartanLocation location) {
        cancel(information, violations, location, 0, false, 0.0);
    }

    public final void cancel(String information, double violations) {
        cancel(information, violations, null, 0, false, 0.0);
    }

    public final void cancel(String information) {
        cancel(information, 1.0, null, 0, false, 0.0);
    }

}
