package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ProbabilityDetection extends CheckDetection {

    private final int[] dataCount;
    private final long[] firstTime, lastTime;
    private final List<Long>[] data;

    public ProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
        this.data = new List[Check.DataType.values().length];
        this.dataCount = new int[Check.DataType.values().length];
        this.firstTime = new long[Check.DataType.values().length];
        this.lastTime = new long[Check.DataType.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            this.data[dataType.ordinal()] = new CopyOnWriteArrayList<>();
            this.dataCount[dataType.ordinal()] = 0;
            this.firstTime[dataType.ordinal()] = -1L;
            this.lastTime[dataType.ordinal()] = -1L;
        }
    }

    // Data

    @Override
    protected final boolean hasData(PlayerProfile profile, Check.DataType dataType) {
        return getFirstTime(dataType) != -1L;
    }

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
        int total = playerProfiles.size();

        if (total >= PlayerEvidence.factorRequirement) {
            int pass = 0,
                    count = 0;
            int halfRequirement = AlgebraUtils.integerCeil(PlayerEvidence.factorRequirement / 2.0);

            for (PlayerProfile profile : playerProfiles) {
                int individualCount = profile.getRunner(this.hackType).getDetection(this.name).getDataCount(dataType);

                if (individualCount > 0) {
                    count += individualCount;
                    pass++;

                    if (pass == PlayerEvidence.factorRequirement) {
                        return true;
                    } else if (pass >= halfRequirement
                            && (count / (double) total) > PlayerEvidence.factorRequirement) {
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
        this.dataCount[dataType.ordinal()] = 0;
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
        this.dataCount[dataType.ordinal()]++;
    }

    @Override
    public final double getData(PlayerProfile profile, Check.DataType dataType) {
        if (getFirstTime(dataType) == -1L) {
            return CheckDetection.defaultData();
        } else {
            Collection<Long> data = this.data[dataType.ordinal()];

            if (data == null) {
                return CheckDetection.defaultData();
            } else {
                double violationCount = data.size(),
                        averageSimilarity,
                        averageViolationVelocity,
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
                            previous = current;
                            violationVelocitySquareSum += difference * difference;
                        }
                    }
                }
                long onlineTime = profile.getContinuity().getOnlineTime();

                if (onlineTime > 0L) {
                    onlineTime /= 1_000L; // Convert to seconds
                    violationCount /= (double) onlineTime;
                }
                if (comparisonCount == 0.0) {
                    averageViolationVelocity = Long.MAX_VALUE;
                    averageSimilarity = 1.0;
                } else {
                    averageViolationVelocity = Math.sqrt(violationVelocitySquareSum / comparisonCount);
                    Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

                    if (!playerProfiles.isEmpty()) {
                        long current,
                                previous,
                                difference;
                        double sum = 0.0;
                        int total = 0;

                        for (PlayerProfile loopProfile : playerProfiles) {
                            CheckDetection detection = loopProfile.getRunner(this.hackType).getDetection(this.name);

                            if (detection.hasData(loopProfile, dataType)) {
                                ProbabilityDetection probabilityDetection = (ProbabilityDetection) detection;
                                data = probabilityDetection.data[dataType.ordinal()];

                                if (data != null) {
                                    iterator = data.iterator();

                                    if (iterator.hasNext()) {
                                        previous = iterator.next();

                                        while (iterator.hasNext()) {
                                            current = iterator.next();

                                            if (loopProfile.getContinuity().wasOnline(current, previous)) {
                                                difference = current - previous;
                                                sum += 1.0 - Math.abs(difference - averageViolationVelocity) / (averageViolationVelocity + difference);
                                                total++;
                                                previous = current;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (total > 0) {
                            averageSimilarity = sum / (double) total;
                        } else {
                            averageSimilarity = 1.0;
                        }
                    } else {
                        averageSimilarity = 1.0;
                    }
                }
                double result = averageViolationVelocity / violationCount * averageSimilarity;

                if (result == Double.POSITIVE_INFINITY
                        || result == Double.NEGATIVE_INFINITY) {
                    return CheckDetection.defaultData();
                } else {
                    CheckDetection.setDefaultData(result);
                    return result;
                }
            }
        }
    }

    @Override
    protected final int getDataCount(Check.DataType dataType) {
        return this.dataCount[dataType.ordinal()];
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
                if (profile.getRunner(this.hackType).getDetection(this.name).hasData(profile, dataType)) {
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
