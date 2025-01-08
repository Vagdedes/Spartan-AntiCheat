package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileContinuity {

    private final PlayerProfile profile;
    private final Map<Check.DataType, Map<Long, Long>> continuity;
    private final Map<Integer, Boolean> online;

    public ProfileContinuity(PlayerProfile profile) {
        this.profile = profile;
        this.continuity = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.online = new ConcurrentHashMap<>();
    }

    public void clear() {
        synchronized (this.continuity) {
            this.continuity.clear();
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
        synchronized (this.continuity) {
            this.continuity.computeIfAbsent(
                    profile.getLastDataType(),
                    k -> new TreeMap<>()
            ).put(moment, length);
        }
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
            synchronized (this.continuity) {
                Map<Long, Long> data = this.continuity.get(
                        protocol == null
                                ? profile.getLastDataType()
                                : protocol.spartan.dataType
                );

                if (data != null) {
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
            }
            this.online.put(hash, false);
            return false;
        }
    }

    public long getOnlineTime() {
        SpartanProtocol protocol = this.profile.protocol();
        long sum = 0L;

        synchronized (this.continuity) {
            Map<Long, Long> data = this.continuity.get(
                    protocol == null
                            ? profile.getLastDataType()
                            : protocol.spartan.dataType
            );

            if (data != null) {
                for (long value : data.values()) {
                    sum += value;
                }
            }
        }
        return protocol == null || protocol.spartan.isAFK()
                ? sum
                : protocol.getActiveTimePlayed() + sum;
    }

}
