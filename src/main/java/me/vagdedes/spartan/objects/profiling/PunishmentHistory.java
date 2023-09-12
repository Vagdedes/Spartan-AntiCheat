package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PunishmentHistory {

    private int bans, kicks, warnings;
    private final LinkedList<PlayerReport> reports;

    PunishmentHistory() {
        this.bans = 0;
        this.kicks = 0;
        this.warnings = 0;
        this.reports = new LinkedList<>();
    }

    public int getOverall(boolean reports) {
        return bans + kicks + warnings + (reports ? this.reports.size() : 0);
    }

    public int getBans() {
        return bans;
    }

    public void increaseBans() {
        bans += 1;
    }

    public int getKicks() {
        return kicks;
    }

    public void increaseKicks() {
        kicks += 1;
    }

    public int getWarnings() {
        return warnings;
    }

    public void increaseWarnings() {
        warnings += 1;
    }

    public List<PlayerReport> getReports() {
        return new LinkedList<>(reports);
    }

    public int getReportCount() {
        return reports.size();
    }

    public List<PlayerReport> getReports(Enums.HackType hackType, int days) {
        if (reports.size() > 0) {
            List<PlayerReport> recentReports = new LinkedList<>();
            boolean hasDays = days > 0,
                    hasHackType = hackType != null;
            long time = hasDays ? System.currentTimeMillis() : 0L,
                    timeRequirement = hasDays ? days * (86_400L * 1_000L) : 0L;

            for (PlayerReport playerReport : getReports()) {
                if ((!hasDays || (time - playerReport.getDate().getTime()) <= timeRequirement)
                        && (!hasHackType || playerReport.isRelated(hackType))) {
                    recentReports.add(playerReport);
                }
            }
            return recentReports;
        }
        return new ArrayList<>(0);
    }

    public int getReportCount(Enums.HackType hackType, int days) {
        return getReports(hackType, days).size();
    }

    public PlayerReport getReport(String reason, boolean checkIfDismissed) {
        for (PlayerReport playerReport : getReports()) {
            if ((!checkIfDismissed || !playerReport.isDismissed()) && playerReport.getReason().equals(reason)) {
                return playerReport;
            }
        }
        return null;
    }

    public void increaseReports(SpartanPlayer reportedPlayer, String reportedName, String reason, Timestamp date, boolean dismissed) {
        reports.add(new PlayerReport(reportedPlayer, reportedName, reason, date, dismissed));
    }
}
