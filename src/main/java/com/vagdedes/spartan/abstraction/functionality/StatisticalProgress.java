package com.vagdedes.spartan.abstraction.functionality;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;

import java.util.ArrayList;
import java.util.Collection;

public class StatisticalProgress {

    public final int mines, logs, kicks, warnings, punishments;
    private final Collection<SpartanPlayer> staffOnline;

    public StatisticalProgress(int mines, int logs,
                               int kicks, int warnings, int punishments,
                               Collection<SpartanPlayer> staffOnline) {
        this.mines = mines;
        this.logs = logs;
        this.kicks = kicks;
        this.punishments = punishments;
        this.warnings = warnings;
        this.staffOnline = staffOnline;
    }

    public StatisticalProgress() {
        this(0, 0, 0, 0, 0, new ArrayList<>(0));
    }

    public Collection<SpartanPlayer> getStaffOnline() {
        return new ArrayList<>(staffOnline);
    }
}
