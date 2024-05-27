package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;

public class PunishmentHistory {

    public static final String
            punishmentMessage = " was punished with the commands: ",
            warningMessage = " was warned for ",
            kickMessage = " was kicked for ";

    private int kicks, warnings, punishments;

    PunishmentHistory() {
        this.kicks = 0;
        this.warnings = 0;
        this.punishments = 0;
    }

    public int getKicks() {
        return kicks;
    }

    public void increaseKicks(SpartanPlayer player, String reason) {
        kicks++;

        if (reason != null) {
            AntiCheatLogs.logInfo(player,
                    Config.getConstruct() + player.name + kickMessage + reason,
                    true
            );
            SpartanLocation location = player.movement.getLocation();
            CrossServerInformation.queueNotification(
                    reason,
                    true
            );
            CloudConnections.executeDiscordWebhook(
                    "punishments",
                    player.uuid,
                    player.name,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    "Kick",
                    reason
            );
        }
    }

    public int getPunishments() {
        return punishments;
    }

    public int getWarnings() {
        return warnings;
    }

    public void increaseWarnings(SpartanPlayer player, String reason) {
        warnings++;

        if (reason != null) {
            AntiCheatLogs.logInfo(
                    player,
                    Config.getConstruct() + player.name + warningMessage + reason,
                    true
            );
            SpartanLocation location = player.movement.getLocation();
            CrossServerInformation.queueNotification(
                    reason,
                    true
            );
            CloudConnections.executeDiscordWebhook(
                    "punishments",
                    player.uuid,
                    player.name,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    "Warning",
                    reason
            );
        }
    }

    public void increasePunishments(SpartanPlayer player, String commands) {
        punishments++;

        if (commands != null) {
            AntiCheatLogs.logInfo(player,
                    Config.getConstruct() + player.name + punishmentMessage + commands,
                    true
            );
            SpartanLocation location = player.movement.getLocation();
            CloudConnections.executeDiscordWebhook(
                    "punishments",
                    player.uuid,
                    player.name,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    "Punishment",
                    commands
            );
        }
    }

}
