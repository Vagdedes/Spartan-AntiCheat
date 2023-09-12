package me.vagdedes.spartan.handlers.connection;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Player;

public class Latency {

    public static boolean canUseProtection() {
        return Settings.getInteger("Protections.max_supported_player_latency") > 0;
    }

    public static int ping(Player p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return p.getPing();
        } else {
            try {
                Object obj = SpartanBukkit.getCraftPlayerMethod(p, "ping");

                if (obj instanceof Integer) {
                    return Math.max((int) obj, 0);
                } else {
                    return 0;
                }
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

}
