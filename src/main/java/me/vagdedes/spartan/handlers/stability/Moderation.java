package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.notifications.clickablemessage.ClickableMessage;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Moderation {

    public static final String
            warningMessage = " was warned for ",
            reportMessage = " was reported for ",
            kickMessage = " was kicked for ",
            falsePositiveDisclaimer = "§4(False Positive)§f ",
            dismissedReportMessage = "Dismissed report against ",
            criticalHitMessage = " landed a critical hit with ";

    private static boolean isDetected(SpartanPlayer p, boolean checkSilent, long ms) {
        LiveViolation liveViolation = p.getLastViolation();
        return liveViolation.getLastViolationTime(true) <= ms
                && (!checkSilent
                || !liveViolation.getHackType().getCheck().isSilent(p.getWorld().getName(), p.getUniqueId()));
    }

    public static boolean wasDetected(SpartanPlayer p) {
        return isDetected(p, false, 105L);
    }

    public static boolean isDetectedAndPrevented(SpartanPlayer p) {
        return isDetected(p, true, 55L);
    }

    public static void warn(CommandSender punisher, SpartanPlayer t, String reason) {
        String punisherName = punisher instanceof ConsoleCommandSender ? Config.messages.getColorfulString("console_name") : punisher.getName(),
                warning = ConfigUtils.replaceWithSyntax(t,
                        Config.messages.getColorfulString("warning_message").replace("{reason}", reason).replace("{punisher}", punisherName),
                        null),
                feedback = ConfigUtils.replaceWithSyntax(t,
                        Config.messages.getColorfulString("warning_feedback_message").replace("{reason}", reason).replace("{punisher}", punisherName),
                        null);
        t.sendMessage(warning);
        punisher.sendMessage(feedback);

        SpartanLocation location = t.getLocation();
        CrossServerInformation.queueNotificationWithWebhook(t.getUniqueId(), t.getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                "Warning", reason,
                true);
        AntiCheatLogs.logInfo(Config.getConstruct() + t.getName() + warningMessage + reason);

        t.getProfile().getPunishmentHistory().increaseWarnings();
    }

    public static void report(SpartanPlayer reporter, SpartanPlayer reported, String reason) {
        if (reporter == reported) {
            reporter.sendMessage(ConfigUtils.replaceWithSyntax(reporter, Config.messages.getColorfulString("failed_command"), null));
            return;
        }
        String report = Config.messages.getColorfulString("report_message").replace("{reason}", reason).replace("{reported}", reported.getName());
        report = ConfigUtils.replaceWithSyntax(reporter, report, null);
        boolean sent = false;
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                if (DetectionNotifications.hasPermission(p)) {
                    if (p.getUniqueId().equals(reporter.getUniqueId())) {
                        sent = true;
                    }
                    p.sendMessage(report);
                }
            }
        }
        if (!sent) {
            reporter.sendMessage(report);
        }

        SpartanLocation location = reporter.getLocation();
        CrossServerInformation.queueNotificationWithWebhook(reporter.getUniqueId(), reporter.getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                "Report -> " + reported.getName(), reason,
                false);

        String name = reported.getName();
        AntiCheatLogs.logInfo(Config.getConstruct() + name + reportMessage + reason);

        reported.getProfile().getPunishmentHistory().increaseReports(reported, name, reason, new Timestamp(System.currentTimeMillis()), false);
    }

    public static void kick(CommandSender punisher, SpartanPlayer p, String reason) {
        String punisherName = punisher instanceof ConsoleCommandSender ? Config.messages.getColorfulString("console_name") : punisher.getName(),
                kick = ConfigUtils.replaceWithSyntax(p,
                        Config.messages.getColorfulString("kick_reason").replace("{reason}", reason).replace("{punisher}", punisherName),
                        null),
                announcement = ConfigUtils.replaceWithSyntax(p,
                        Config.messages.getColorfulString("kick_broadcast_message").replace("{reason}", reason).replace("{punisher}", punisherName),
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

        SpartanLocation location = p.getLocation();
        CrossServerInformation.queueNotificationWithWebhook(p.getUniqueId(), p.getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                "Kick", reason,
                true);

        AntiCheatLogs.logInfo(Config.getConstruct() + p.getName() + kickMessage + reason);

        p.getProfile().getPunishmentHistory().increaseKicks();
        p.kickPlayer(kick);
    }

    public static void detection(UUID uuid,
                                 SpartanPlayer detectedPlayer,
                                 PlayerProfile playerProfile,
                                 HackType hackType,
                                 Check check,
                                 String detailedDetection,
                                 String info,
                                 int level,
                                 boolean log, boolean falsePositive, boolean canPrevent, boolean hacker,
                                 ResearchEngine.DataType dataType) {
        if (level < Check.maxViolationsPerCycle) {
            // Variables
            String name = detectedPlayer.getName(),
                    world = detectedPlayer.getWorld().getName();
            boolean individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
            int cancelViolation = CancelViolation.get(hackType, dataType),
                    playerCount = SpartanBukkit.getPlayerCount();

            // Message Preparation
            String shorterMessage, message = Config.messages.getColorfulString("detection_notification");

            if (ConfigUtils.contains(message, "{info}")) {
                if (falsePositive) {
                    message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(detectedPlayer, message, hackType);
                } else {
                    message = ConfigUtils.replaceWithSyntax(detectedPlayer, message, hackType);
                }

                // Separator
                message = message.replace("{info}", info);

                if (individualOnlyNotifications) {
                    shorterMessage = message;
                } else {
                    shorterMessage = detailedDetection != null ? message.replace("{info}", detailedDetection) : message;
                }
            } else {
                if (falsePositive) {
                    message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(detectedPlayer, message, hackType);
                } else {
                    message = ConfigUtils.replaceWithSyntax(detectedPlayer, message, hackType);
                }
                shorterMessage = message;
            }

            // Cross-Server & Console Notifications
            boolean cannotPunish = !Check.hasPunishCapabilities(hackType),
                    consoleLog = !falsePositive && level >= cancelViolation,
                    important = hacker
                            || cannotPunish
                            || canPrevent
                            || consoleLog
                            || playerProfile.isSuspected();

            if (important) {
                CrossServerInformation.queueNotification(message, true);
            }

            // Logs
            if (log) {
                SpartanLocation location = detectedPlayer.getLocation();
                String cancelViolationString = (canPrevent ? "-" : "") + cancelViolation + (check.hasCancelViolation() ? "*" : ""),
                        information = (falsePositive ? "(False Positive) " : "")
                                + Config.getConstruct() + name + " failed " + hackType + " (VL: " + level + ") " +
                                "[(Version: " + MultiVersion.fork() + " " + MultiVersion.versionString() + "), (C-V: " + cancelViolationString + ") (Silent: " + check.isSilent(world, uuid) + "), " +
                                "(Ping: " + detectedPlayer.getPing() + "ms), (TPS: " + AlgebraUtils.cut(TPS.get(detectedPlayer, false), 3) + "), (Hacker: " + hacker + "), (Online: " + playerCount + "), " +
                                "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), (" + info + ")]";
                AntiCheatLogs.logInfo(detectedPlayer, information, consoleLog ? shorterMessage : null, null, hackType, falsePositive, true, level, cancelViolation);
            }

            // Local Notifications
            String command = Config.settings.getString("Notifications.message_clickable_command").replace("{player}", name);

            if (individualOnlyNotifications) {
                Integer divisor = DetectionNotifications.getDivisor(detectedPlayer, false);

                if (DetectionNotifications.canAcceptMessages(detectedPlayer, divisor, false)
                        && isDivisorValid(level, divisor, hacker || falsePositive || cannotPunish || TestServer.isIdentified())) { // Attention
                    ClickableMessage.sendCommand(detectedPlayer, message, "Command: " + command, command);
                }
            } else {
                List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

                if (notificationPlayers.size() > 0) {
                    boolean testServer = TestServer.isIdentified(),
                            uncertain = hacker || falsePositive || testServer || cannotPunish; // Attention

                    for (SpartanPlayer staff : notificationPlayers) {
                        int divisor = NotifyViolation.get(staff, detectedPlayer, cancelViolation, playerCount, testServer);

                        if (isDivisorValid(level, divisor, uncertain)) {
                            ClickableMessage.sendCommand(
                                    staff,
                                    divisor == 1 ? message : shorterMessage,
                                    "Command: " + command,
                                    command
                            );
                        }
                    }
                }
            }
        }
    }

    public static void performPunishments(SpartanPlayer p, HackType hackType, int violation, ResearchEngine.DataType dataType) {
        Check check = hackType.getCheck();

        if (check.canPunish()) {
            boolean legacy = Config.isLegacy();
            String[] commands = legacy ? check.getLegacyCommands(violation) : check.getCommands();

            if (commands.length > 0) {
                Player n = p.getPlayer();

                if (n != null && n.isOnline()) {
                    boolean performed = false, found = false;
                    UUID uuid = p.getUniqueId();
                    String name = p.getName();
                    SpartanLocation location = p.getLocation();

                    if (legacy) {
                        boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");

                        for (String command : commands) {
                            if (command != null) {
                                found = true;
                                String modifiedCommand = ConfigUtils.replaceWithSyntax(p, command, hackType);

                                if (enabledDeveloperAPI) {
                                    PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, new HackType[]{hackType}, modifiedCommand);
                                    Register.manager.callEvent(event);

                                    if (event.isCancelled()) {
                                        continue;
                                    }
                                }
                                performed = true;
                                SpartanBukkit.runDelayedTask(p, () -> {
                                    if (p != null) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                    }
                                }, 1);
                            }
                        }
                    } else {
                        Collection<HackType> detectedHacks = Config.getPunishableHackModules(p, hackType, violation, dataType);

                        if (detectedHacks.size() > 0) {
                            boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                            HackType[] hackTypes = detectedHacks.toArray(new HackType[0]);
                            StringBuilder stringBuilder = new StringBuilder();

                            for (HackType detectedHack : detectedHacks) {
                                stringBuilder.append(detectedHack.getCheck().getName()).append(", ");
                            }
                            String detections = stringBuilder.substring(0, stringBuilder.length() - 2);

                            for (String command : commands) {
                                if (command != null) {
                                    found = true;
                                    String modifiedCommand = ConfigUtils.replaceWithSyntax(
                                            p,
                                            command.replaceAll("\\{detections}|\\{detection}", detections),
                                            null
                                    );

                                    if (enabledDeveloperAPI) {
                                        PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, hackTypes, modifiedCommand);
                                        Register.manager.callEvent(event);

                                        if (event.isCancelled()) {
                                            continue;
                                        }
                                    }

                                    performed = true;
                                    SpartanBukkit.runDelayedTask(p, () -> {
                                        if (p != null) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                        }
                                    }, 1);
                                }
                            }
                        }
                    }

                    if (performed) {
                        String commandsString = StringUtils.toString(commands, "\n");

                        if (commandsString.length() > 0) {
                            SpartanBukkit.runDelayedTask(p, () ->
                                    CrossServerInformation.queueWebhook(uuid, name,
                                            location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                                            "Punishment", commandsString), 2);
                        }
                    } else if (found && AwarenessNotifications.canSend(SpartanBukkit.uuid, hackType + "-cancelled-punishment-event")) {
                        String notification = "Just a reminder that the punishments of the '" + hackType + "' check were just cancelled via code by a third-party plugin."
                                + " Please do not reach support for this as it relates only to your server.";
                        List<Plugin> dependentPlugins = PluginUtils.getDependentPlugins(Register.plugin.getName());

                        if (dependentPlugins.size() > 0) {
                            StringBuilder dependentPluginNames = new StringBuilder();

                            for (Plugin plugin : dependentPlugins) {
                                dependentPluginNames.append(dependentPluginNames.length() == 0 ? "" : ", ").append(plugin.getName());
                            }
                            notification += " Here are possible plugins that could be doing this:\n" + dependentPluginNames;
                        }
                        AwarenessNotifications.forcefullySend(notification);
                    }
                }
            } else if (AwarenessNotifications.canSend(SpartanBukkit.uuid, hackType + "-no-punishment-commands")) {
                AwarenessNotifications.forcefullySend("Just a reminder that you have set no punishment commands for the '" + hackType + "' check.");
            }
        } else if (check.canPunish() && AwarenessNotifications.canSend(SpartanBukkit.uuid, hackType + "-disabled-punishments")) {
            AwarenessNotifications.forcefullySend("Just a reminder that punishments have been disabled for the '" + hackType + "' check.");
        }
    }

    // Handlers

    private static boolean isDivisorValid(int level, int divisor, boolean uncertain) {
        if (level > 0) {
            return divisor > 0 && (level % divisor) == 0 // Basic
                    || divisor < 0 && level >= -divisor; // Secret
        }
        return uncertain && (divisor == 0 || divisor == 1); // Uncertain
    }
}
