package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

import java.util.*;

public abstract class ProbabilityDetection extends CheckDetection {

    private int lastProbability;
    private final Long[] lastTime;
    private final Map<Check.DataType, Collection<Long>> data;

    public ProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.lastTime = new Long[Check.DataType.values().length];
    }

    // Data

    @Override
    protected final boolean hasData(Check.DataType dataType) {
        synchronized (this.data) {
            if (this.data.containsKey(dataType)) {
                return this.executor.hasOnlineTime(dataType);
            }
        }
        return false;
    }

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (playerProfiles.size() >= PlayerEvidence.factorRequirement) {
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                if (profile.getRunner(this.hackType).getDetection(this.name).hasData(dataType)) {
                    count++;

                    if (count == PlayerEvidence.factorRequirement) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void clearData(Check.DataType dataType) {
        synchronized (this.data) {
            this.data.remove(dataType);
            this.lastTime[dataType.ordinal()] = null;
        }
    }

    @Override
    public final void store(Check.DataType dataType, long time) {
        synchronized (this.data) {
            Collection<Long> data = this.data.computeIfAbsent(
                    dataType,
                    k -> new TreeSet<>()
            );

            if (data.size() == 2_048) {
                Iterator<Long> iterator = data.iterator();
                iterator.next();
                iterator.remove();
            }
            data.add(time);
            this.lastTime[dataType.ordinal()] = time;
        }
    }

    @Override
    public final Double getData(Check.DataType dataType) {
        double violationCount, averageViolationSpread;

        synchronized (this.data) {
            Collection<Long> data = this.data.get(dataType);

            if (data == null || data.size() == 1) {
                return null;
            } else {
                long squareSum = 0;
                Iterator<Long> iterator = data.iterator();
                long previous = iterator.next();
                violationCount = 0.0;

                while (iterator.hasNext()) {
                    Long current = iterator.next();

                    if (this.executor.wasOnline(dataType, current, previous)) {
                        violationCount++;
                        long difference = current - previous;
                        previous = current;
                        squareSum += difference * difference;
                    } else {
                        iterator.remove();
                    }
                }
                if (violationCount == 0.0) {
                    averageViolationSpread = Double.MAX_VALUE;
                } else {
                    averageViolationSpread = Math.sqrt(squareSum / violationCount);
                    long onlineTime = this.executor.getOnlineTime(dataType);

                    if (onlineTime != 0L) {
                        violationCount = (violationCount + 1.0) / (double) onlineTime;
                    } else {
                        violationCount++;
                    }
                }
            }
        }
        return violationCount == 0.0
                ? null
                : averageViolationSpread / violationCount;
    }

    @Override
    final Long getFirstTime(Check.DataType dataType) {
        Long time = null;

        synchronized (this.data) {
            Collection<Long> data = this.data.get(dataType);

            if (data != null) {
                time = data.iterator().next();
            }
        }
        return time;
    }

    @Override
    final Long getLastTime(Check.DataType dataType) {
        synchronized (this.data) {
            return this.lastTime[dataType.ordinal()];
        }
    }

    @Override
    final double getDataCompletion(Check.DataType dataType) {
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (!playerProfiles.isEmpty()) {
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                if (profile.getRunner(this.hackType).getDetection(this.name).hasData(dataType)) {
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

    // Notification

    @Override
    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();
        SpartanProtocol protocol = detected instanceof SpartanProtocol
                ? (SpartanProtocol) detected
                : null;

        if (this.notifications <= time
                && (this.lastProbability != probability
                || time - this.notifications >= 5_000L
                || (protocol != null
                ? protocol.spartan.equals(this.protocol().spartan)
                : this.protocol.bukkit.getName().equals(detected.toString())))) {
            int ticks = this.executor.getNotificationTicksCooldown(protocol);

            if (ticks > 0) {
                this.notifications = time + (ticks * TPS.tickTime);
            } else {
                this.notifications = time;
            }
            this.lastProbability = probability;
            return true;
        }
        return false;
    }

}
