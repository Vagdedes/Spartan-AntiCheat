package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.handlers.connection.Latency;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.objects.system.LagLeniency;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LagLeniencies {

    // Useful for the following:
    // vehicles: detections that are related to riding vehicles
    // combat: detections that track combat interactions
    // schedulers: detections that run every one to few ticks
    // building: detections that are related to placing/breaking blocks
    // packets: detections that count movement packets
    // repetition: detections that work by tracking repeated situations
    // inventory: detections that are related to inventories
    // base: anything related to the base, system, e.t.c part of the plugin
    // stable: detections that require stable movement
    // item: detections that track held item interactions
    // library: situations where server libraries become unreliable
    // instability: situations where a detection's calculations stop being reliable

    public static final int
            excellentPing = 50,
            goodPing = 75,
            mediocrePing = 150,
            continentPing = 200,
            averagePing = 250,
            badPing = 300,
            unplayablePing = 500,
            maxAllowedPing = 1000,
            maxServerPing = 5000;

    private static final int
            cooldownTicks = 20,
            criticalDifference = 33,
            maxDifference = (criticalDifference * 3) + 1;
    private static final double testServerMaximumDelay = TPS.maximum / 4.0;

    private static final Map<UUID, LagLeniency> hm = new LinkedHashMap<>(Enums.HackType.values().length);

    public static void clear() {
        hm.clear();
    }

    public static void remove(SpartanPlayer p) {
        hm.remove(p.getUniqueId());
    }

    public static void add(SpartanPlayer p) {
        hm.put(p.getUniqueId(), new LagLeniency(p));
    }

    public static void cache() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                add(p);
            }
        }
    }

    // Separator

    public static boolean hasPingExemption(SpartanPlayer p) {
        boolean pingException = p.getProfile().isSuspectedOrHacker()
                || p.getTicksOnAir() >= 100
                || p.getExtraPackets() >= TPS.maximum;

        if (!pingException) {
            int cancelViolations = 0;
            ResearchEngine.DataType dataType = p.getDataType();

            for (LiveViolation liveViolation : p.getViolations()) {
                if (liveViolation.getLevel() >= CancelViolation.get(liveViolation.getHackType(), dataType)) {
                    cancelViolations += 1;

                    if (cancelViolations >= Check.hackerCheckAmount) {
                        pingException = true;
                        break;
                    }
                }
            }
        }
        return pingException;
    }

    public static boolean hasHighLatency(SpartanPlayer p, int i) { // Check Lag Leniencies protection for more info about usages
        if (!Latency.canUseProtection()) {
            return false;
        }
        int ping = p.getPing();
        return ping >= i && ping <= maxAllowedPing && !hasPingExemption(p);
    }

    // Separator

    public static double getDelay(SpartanPlayer p) {
        double pingDelay;

        if (!Latency.canUseProtection() || hasPingExemption(p)) {
            pingDelay = 0.0;
        } else {
            int latency = p.getPing();

            if (latency <= goodPing) {
                pingDelay = 0.0;
            } else {
                pingDelay = Math.min(
                        hasInconsistencies(p, "ping") ? Math.max(badPing, latency) : latency,
                        Math.min(Config.settings.getInteger("Protections.max_supported_player_latency"), maxAllowedPing)
                );
            }
        }

        // Separator
        double tpsDelay;

        if (!Config.settings.getBoolean(Config.settings.tpsProtectionOption)) {
            tpsDelay = 0.0;
        } else {
            double tps = TPS.get(p, false);

            if (tps >= TPS.excellent) {
                tpsDelay = 0.0;
            } else {
                tpsDelay = TPS.maximum - tps;
            }
        }

        // Separator
        return Math.min(
                Math.max(tpsDelay, pingDelay > excellentPing ? (pingDelay - excellentPing) / ((double) excellentPing) : 0),
                TestServer.isIdentified() ? testServerMaximumDelay : TPS.maximum
        );
    }

    public static double getDelaySimplified(double delay) {
        return delay > 0.0 && delay < 0.5 ? 0.5 : AlgebraUtils.roundToHalf(delay);
    }

    // Spearator

    public static boolean hasInconsistencies(SpartanPlayer p, String key) {
        // Null = Calculate, Empty Key = Basics, Normal key = Cache
        String structure = "lag-leniences=feature=";

        if (key == null) {
            boolean tps = Config.settings.getBoolean(Config.settings.tpsProtectionOption),
                    ping = Latency.canUseProtection();

            if (tps || ping) {
                boolean result = false;
                UUID uuid = p.getUniqueId();
                LagLeniency data = hm.get(uuid);

                if (data != null) {
                    // TPS Protection
                    if (tps) {
                        double current = TPS.get(p, false);

                        if (current < TPS.excellent) {
                            double previous = data.getTPS();

                            if (previous > 0.0 && (previous - TPS.get(p, false)) >= TPS.criticalDifference) {
                                TPS.setProtection(p, cooldownTicks);
                                result = true;
                            }
                        }
                    }

                    // Latency Protection
                    if (ping && data.getCreationTime() <= 30_000L) {
                        int current = p.getPing();

                        if (current > goodPing && current <= averagePing) {
                            int previous = data.getPing();

                            if (previous > 0 // Ensure the previous latency has been calculated
                                    && current >= (previous + criticalDifference) // Check if the threshold has been exceeded
                                    && (current - previous) <= maxDifference) { // Limit the threshold to account for ping-spoof
                                p.getCooldowns().add(structure + "ping", cooldownTicks);
                                result = true;
                            }
                        }
                    }
                }
                hm.put(uuid, new LagLeniency(p));
                return result || hasInconsistencies(p, "");
            }
            return false;
        }
        return key.isEmpty() ? (TPS.areDropping(p) || !p.getCooldowns().canDo(structure + "ping")) :
                (key.equals("tps") ? TPS.areDropping(p) : !p.getCooldowns().canDo(structure + key));
    }
}
