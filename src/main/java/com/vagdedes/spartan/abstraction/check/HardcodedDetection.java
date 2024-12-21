package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;

public abstract class HardcodedDetection extends CheckDetection {

    public HardcodedDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
    }

    public final void setHackingRatio(double ratio) {
        SpartanProtocol protocol = this.protocol();

        if (protocol != null && protocol.bukkit.isOnline()) {
            this.setProbability(
                    protocol.spartan.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
        }
    }

    @Override
    public final void clearProbability(Check.DataType dataType) {
    }

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        return this.getProbability(dataType) != PlayerEvidence.emptyProbability;
    }

    @Override
    final double getDataCompletion(Check.DataType dataType) {
        return this.hasSufficientData(dataType) ? 1.0 : 0.0;
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
