package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Player;

public class Latency {

    public static int ping(Player p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return p.getPing();
        } else {
            try {
                Object obj = SpartanBukkit.getCraftPlayerMethod(p, "ping");
                return obj instanceof Integer ? Math.max((int) obj, 0) : 0;
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    public static double getDelay(SpartanPlayer player) {
        double pingDelay;

        if (Config.settings.getInteger(Settings.maxSupportedLatencyOption) <= 0
                || player.getProfile().isSuspectedOrHacker()) {
            pingDelay = 0.0;
        } else {
            int latency = player.getPing();

            if (latency <= TPS.tickTimeInteger) {
                pingDelay = 0.0;
            } else {
                pingDelay = Math.min(
                        latency,
                        Math.min(
                                Config.settings.getInteger("Protections.max_supported_player_latency"),
                                1000
                        )
                );
            }
        }

        // Separator
        double tpsDelay;

        if (!Config.settings.getBoolean(Settings.tpsProtectionOption)) {
            tpsDelay = 0.0;
        } else {
            double tps = TPS.get(player, false);

            if (tps >= TPS.excellent) {
                tpsDelay = 0.0;
            } else {
                tpsDelay = TPS.maximum - tps;
            }
        }

        // Separator
        return Math.min(
                Math.max(
                        tpsDelay, pingDelay > TPS.tickTimeInteger
                                ? (pingDelay - TPS.tickTimeInteger) / TPS.tickTimeDecimal
                                : 0
                ),
                TPS.maximum
        );
    }
}
