package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.functionality.moderation.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PlayerPunishments {

    private final PlayerBukkit parent;
    private long kickCooldown, warnCooldown;

    public PlayerPunishments(PlayerBukkit player) {
        this.parent = player;
    }

    public boolean kick(CommandSender punisher, String reason) {
        if (kickCooldown < System.currentTimeMillis()) {
            kickCooldown = System.currentTimeMillis() + 1_000L;
            Player target = this.parent.protocol.bukkit();
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

            Collection<PlayerProtocol> protocols = PluginBase.getProtocols();

            if (!protocols.isEmpty()) {
                for (PlayerProtocol protocol : protocols) {
                    if (DetectionNotifications.hasPermission(protocol)) {
                        protocol.bukkit().sendMessage(announcement);
                    }
                }
            }

            PluginBase.transferTask(
                    this.parent.protocol,
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
            Player target = this.parent.protocol.bukkit();
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
            return true;
        } else {
            return false;
        }
    }

}
