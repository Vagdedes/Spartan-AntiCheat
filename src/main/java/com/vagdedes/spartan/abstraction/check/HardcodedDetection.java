package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;

public abstract class HardcodedDetection extends CheckDetection {

    public HardcodedDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
    }

    public final void setHackingRatio(double ratio) {
        if (this.protocol() != null) {
            this.setProbability(
                    this.protocol().spartan.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
        }
    }

    @Override
    public void clearProbability(Check.DataType dataType) {
    }

    @Override
    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time) {
            boolean player = detected instanceof SpartanProtocol;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanProtocol) detected : null
            );

            if (ticks > 0) {
                this.notifications = time + (ticks * TPS.tickTime);
            } else {
                this.notifications = time;
            }
            return true;
        }
        return false;
    }

}
