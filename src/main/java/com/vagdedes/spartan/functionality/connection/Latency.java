package com.vagdedes.spartan.functionality.connection;

import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
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
        int max = Config.settings.getInteger("Protections.max_supported_player_latency");

        if (max <= 0) {
            pingDelay = 0.0;
        } else {
            int latency = player.getPing();

            if (latency <= TPS.tickTimeInteger) {
                pingDelay = 0.0;
            } else {
                pingDelay = Math.min(latency, max);
            }
        }

        // Separator
        double tpsDelay;

        if (!Config.settings.getBoolean(Settings.tpsProtectionOption)) {
            tpsDelay = 0.0;
        } else {
            double tps = TPS.get();

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
