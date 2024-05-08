package com.vagdedes.spartan.abstraction.replicates;

import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SpartanPunishments {

    private final SpartanPlayer parent;

    public SpartanPunishments(SpartanPlayer player) {
        this.parent = player;
    }

    public void kick(CommandSender punisher, String reason) {
        Player target = this.parent.getPlayer();

        if (target != null && target.isOnline()) {
            String punisherName = punisher instanceof ConsoleCommandSender
                    ? Config.messages.getColorfulString("console_name")
                    : punisher.getName(),
                    kick = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("kick_reason")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null),
                    announcement = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("kick_broadcast_message")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null);

            if (Config.settings.getBoolean("Punishments.broadcast_on_punishment")) {
                Bukkit.broadcastMessage(announcement);
            } else {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    for (SpartanPlayer o : players) {
                        if (DetectionNotifications.hasPermission(o)) {
                            o.sendMessage(announcement);
                        }
                    }
                }
            }

            this.parent.getProfile().punishmentHistory.increaseKicks(this.parent, kick);
            target.kickPlayer(kick);
        }
    }

    public void warn(CommandSender punisher, String reason) {
        Player target = this.parent.getPlayer();

        if (target != null && target.isOnline()) {
            String punisherName = punisher instanceof ConsoleCommandSender
                    ? Config.messages.getColorfulString("console_name")
                    : punisher.getName(),
                    warning = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("warning_message")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null),
                    feedback = ConfigUtils.replaceWithSyntax(target,
                            Config.messages.getColorfulString("warning_feedback_message")
                                    .replace("{reason}", reason)
                                    .replace("{punisher}", punisherName),
                            null);
            target.sendMessage(warning);
            punisher.sendMessage(feedback);

            this.parent.getProfile().punishmentHistory.increaseWarnings(this.parent, reason);
        }
    }
}
