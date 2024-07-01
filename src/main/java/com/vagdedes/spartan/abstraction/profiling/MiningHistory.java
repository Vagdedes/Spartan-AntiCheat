package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MiningHistory {

    public static MiningHistory.MiningOre getMiningOre(Material material) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
            if (material == Material.ANCIENT_DEBRIS) {
                return MiningHistory.MiningOre.ANCIENT_DEBRIS;
            }
            if (material == Material.GILDED_BLACKSTONE || material == Material.NETHER_GOLD_ORE) {
                return MiningHistory.MiningOre.GOLD;
            }
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                if (material == Material.DEEPSLATE_DIAMOND_ORE) {
                    return MiningHistory.MiningOre.DIAMOND;
                }
                if (material == Material.DEEPSLATE_EMERALD_ORE) {
                    return MiningHistory.MiningOre.EMERALD;
                }
                if (material == Material.DEEPSLATE_GOLD_ORE) {
                    return MiningHistory.MiningOre.GOLD;
                }
            }
        }
        switch (material) {
            case DIAMOND_ORE:
                return MiningHistory.MiningOre.DIAMOND;
            case EMERALD_ORE:
                return MiningHistory.MiningOre.EMERALD;
            case GOLD_ORE:
                return MiningHistory.MiningOre.GOLD;
        }
        return null;
    }

    public enum MiningOre {
        ANCIENT_DEBRIS, DIAMOND, EMERALD, GOLD;

        private final String string;

        MiningOre() {
            string = this.name().toLowerCase().replace("_", "-");
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public final MiningOre ore;
    private final Map<Integer, Map<String, Integer>> count;

    MiningHistory(MiningOre ore) {
        World.Environment[] environments = World.Environment.values();
        this.ore = ore;
        this.count = new ConcurrentHashMap<>(environments.length + 1, 1.0f);
    }

    public Set<Map.Entry<String, Integer>> getMinesEntry(World.Environment environment) {
        return new HashSet<>(
                count.getOrDefault(
                        environment.ordinal(),
                        new HashMap<>(0)
                ).entrySet()
        );
    }

    public double getMinesDeviation(World.Environment environment) {
        Map<String, Integer> map = count.get(environment.ordinal());

        if (map == null || map.isEmpty()) {
            return 0.0;
        } else {
            int mines = 0;

            for (int count : map.values()) {
                mines += count * count;
            }
            return Math.sqrt(mines / ((double) map.size()));
        }
    }

    public void increaseMines(World.Environment environment, int amount, String date) {
        Map<String, Integer> map = count.computeIfAbsent(
                environment.ordinal(),
                k -> new ConcurrentHashMap<>()
        );
        map.put(date, map.getOrDefault(date, 0) + amount);
    }

    public void increaseMines(World.Environment environment, int amount) {
        increaseMines(
                environment,
                amount,
                new Timestamp(System.currentTimeMillis()).toString().substring(0, 10)
        );
    }

}
