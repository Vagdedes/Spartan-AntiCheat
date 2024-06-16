package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SpartanPunishments {

    private final SpartanPlayer parent;
    private long kickCooldown, warnCooldown;

    public SpartanPunishments(SpartanPlayer player) {
        this.parent = player;
    }

    public boolean kick(CommandSender punisher, String reason) {
        if (kickCooldown < System.currentTimeMillis()) {
            kickCooldown = System.currentTimeMillis() + 1_000L;
            Player target = this.parent.getInstance();
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

            SpartanBukkit.transferTask(
                    this.parent,
                    () -> target.kickPlayer(kick)
            );
            return true;
        } else {
            return false;
        }
    }

    public boolean warn(CommandSender punisher, String reason) {
        if (warnCooldown < System.currentTimeMillis()) {
            warnCooldown = System.currentTimeMillis() + 1_000L;
            Player target = this.parent.getInstance();

            if (target != null) {
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
            return true;
        } else {
            return false;
        }
    }

}
