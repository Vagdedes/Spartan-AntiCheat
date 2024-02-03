package com.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;

public class MiningHistory {

    private final Enums.MiningOre ore;
    private final int[] mines;
    private int days;

    MiningHistory(Enums.MiningOre ore, int mines, int days) {
        World.Environment[] environments = World.Environment.values();
        this.mines = new int[environments.length];

        for (World.Environment environment : environments) {
            this.mines[environment.ordinal()] = mines;
        }
        this.ore = ore;
        this.days = days;
    }

    public Enums.MiningOre getOre() {
        return ore;
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

    public int increaseMines(World.Environment environment, int amount) {
        return mines[environment.ordinal()] += amount;
    }

    public int getDays() {
        return days;
    }

    public int increaseDays() {
        return days += 1;
    }
}
