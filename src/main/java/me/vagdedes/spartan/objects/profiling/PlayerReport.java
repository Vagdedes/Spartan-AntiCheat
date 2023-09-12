package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.configuration.AntiCheatLogs;
import me.vagdedes.spartan.features.notifications.AwarenessNotifications;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

public class PlayerReport {

    private final String reported, reason;
    private final Timestamp date;
    private boolean dismissed;
    private final Set<Enums.HackType> relations;

    PlayerReport(SpartanPlayer reportedPlayer, String reportedName, String reason, Timestamp date, boolean dismissed) {
        PlayerProfile playerProfile = reportedPlayer != null ? reportedPlayer.getProfile() : null;
        boolean hasProfile = playerProfile != null;
        String reasonModified = reason.toLowerCase();
        Set<Enums.HackType> relations = new HashSet<>();

        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (reasonModified.contains(hackType.toString().toLowerCase())
                    || reasonModified.contains(hackType.getCheck().getName().toLowerCase())
                    || hasProfile && playerProfile.isSuspectedOrHacker() && playerProfile.getEvidence().has(hackType)) {
                relations.add(hackType);
            }
        }

        // Separator
        this.reported = reportedName;
        this.reason = reason;
        this.date = date;
        this.dismissed = dismissed;
        this.relations = relations;
    }

    public String getReported() {
        return reported;
    }

    public String getReason() {
        return reason;
    }

    public Timestamp getDate() {
        return date;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public Set<Enums.HackType> getRelations() {
        return new HashSet<>(relations);
    }

    public boolean isRelated(Enums.HackType hackType) {
        return relations.contains(hackType);
    }

    public boolean dismiss(String name, Object executor, boolean log) {
        if (!isDismissed()) {
            dismissed = true;
            String newLog = Moderation.dismissedReportMessage + name + " with reason: " + reason;

            if (log) {
                AntiCheatLogs.logInfo(Config.getConstruct() + newLog);
            }
            if (executor != null) {
                AwarenessNotifications.forcefullySend(executor, newLog);
            }
            return true;
        }
        return false;
    }
}
