package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.ultimatestatistics.api.UltimateStatisticsAPI;
import me.vagdedes.ultimatestatistics.system.Enums;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class UltimateStatistics {

    private static final Map<UUID, Boolean> hm = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));

    public static boolean isSuspected(UUID uuid) {
        if (Compatibility.CompatibilityType.UltimateStatistics.isFunctional()) {
            Boolean result = hm.get(uuid);

            if (result != null) {
                return result;
            }
            boolean b = getRatio(uuid) >= 3.0 || getKicks(uuid) >= 25;
            hm.put(uuid, b);
            return b;
        }
        return false;
    }

    private static double getRatio(UUID uuid) {
        Object obj = UltimateStatisticsAPI.getStats(uuid, Enums.EventType.Players_Killed, "amount");

        if (obj instanceof Integer) {
            int kills = (int) obj;
            obj = UltimateStatisticsAPI.getStats(uuid, Enums.EventType.Deaths_By_Player, "amount");

            if (obj instanceof Integer) {
                int deaths = (int) obj;

                if (deaths > 0) {
                    return kills / ((double) deaths);
                }
            }
        }
        return 0.0;
    }

    private static double getKicks(UUID uuid) {
        Object obj = UltimateStatisticsAPI.getStats(uuid, Enums.EventType.Times_Kicked, "amount");
        return obj instanceof Integer ? (int) obj : 0.0;
    }
}
