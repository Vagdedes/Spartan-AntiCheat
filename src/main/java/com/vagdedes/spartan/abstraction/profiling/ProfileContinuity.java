package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.java.OverflowMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileContinuity {

    private final PlayerProfile profile;
    private final Map<Long, Long>[] continuity;
    private final Map<Integer, Boolean> online;

    public ProfileContinuity(PlayerProfile profile) {
        this.profile = profile;
        this.continuity = new Map[Check.DataType.values().length];
        this.online = new OverflowMap<>(
                new ConcurrentHashMap<>(),
                1_024
        );

        for (Check.DataType dataType : Check.DataType.values()) {
            this.continuity[dataType.ordinal()] = new ConcurrentHashMap<>();
        }
    }

    public void clear() {
        for (Map<Long, Long> map : this.continuity) {
            map.clear();
        }
        this.online.clear();
    }

    public void setActiveTime(long moment, long length, boolean log) {
        if (log) {
            AntiCheatLogs.rawLogInfo(
                    moment,
                    this.profile.name + PlayerProfile.activeFor + length,
                    false,
                    true,
                    true
            );
        }
        this.continuity[profile.getLastDataType().ordinal()].put(moment, length);
    }

    public boolean wasOnline(long current, long previous) {
        int hash = (Long.hashCode(current) * SpartanBukkit.hashCodeMultiplier) + Long.hashCode(previous);
        Boolean cache = this.online.get(hash);

        if (cache != null) {
            return cache;
        }
        SpartanProtocol protocol = this.profile.protocol();

        if (protocol != null
                && current >= protocol.getActiveCreationTime()
                && previous >= protocol.getActiveCreationTime()) {
            return !protocol.spartan.isAFK();
        } else {
            Map<Long, Long> data = this.continuity[
                    (protocol == null
                            ? profile.getLastDataType()
                            : protocol.spartan.dataType).ordinal()
                    ];

            if (!data.isEmpty()) {
                for (Map.Entry<Long, Long> entry : data.entrySet()) {
                    long length = entry.getValue(),
                            moment = entry.getKey();

                    if (previous >= (moment - length)
                            && current <= moment) {
                        this.online.put(hash, true);
                        return true;
                    }
                }
            }
            this.online.put(hash, false);
            return false;
        }
    }

    public boolean hasOnlineTime() {
        SpartanProtocol protocol = this.profile.protocol();
        return !this.continuity[
                (protocol == null
                        ? profile.getLastDataType()
                        : protocol.spartan.dataType).ordinal()
                ].isEmpty();
    }

    public long getOnlineTime() {
        SpartanProtocol protocol = this.profile.protocol();
        long sum = 0L;

        Map<Long, Long> data = this.continuity[
                (protocol == null
                        ? profile.getLastDataType()
                        : protocol.spartan.dataType).ordinal()
                ];

        if (!data.isEmpty()) {
            for (long value : data.values()) {
                sum += value;
            }
        }
        return protocol == null || protocol.spartan.isAFK()
                ? sum
                : protocol.getActiveTimePlayed() + sum;
    }

}
