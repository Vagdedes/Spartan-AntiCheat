package com.vagdedes.spartan.objects.features;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class StatisticalProgress {

    private final int mines, logs, reports, bans, kicks, warnings, staffOffline;
    private final SpartanPlayer[] staffOnline;

    public StatisticalProgress(int mines, int logs, int reports, int bans, int kicks, int warnings, int staffOffline, SpartanPlayer[] staffOnline) {
        this.mines = mines;
        this.logs = logs;
        this.reports = reports;
        this.bans = bans;
        this.kicks = kicks;
        this.warnings = warnings;
        this.staffOffline = staffOffline;
        this.staffOnline = staffOnline;
    }

    public StatisticalProgress() {
        this(0, 0, 0, 0, 0, 0, 0, new SpartanPlayer[]{});
    }

    public int getMines() {
        return mines;
    }

    public int getLogs() {
        return logs;
    }

    public int getReports() {
        return reports;
    }

    public int getBans() {
        return bans;
    }

    public int getKicks() {
        return kicks;
    }

    public int getWarnings() {
        return warnings;
    }

    public int getStaffOffline() {
        return staffOffline;
    }

    public SpartanPlayer[] getStaffOnline() {
        return staffOnline;
    }
}
