package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MiningHistory {

    public final Enums.MiningOre ore;
    private final int[] mines;
    private final Set<String> days;

    MiningHistory(Enums.MiningOre ore, int mines) {
        World.Environment[] environments = World.Environment.values();
        this.mines = new int[environments.length];

        for (World.Environment environment : environments) {
            this.mines[environment.ordinal()] = mines;
        }
        this.ore = ore;
        this.days = Collections.synchronizedSet(new HashSet<>());
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
        synchronized (days) {
            days.add(date);
        }
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
