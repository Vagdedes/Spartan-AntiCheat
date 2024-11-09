package com.vagdedes.spartan.functionality.connection;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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

}
