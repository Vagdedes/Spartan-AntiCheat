package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.Timestamp;
import java.util.Map;
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
    private final int[] mines;
    private final Map<String, Boolean> days;

    MiningHistory(MiningOre ore, int mines) {
        World.Environment[] environments = World.Environment.values();
        this.mines = new int[environments.length];

        for (World.Environment environment : environments) {
            this.mines[environment.ordinal()] = mines;
        }
        this.ore = ore;
        this.days = new ConcurrentHashMap<>();
    }

    public int getMines() {
        int total = 0;

        for (int mines : this.mines) {
            total += mines;
        }
        return total;
    }

    public int getMines(World.Environment environment) {
        return mines[environment.ordinal()];
    }

    public int increaseMines(World.Environment environment, int amount, String date) {
        days.putIfAbsent(date, null);
        return mines[environment.ordinal()] += amount;
    }

    public int increaseMines(World.Environment environment, int amount) {
        return increaseMines(
                environment,
                amount,
                new Timestamp(System.currentTimeMillis()).toString().substring(0, 10)
        );
    }

    public int getDays() {
        return days.size();
    }
}
