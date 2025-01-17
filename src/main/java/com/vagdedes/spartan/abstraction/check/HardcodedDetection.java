package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;

public abstract class HardcodedDetection extends CheckDetection {

    private long lastUpdate;

    public HardcodedDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def
    ) {
        super(executor, forcedDataType, detectionType, name, def);
        this.lastUpdate = System.currentTimeMillis();
    }

    public final void setHackingRatio(double ratio) {
        if (this.protocol != null) {
            this.setProbability(
                    this.protocol.spartan.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    @Override
    public final double getProbability(Check.DataType dataType) {
        long max = 60_000;
        double passed = max - (System.currentTimeMillis() - this.lastUpdate) / (double) max;

        if (passed <= 0.0) {
            return PlayerEvidence.emptyProbability;
        } else if (PlayerEvidence.POSITIVE) {
            return passed * super.getProbability(dataType);
        } else {
            return 1.0 - (passed * (1.0 - super.getProbability(dataType)));
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

}
