package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;

import java.util.List;

public abstract class HardcodedDetection extends CheckDetection {

    private static final long decay = 60_000;

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
                    this.protocol.bukkitExtra.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
            this.lastUpdate = System.currentTimeMillis() + decay;
        }
    }

    // Separator

    @Override
    public final double getProbability(Check.DataType dataType) {
        if (this.lastUpdate > System.currentTimeMillis()) {
            double passed = decay - (this.lastUpdate - System.currentTimeMillis()) / (double) decay;

            if (PlayerEvidence.POSITIVE) {
                return passed * super.getProbability(dataType);
            } else {
                return 1.0 - (passed * (1.0 - super.getProbability(dataType)));
            }
        } else {
            return PlayerEvidence.emptyProbability;
        }
    }

    @Override
    public final void clearProbability(Check.DataType dataType) {
    }

    @Override
    public final void setProbability(Check.DataType dataType, double probability) {
        if (probability != PlayerEvidence.nullProbability) {
            super.setProbability(dataType, probability);
        }
    }

    // Separator

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        return this.getProbability(dataType) != PlayerEvidence.emptyProbability;
    }

    @Override
    public void clearData(Check.DataType dataType) {
    }

    @Override
    public final void storeData(Check.DataType dataType, long time) {
    }

    @Override
    public final void sortData() {
    }

    @Override
    public final double getAllData(PlayerProfile profile, Check.DataType dataType) {
        return super.getAllData(profile, dataType);
    }

    @Override
    public final List<Double> getDataSamples(PlayerProfile profile, Check.DataType dataType) {
        return super.getDataSamples(profile, dataType);
    }

}
