package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ProbabilityDetection extends CheckDetection {

    private final long[] firstTime, lastTime;
    private final List<Long>[] data;

    public ProbabilityDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def
    ) {
        super(executor, forcedDataType, detectionType, name, def);
        this.data = new List[Check.DataType.values().length];
        this.firstTime = new long[Check.DataType.values().length];
        this.lastTime = new long[Check.DataType.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            this.data[dataType.ordinal()] = new CopyOnWriteArrayList<>();
            this.firstTime[dataType.ordinal()] = -1L;
            this.lastTime[dataType.ordinal()] = -1L;
        }
    }

    // Probability

    @Override
    public final double getProbability(Check.DataType dataType) {
        return super.getProbability(dataType);
    }

    // Data

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
        int total = playerProfiles.size();

        if (total >= PlayerEvidence.factorRequirement) {
            int pass = 0;

            for (PlayerProfile profile : playerProfiles) {
                CheckDetection detection = profile.getRunner(this.hackType).getDetection(this.name);

                if (detection != null
                        && detection.hasData(profile, dataType)) {
                    pass++;

                    if (pass == PlayerEvidence.factorRequirement) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public final void sort() {
        for (Check.DataType dataType : Check.DataType.values()) {
            List<Long> data = this.data[dataType.ordinal()];

            if (data != null) {
                Collections.sort(data);
            }
        }
    }

    @Override
    public void clearData(Check.DataType dataType) {
        this.data[dataType.ordinal()].clear();
        this.firstTime[dataType.ordinal()] = -1L;
        this.lastTime[dataType.ordinal()] = -1L;
    }

    @Override
    public final void store(Check.DataType dataType, long time) {
        Collection<Long> data = this.data[dataType.ordinal()];
        int size = data.size() - 2_048;

        if (size > 0) {
            Iterator<Long> iterator = data.iterator();

            while (iterator.hasNext() && size > 0) {
                if (data.remove(iterator.next())) {
                    size--;
                }
            }
        }
        data.add(time);

        if (this.firstTime[dataType.ordinal()] == -1L) {
            this.firstTime[dataType.ordinal()] = time;
        } else if (time < this.firstTime[dataType.ordinal()]) {
            this.firstTime[dataType.ordinal()] = time;
        }
        if (this.lastTime[dataType.ordinal()] == -1L) {
            this.lastTime[dataType.ordinal()] = time;
        } else if (time > this.lastTime[dataType.ordinal()]) {
            this.lastTime[dataType.ordinal()] = time;
        }
    }

    @Override
    protected boolean hasData(
            PlayerProfile profile,
            Check.DataType dataType
    ) {
        return !this.data[dataType.ordinal()].isEmpty()
                && profile.getContinuity().hasOnlineTime();
    }

    @Override
    public final double getData(PlayerProfile profile, Check.DataType dataType) {
        Collection<Long> data = this.data[dataType.ordinal()];

        if (data.isEmpty()) {
            return -1.0;
        }
        long onlineTime = profile.getContinuity().getOnlineTime();

        if (onlineTime == 0L) {
            return -1.0;
        }
        double violationCount = data.size(),
                comparisonCount = 0.0;
        long violationVelocitySquareSum = 0;
        Iterator<Long> iterator = data.iterator();

        if (iterator.hasNext()) {
            long previous = iterator.next();

            while (iterator.hasNext()) {
                long current = iterator.next();

                if (profile.getContinuity().wasOnline(current, previous)) {
                    comparisonCount++;
                    long difference = current - previous;
                    violationVelocitySquareSum += difference * difference;
                }
                previous = current;
            }
        }
        if (comparisonCount == 0.0) {
            return -1.0;
        }
        onlineTime /= 1_000L; // Convert to seconds
        violationCount /= (double) onlineTime;

        double averageViolationVelocity = Math.sqrt(violationVelocitySquareSum / comparisonCount),
                result = averageViolationVelocity / violationCount;

        if (Double.isInfinite(result)
                || Double.isNaN(result)) {
            return -1.0;
        }
        return result;
    }

    @Override
    final long getFirstTime(Check.DataType dataType) {
        return this.firstTime[dataType.ordinal()];
    }

    @Override
    final long getLastTime(Check.DataType dataType) {
        return this.lastTime[dataType.ordinal()];
    }

    @Override
    final double getDataCompletion(Check.DataType dataType) {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (!playerProfiles.isEmpty()) {
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                CheckDetection detection = profile.getRunner(this.hackType).getDetection(this.name);

                if (detection != null
                        && detection.hasData(profile, dataType)) {
                    count++;

                    if (count == PlayerEvidence.factorRequirement) {
                        return 1.0;
                    }
                }
            }
            return count / (double) PlayerEvidence.factorRequirement;
        } else {
            return 0.0;
        }
    }

}
