package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.system.Enums;

public class DynamicPunishment {

    private final Enums.HackType hackType;
    private int amount;

    DynamicPunishment(Enums.HackType hackType) {
        this.hackType = hackType;
        this.amount = 0;
    }

    public Enums.HackType getHackType() {
        return hackType;
    }

    public int getAmount() {
        return amount;
    }

    public int increaseAmount() {
        return amount += 1;
    }
}
