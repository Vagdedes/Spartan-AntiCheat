package com.vagdedes.spartan.functionality.connection;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Config;
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
        int max = Config.settings.getInteger("Important.max_supported_player_latency");

        if (max <= 0) {
            pingDelay = 0.0;
        } else {
            int latency = player.protocol.getPing();

            if (latency <= TPS.tickTimeInteger) {
                pingDelay = 0.0;
            } else {
                pingDelay = Math.min(latency, max);
            }
        }

        // Separator
        return pingDelay > TPS.tickTimeInteger
                ? (pingDelay - TPS.tickTimeInteger) / TPS.tickTimeDecimal
                : 0;
    }
}
