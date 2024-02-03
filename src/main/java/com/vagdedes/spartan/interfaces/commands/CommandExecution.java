package com.vagdedes.spartan.interfaces.commands;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.compatibility.manual.essential.MinigameMaker;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.moderation.BanManagement;
import com.vagdedes.spartan.functionality.moderation.Wave;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickablemessage.ClickableMessage;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.connection.Piracy;
import com.vagdedes.spartan.handlers.stability.Moderation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.NetworkUtils;
import io.signality.utils.system.Events;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.*;

public class CommandExecution implements CommandExecutor {

    public static final int maxConnectedArgumentLength = 4096;
    private static final String[] timeCharacters = new String[]{"m", "h", "d", "w", "y"};

    private static void buildCommand(CommandSender sender, ChatColor chatColor, String command, String description) {
        ClickableMessage.sendCommand(sender, chatColor + command, description, command);
    }

    public static boolean isCustomCommand(String command) {
        return command.length() > 0 && !command.toLowerCase().startsWith(Register.plugin.getName().toLowerCase());
    }

    public static boolean spartanMessage(CommandSender sender, boolean isPlayer) {
        if (!isPlayer || Permissions.has((Player) sender)) {
            String v = API.getVersion();
            sender.sendMessage("");
            String command = "§4" + SpartanEdition.getProductName(false);

            if (Piracy.enabled) {
                command += " §8[§7(§fVersion: " + v + "§7)§8, §7(§fID: " + IDs.hide(IDs.user()) + "/" + IDs.hide(IDs.nonce()) + "§7)§8]";
            } else if (IDs.isValid()) {
                command += " §8[§7(§fVersion: " + v + "§7)§8, §7(§fID: " + IDs.hide(IDs.user()) + "§7)§8]";
            } else {
                command += " §8" + v;
            }
            sender.sendMessage(command);
            sender.sendMessage("§8§l<> §7Required command argument");
            sender.sendMessage("§8§l[] §7Optional command argument");
            return true;
        }
        sender.sendMessage(Config.messages.getColorfulString("unknown_command"));
        return false;
    }

    public static void completeMessage(CommandSender sender, String list) {
        boolean isPlayer = sender instanceof Player;
        SpartanPlayer player = isPlayer ? SpartanBukkit.getPlayer((Player) sender) : null;
        isPlayer &= player != null;

        if (spartanMessage(sender, isPlayer)) {
            String command = Register.plugin.getName().toLowerCase();

            boolean ban = !isPlayer || Permissions.has(player, Permission.BAN),
                    unban = !isPlayer || Permissions.has(player, Permission.UNBAN);
            boolean info = !isPlayer || Permissions.has(player, Enums.Permission.INFO),
                    manage = !isPlayer || Permissions.has(player, Enums.Permission.MANAGE);

            switch (list) {
                case "default":
                    if ((info || manage) && isPlayer) {
                        buildCommand(sender, ChatColor.GREEN, "/" + command + " menu",
                                "Click this command to open the plugin's inventory menu.");
                    }
                    if (!isPlayer || Permissions.has(player, Permission.ADMIN)) {
                        ClickableMessage.sendCommand(sender, ChatColor.GREEN + "/" + command + " customer-support <discord-tag>", "This command can be used to provide crucial details to the developers about problematic detections.", null);
                        ClickableMessage.sendCommand(sender, ChatColor.GREEN + "/" + command + " customer-support <check> <discord-tag> [explanation]", "This command can be used to provide crucial details to the developers about a check.", null);
                    }
                    if (manage) {
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " toggle <check>",
                                "This command can be used to enable/disable a check and its detections.", null);
                    }
                    if (!isPlayer || Permissions.has(player, Permission.RELOAD)) {
                        buildCommand(sender, ChatColor.RED, "/" + command + " reload",
                                "Click this command to reload the plugin's cache.");
                    }
                    if (!isPlayer || ban || unban || info
                            || Permissions.has(player, Permission.KICK)
                            || Permissions.has(player, Permission.WARN)
                            || Permissions.has(player, Permission.USE_BYPASS)
                            || Permissions.has(player, Permission.WAVE)
                            || Permissions.has(player, Permission.REPORT)) {
                        buildCommand(sender, ChatColor.RED, "/" + command + " moderation",
                                "Click this command to view a list of moderation commands.");
                    }
                    if (!isPlayer || Permissions.has(player, Permission.CONDITION)) {
                        buildCommand(sender, ChatColor.RED, "/" + command + " conditions",
                                "Click this command to view a list of conditional commands.");
                    }
                    break;
                case "moderation":
                    boolean permission = false;

                    if (isPlayer && DetectionNotifications.hasPermission(player)) {
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " notifications [frequency]",
                                "This command can be used to receive chat messages whenever a player is suspected of using hack modules.", null);
                    }
                    if (isPlayer && info) {
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " info [player]",
                                "This command can be used to view useful information about a player and execute actions upon them.", null);
                    }
                    if (!isPlayer || Permissions.has(player, Permission.USE_BYPASS)) {
                        permission = true;
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " bypass <player> <check> [seconds]",
                                "This command can be used to cause a player to temporarily bypass a check and its detections.", null);
                    }
                    if (!isPlayer || Permissions.has(player, Permission.WARN)) {
                        permission = true;
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " warn <player> <reason>",
                                "This command can be used to individually warn a player about something important.", null);
                    }
                    if (!isPlayer || Permissions.has(player, Permission.KICK)) {
                        permission = true;
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " kick <player> <reason>",
                                "This command can be used to kick players from the server for a specific reason.", null);
                    }
                    if (ban || unban) {
                        permission = true;

                        if (ban) {
                            ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " ban <player> [(time)m/h/d/w/y] <reason>",
                                    "This command can be used to temporarily prevent a player from joining the server.", null);
                        }
                        if (unban) {
                            ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " unban <player>",
                                    "This command can be used to allow a banned player to gain the ability to join the server again.", null);
                        }
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " ban-info [player]",
                                "This command can be used to view information about permanently/temporarily banned players.", null);
                    }
                    if (!isPlayer || Permissions.has(player, Permission.WAVE)) {
                        permission = true;
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " wave <add/remove/clear/run/list> [player] [command]",
                                "This command can be used to add a player to a list with a command representing their punishment. " +
                                        "This list can be executed manually by a player or automatically based on the plugin's configuration, " +
                                        "and cause added players to punished all at once and in order."
                                        + "\n\n"
                                        + "Example: /" + command + " wave add playerName ban {player} You have been banned for hacking!", null);
                    }
                    if (isPlayer && Permissions.has(player, Permission.REPORT)) {
                        permission = true;
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " report <player> [reason]",
                                "This command can be used by players to report other players for specific reasons.", null);
                    }
                    if (!isPlayer || Permissions.has(player, Permission.ADMIN)) {
                        permission = true;
                        ClickableMessage.sendCommand(sender, ChatColor.RED + "/" + command + " proxy-command <command>",
                                "This command can be used to transfer commands to the proxy/network of servers. (Example: BungeeCord)", null);
                    }

                    if (!permission) {
                        completeMessage(sender, "default");
                    }
                    break;
                case "conditions":
                    if (!isPlayer || Permissions.has(player, Permission.CONDITION)) {
                        sender.sendMessage(ChatColor.RED + "/" + command + " <player> if <condition> equals <result> do <command>");
                        sender.sendMessage(ChatColor.RED + "/" + command + " <player> if <condition> contains <result> do <command>");
                        sender.sendMessage(ChatColor.RED + "/" + command + " <player> if <number> is-less-than <result> do <command>");
                        sender.sendMessage(ChatColor.RED + "/" + command + " <player> if <number> is-greater-than <result> do <command>");
                    } else {
                        completeMessage(sender, "default");
                    }
                    break;
                case "commands":
                    sender.sendMessage(ChatColor.RED + "Run '/spartan commands' for a list of commands");
                default:
                    break;
            }
        }
    }

    public static int num(final String s) {
        return Integer.parseInt(s);
    }

    public static double dbl(final String s) {
        return Double.parseDouble(s);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;

        if (label.equalsIgnoreCase(Register.plugin.getName()) && (isPlayer || sender instanceof ConsoleCommandSender)) {
            SpartanPlayer player = isPlayer ? SpartanBukkit.getPlayer((Player) sender) : null;

            if (isPlayer && player == null) {
                return false;
            }
            if (args.length == 1) {
                if (isPlayer && args[0].equalsIgnoreCase("Menu")) {
                    SpartanMenu.mainMenu.open(player);

                } else if (args[0].equalsIgnoreCase("Moderation")) {
                    completeMessage(sender, args[0].toLowerCase());

                } else if (args[0].equalsIgnoreCase("Conditions")) {
                    completeMessage(sender, args[0].toLowerCase());

                } else if (args[0].equalsIgnoreCase("Ban-info")) {
                    if (isPlayer && (!Permissions.has((Player) sender, Permission.BAN) && !Permissions.has((Player) sender, Permission.UNBAN))) {
                        sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    }
                    sender.sendMessage(ChatColor.GRAY + "Banned Players" + ChatColor.DARK_GRAY + ":");
                    sender.sendMessage(BanManagement.getBanListString());

                } else if (args[0].equalsIgnoreCase("Reload") || args[0].equalsIgnoreCase("Rl")) {
                    if (isPlayer && !Permissions.has((Player) sender, Permission.RELOAD)) {
                        sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    }
                    Config.reload(sender);

                } else if (isPlayer && args[0].equalsIgnoreCase("Info")) {
                    if (!Permissions.has((Player) sender, Permission.INFO)) {
                        sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    }
                    SpartanMenu.playerInfo.open(player, sender.getName());

                } else if (isPlayer && args[0].equalsIgnoreCase("Notifications")) {
                    if (!DetectionNotifications.hasPermission(player)) {
                        sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    }
                    DetectionNotifications.toggle(player, 0);

                } else if (Compatibility.CompatibilityType.MinigameMaker.isFunctional() && args[0].equalsIgnoreCase("Add-Incompatible-Item")) {
                    if (isPlayer && !Permissions.has((Player) sender, Permission.MANAGE)) {
                        sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    }
                    int counter = 0;
                    StringBuilder events = new StringBuilder();

                    for (Events.EventType eventType : Events.EventType.values()) {
                        counter++;
                        String event = eventType.toString().toLowerCase().replace("_", "-");

                        if (counter % 2 == 0) {
                            events.append(ChatColor.YELLOW).append(event).append(ChatColor.DARK_GRAY).append(", ");
                        } else {
                            events.append(ChatColor.GRAY).append(event).append(ChatColor.DARK_GRAY).append(", ");
                        }
                    }
                    events = new StringBuilder(events.substring(0, events.length() - 2));
                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.GOLD + "Events: " + events);
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Hint: use %spc% for space characters.");
                    sender.sendMessage(ChatColor.RED + "Usage: /spartan add-incompatible-item <event> <material> [name] <check(s)> <bypass-seconds>");
                    sender.sendMessage(ChatColor.RED + "Usage: /spartan add-incompatible-item <event> <material> [name] <check(s)> <bypass-seconds>");
                    sender.sendMessage(ChatColor.GREEN + "Example #1: /spartan add-incompatible-item block-break diamond-pickaxe FastPlace|FastBreak|BlockReach 3");
                    sender.sendMessage(ChatColor.GREEN + "Example #2: /spartan add-incompatible-item player-hand-damage-player iron-sword arthur's%spc%excalibur KillAura 1");
                    sender.sendMessage("");

                    if (isPlayer) {
                        SpartanMenu.supportIncompatibleItems.open(player);
                    }

                } else {
                    completeMessage(sender, "default");
                }
            } else if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("Proxy-Command")) {
                    if (isPlayer && !Permissions.has((Player) sender, Permission.ADMIN)) {
                        sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    }
                    StringBuilder argumentsToStringBuilder = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        argumentsToStringBuilder.append(args[i]).append(" ");
                    }
                    String argumentsToString = argumentsToStringBuilder.substring(0, argumentsToStringBuilder.length() - 1);

                    if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                        sender.sendMessage(Config.messages.getColorfulString("massive_command_reason"));
                        return true;
                    }
                    if (!NetworkUtils.executeCommand(isPlayer ? player.getPlayer() : null, argumentsToString)) {
                        sender.sendMessage(Config.messages.getColorfulString("failed_command"));
                        return true;
                    }
                    sender.sendMessage(Config.messages.getColorfulString("successful_command"));
                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("Wave")) {
                        String command = args[1];

                        if (isPlayer && !Permissions.has((Player) sender, Permission.WAVE)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        if (command.equalsIgnoreCase("Run")) {
                            if (Wave.getWaveList().length == 0) {
                                sender.sendMessage(Config.messages.getColorfulString("empty_wave_list"));
                                return true;
                            }
                            if (!Wave.start()) {
                                sender.sendMessage(Config.messages.getColorfulString("failed_command"));
                            }
                        } else if (command.equalsIgnoreCase("Clear")) {
                            Wave.clear();
                            sender.sendMessage(Config.messages.getColorfulString("wave_clear_message"));
                        } else if (command.equalsIgnoreCase("List")) {
                            sender.sendMessage(ChatColor.GRAY + "Wave Queued Players" + ChatColor.DARK_GRAY + ":");
                            sender.sendMessage(Wave.getWaveListString());
                        } else {
                            completeMessage(sender, "moderation");
                        }

                    } else if (isPlayer && args[0].equalsIgnoreCase("Info")) {
                        if (!Permissions.has((Player) sender, Permission.INFO)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        SpartanMenu.playerInfo.open(player, ConfigUtils.replaceWithSyntax(args[1], null));

                    } else if (args[0].equalsIgnoreCase("Toggle")) {
                        String check = args[1];

                        if (isPlayer && !Permissions.has((Player) sender, Permission.MANAGE)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        boolean exists = false;

                        for (Enums.HackType hackType : Enums.HackType.values()) {
                            if (hackType.getCheck().getName().equalsIgnoreCase(check)) {
                                check = hackType.toString();
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            Enums.HackType type = Enums.HackType.valueOf(check);
                            Check checkObj = type.getCheck();

                            if (checkObj.isEnabled(null, null, null)) {
                                checkObj.setEnabled(null, false);
                                String message = Config.messages.getColorfulString("check_disable_message");
                                message = ConfigUtils.replaceWithSyntax(isPlayer ? (Player) sender : null, message, type);
                                sender.sendMessage(message);
                            } else {
                                checkObj.setEnabled(null, true);
                                String message = Config.messages.getColorfulString("check_enable_message");
                                message = ConfigUtils.replaceWithSyntax(isPlayer ? (Player) sender : null, message, type);
                                sender.sendMessage(message);
                            }
                        } else {
                            sender.sendMessage(Config.messages.getColorfulString("non_existing_check"));
                        }

                    } else if (isPlayer && args[0].equalsIgnoreCase("Report")) {
                        SpartanPlayer t = SpartanBukkit.getPlayer(args[1]);

                        if (!Permissions.has((Player) sender, Permission.REPORT)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        if (t == null) {
                            sender.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                            return true;
                        }
                        SpartanMenu.playerReports.open(player, t);

                    } else if (args[0].equalsIgnoreCase("Unban")) {
                        OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);

                        if (isPlayer && !Permissions.has((Player) sender, Permission.UNBAN)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        UUID uuid = t.getUniqueId();

                        if (!BanManagement.isBanned(uuid)) {
                            sender.sendMessage(Config.messages.getColorfulString("player_not_banned"));
                            return true;
                        }
                        BanManagement.unban(uuid, true);

                        String message = Config.messages.getColorfulString("unban_message");
                        message = ConfigUtils.replaceWithSyntax(t, message, null);
                        sender.sendMessage(message);

                    } else if (args[0].equalsIgnoreCase("Ban-info")) {
                        OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);

                        if (isPlayer && (!Permissions.has((Player) sender, Permission.BAN) && !Permissions.has((Player) sender, Permission.UNBAN))) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        UUID tuuid = t.getUniqueId();

                        if (!BanManagement.isBanned(tuuid)) {
                            sender.sendMessage(Config.messages.getColorfulString("player_not_banned"));
                            return true;
                        }
                        sender.sendMessage(ChatColor.GRAY + "Ban Information" + ChatColor.DARK_GRAY + ":");
                        sender.sendMessage(ChatColor.GRAY + "Player" + ChatColor.DARK_GRAY + ": " + ChatColor.RED + t.getName());
                        sender.sendMessage(ChatColor.GRAY + "Punisher" + ChatColor.DARK_GRAY + ": " + ChatColor.RED + BanManagement.getProtected(tuuid, "punisher"));
                        sender.sendMessage(ChatColor.GRAY + "Reason" + ChatColor.DARK_GRAY + ": " + ChatColor.RED + BanManagement.getProtected(tuuid, "reason"));
                        long creation = BanManagement.getCreation(tuuid);

                        if (creation != 0L) {
                            sender.sendMessage(ChatColor.GRAY + "Creation" + ChatColor.DARK_GRAY + ": " + ChatColor.RED + new Timestamp(creation).toString().substring(0, 10));
                        }
                        long expiration = BanManagement.getExpiration(tuuid);

                        if (expiration != 0L) {
                            sender.sendMessage(ChatColor.GRAY + "Expiration" + ChatColor.DARK_GRAY + ": " + ChatColor.RED + new Timestamp(expiration).toString().substring(0, 10));
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "Expiration" + ChatColor.DARK_GRAY + ": " + ChatColor.RED + "Never");
                        }

                    } else if (args[0].equalsIgnoreCase("Customer-Support")) {
                        if (isPlayer && !Permissions.has((Player) sender, Permission.ADMIN)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        AwarenessNotifications.forcefullySend(sender, "Please wait...");

                        SpartanBukkit.connectionThread.execute(() -> {
                            Enums.HackType[] hackTypes = Enums.HackType.values();
                            Set<Enums.HackType> set = new HashSet<>(hackTypes.length);

                            for (Enums.HackType hackType : hackTypes) {
                                if (hackType.getCheck().getProblematicDetections() > 0) {
                                    set.add(hackType);
                                }
                            }
                            int size = set.size();

                            if (size > 0) {
                                String discordTag = args[1];

                                for (Enums.HackType hackType : set) {
                                    AwarenessNotifications.forcefullySend(sender,
                                            hackType.getCheck().getName() + ": " + CloudConnections.sendCustomerSupport(discordTag, hackType.toString(), "Identified Problematic Detection", false)
                                    );
                                }
                            } else {
                                AwarenessNotifications.forcefullySend(sender, "No problematic checks found, please specify a check name instead. (Example: /spartan customer-support " + Enums.HackType.values()[0] + " DiscordName#0001 Explanation)");
                            }
                        });

                    } else if (isPlayer && args[0].equalsIgnoreCase("Notifications")) {
                        if (!DetectionNotifications.hasPermission(player)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        String divisorString = args[1];

                        if (AlgebraUtils.validInteger(divisorString)) {
                            int divisor = Math.min(Integer.parseInt(divisorString), Check.maxViolationsPerCycle);

                            if (divisor > 0) {
                                Integer cachedDivisor = DetectionNotifications.getDivisor(player, true);

                                if (cachedDivisor != null && cachedDivisor != divisor) {
                                    DetectionNotifications.change(player, divisor, true);
                                } else {
                                    DetectionNotifications.toggle(player, divisor);
                                }
                            } else {
                                completeMessage(sender, "moderation");
                            }
                        } else {
                            completeMessage(sender, "moderation");
                        }

                    } else {
                        completeMessage(sender, "default");
                    }
                } else { // 3 or more arguments
                    StringBuilder argumentsToStringBuilder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        argumentsToStringBuilder.append(args[i]).append(" ");
                    }
                    String argumentsToString = argumentsToStringBuilder.substring(0, argumentsToStringBuilder.length() - 1);

                    if (args[0].equalsIgnoreCase("Kick")) {
                        SpartanPlayer t = SpartanBukkit.getPlayer(args[1]);

                        if (isPlayer && !Permissions.has((Player) sender, Permission.KICK)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                            sender.sendMessage(Config.messages.getColorfulString("massive_command_reason"));
                            return true;
                        }
                        if (t == null) {
                            sender.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                            return true;
                        }
                        Moderation.kick(sender, t, argumentsToString);

                    } else if (args[0].equalsIgnoreCase("Warn")) {
                        SpartanPlayer t = SpartanBukkit.getPlayer(args[1]);

                        if (isPlayer && !Permissions.has((Player) sender, Permission.WARN)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                            sender.sendMessage(Config.messages.getColorfulString("massive_command_reason"));
                            return true;
                        }
                        if (t == null) {
                            sender.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                            return true;
                        }
                        Moderation.warn(sender, t, argumentsToString);

                    } else if (args[0].equalsIgnoreCase("Ban")) {
                        if (isPlayer && !Permissions.has((Player) sender, Permission.BAN)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        String targetName = args[1];
                        OfflinePlayer t = Bukkit.getOfflinePlayer(targetName);
                        boolean isTempBan = false;
                        String time = args[2];

                        if (args.length > 3) {
                            for (String character : timeCharacters) {
                                if (time.endsWith(character) && AlgebraUtils.validInteger(time.replace(character, ""))) {
                                    isTempBan = true;
                                    break;
                                }
                            }
                        }

                        if (isTempBan) {
                            String command = "spartan tempban " + targetName + " " + time + " " + argumentsToString.substring(time.length() + 1); // Add 1 for the expected space character

                            if (isPlayer) {
                                ((Player) sender).performCommand(command);
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            }
                        } else {
                            BanManagement.ban(t.getUniqueId(), sender, argumentsToString, 0L);

                            String message = Config.messages.getColorfulString("ban_message");
                            message = message.replace("{reason}", argumentsToString);
                            message = message.replace("{punisher}", sender.getName());
                            message = message.replace("{expiration}", "Permanently");
                            message = ConfigUtils.replaceWithSyntax(t, message, null);
                            sender.sendMessage(message);
                        }

                    } else if (isPlayer && args[0].equalsIgnoreCase("Report")) {
                        SpartanPlayer t = SpartanBukkit.getPlayer(args[1]);

                        if (!Permissions.has((Player) sender, Permission.REPORT)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                            sender.sendMessage(Config.messages.getColorfulString("massive_command_reason"));
                            return true;
                        }
                        if (t == null) {
                            sender.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                            return true;
                        }
                        Moderation.report(player, t, argumentsToString);

                    } else if (args[0].equalsIgnoreCase("Bypass")) {
                        boolean noSeconds = args.length == 3;

                        if (noSeconds || args.length == 4) {
                            Enums.HackType[] hackTypes = Enums.HackType.values();
                            int maxHackTypes = hackTypes.length;
                            SpartanPlayer t = SpartanBukkit.getPlayer(args[1]);
                            String[] checks = args[2].split(",", maxHackTypes);
                            String sec = noSeconds ? null : args[3];

                            if (isPlayer && !Permissions.has((Player) sender, Permission.USE_BYPASS)) {
                                sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                                return true;
                            }
                            if (t == null) {
                                sender.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                                return true;
                            }
                            List<Enums.HackType> found = new ArrayList<>(maxHackTypes);

                            for (String check : checks) {
                                for (Enums.HackType hackType : hackTypes) {
                                    if (hackType.getCheck().getName().equalsIgnoreCase(check)) {
                                        found.add(hackType);
                                        break;
                                    }
                                }
                            }
                            if (found.size() > 0) {
                                for (Enums.HackType hackType : found) {
                                    int seconds = noSeconds ? 0 : Integer.parseInt(sec);

                                    if (noSeconds) {
                                        hackType.getCheck().addDisabledUser(t.getUniqueId(), "Command-" + sender.getName(), 0);
                                    } else {
                                        if (seconds < 1 || seconds > 3600) {
                                            sender.sendMessage(ChatColor.RED + "Seconds must be between 1 and 3600.");
                                            return true;
                                        }
                                        hackType.getCheck().addDisabledUser(t.getUniqueId(), "Command-" + sender.getName(), seconds * 20);
                                    }
                                    String message = ConfigUtils.replaceWithSyntax(t, Config.messages.getColorfulString("bypass_message"), hackType)
                                            .replace("{time}", noSeconds ? "infinite" : String.valueOf(seconds));
                                    sender.sendMessage(message);
                                }
                            } else {
                                sender.sendMessage(Config.messages.getColorfulString("non_existing_check"));
                            }
                        } else {
                            completeMessage(sender, "moderation");
                        }
                    } else if (args[0].equalsIgnoreCase("Wave")) {
                        String command = args[1];
                        OfflinePlayer t = Bukkit.getOfflinePlayer(args[2]);

                        if (isPlayer && !Permissions.has((Player) sender, Permission.WAVE)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        if (command.equalsIgnoreCase("add") && args.length >= 4) {
                            if (Wave.getWaveList().length >= 100) {
                                sender.sendMessage(Config.messages.getColorfulString("full_wave_list"));
                                return true;
                            }
                            argumentsToStringBuilder = new StringBuilder();
                            for (int i = 3; i < args.length; i++) {
                                argumentsToStringBuilder.append(args[i]).append(" ");
                            }
                            argumentsToString = argumentsToStringBuilder.substring(0, argumentsToStringBuilder.length() - 1);

                            if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                                sender.sendMessage(Config.messages.getColorfulString("massive_command_reason"));
                                return true;
                            }
                            String message = Config.messages.getColorfulString("wave_add_message");
                            message = ConfigUtils.replaceWithSyntax(t, message, null);
                            sender.sendMessage(message);
                            Wave.add(t.getUniqueId(), argumentsToString); // After to allow for further messages to take palce
                        } else if (command.equalsIgnoreCase("remove")) {
                            UUID uuid = t.getUniqueId();

                            if (Wave.getCommand(uuid) == null) {
                                String message = Config.messages.getColorfulString("wave_not_added_message");
                                message = ConfigUtils.replaceWithSyntax(t, message, null);
                                sender.sendMessage(message);
                                return true;
                            }
                            Wave.remove(uuid);
                            String message = Config.messages.getColorfulString("wave_remove_message");
                            message = ConfigUtils.replaceWithSyntax(t, message, null);
                            sender.sendMessage(message);
                        } else {
                            completeMessage(sender, "moderation");
                        }

                    } else if (args.length == 3 && args[0].equalsIgnoreCase("Customer-Support")) {
                        if (isPlayer && !Permissions.has((Player) sender, Permission.ADMIN)) {
                            sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                            return true;
                        }
                        AwarenessNotifications.forcefullySend(sender, "Please wait...");

                        SpartanBukkit.connectionThread.execute(() -> {
                            String check = args[1];
                            String discordTag = args[2];
                            AwarenessNotifications.forcefullySend(sender, CloudConnections.sendCustomerSupport(discordTag, check, "No Provided Description", false));
                        });
                    } else if (args.length >= 4) {
                        argumentsToStringBuilder = new StringBuilder();
                        for (int i = 3; i < args.length; i++) {
                            argumentsToStringBuilder.append(args[i]).append(" ");
                        }
                        argumentsToString = argumentsToStringBuilder.substring(0, argumentsToStringBuilder.length() - 1);

                        if (args[0].equalsIgnoreCase("Customer-Support")) {
                            if (isPlayer && !Permissions.has((Player) sender, Permission.ADMIN)) {
                                sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                                return true;
                            }
                            if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                                sender.sendMessage(Config.messages.getColorfulString("failed_command")); // Different reply, do not change
                                return true;
                            }
                            String argumentsToStringThreaded = argumentsToString;
                            AwarenessNotifications.forcefullySend(sender, "Please wait...");

                            SpartanBukkit.connectionThread.execute(() -> {
                                String check = args[1];
                                String discordTag = args[2];
                                AwarenessNotifications.forcefullySend(sender, CloudConnections.sendCustomerSupport(discordTag, check, argumentsToStringThreaded, false));
                            });

                        } else if (args[0].equalsIgnoreCase("Tempban")) {
                            if (isPlayer && !Permissions.has((Player) sender, Permission.BAN)) {
                                sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                                return true;
                            }
                            if (isPlayer ? argumentsToString.length() > player.getMaxChatLength() : argumentsToString.length() > maxConnectedArgumentLength) {
                                sender.sendMessage(Config.messages.getColorfulString("massive_command_reason"));
                                return true;
                            }
                            OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                            String time = args[2];
                            char type = 'a';

                            for (String character : timeCharacters) {
                                if (time.endsWith(character)) {
                                    type = character.charAt(0);
                                    break;
                                }
                            }

                            if (type == 'a') {
                                sender.sendMessage(Config.messages.getColorfulString("failed_command"));
                                return true;
                            }
                            String multiplier = time.substring(0, time.length() - 1);

                            if (!AlgebraUtils.validInteger(multiplier)) {
                                sender.sendMessage(Config.messages.getColorfulString("failed_command"));
                                return true;
                            }
                            long ms = TimeUtils.getTime(Integer.parseInt(multiplier), type);

                            if (ms == 0L) {
                                sender.sendMessage(Config.messages.getColorfulString("failed_command"));
                                return true;
                            }
                            ms += System.currentTimeMillis();
                            BanManagement.ban(t.getUniqueId(), sender, argumentsToString, ms);

                            String message = Config.messages.getColorfulString("ban_message");
                            message = message.replace("{reason}", argumentsToString);
                            message = message.replace("{punisher}", sender.getName());
                            message = message.replace("{expiration}", new Timestamp(ms).toString().substring(0, 10));
                            message = ConfigUtils.replaceWithSyntax(t, message, null);
                            sender.sendMessage(message);

                        } else if (Compatibility.CompatibilityType.MinigameMaker.isFunctional() && args[0].equalsIgnoreCase("Add-Incompatible-Item")) {
                            if (args.length == 6) {
                                if (isPlayer && !Permissions.has((Player) sender, Permission.MANAGE)) {
                                    sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                                    return true;
                                }
                                String seconds = args[5];

                                if (AlgebraUtils.validInteger(seconds) && MinigameMaker.addItem(args[1], args[2], args[4], args[3], Math.max(Integer.parseInt(seconds), 1))) {
                                    if (isPlayer) {
                                        SpartanMenu.supportIncompatibleItems.open(player);
                                    } else {
                                        sender.sendMessage(ChatColor.GREEN + "Incompatible item successfully added.");
                                    }
                                } else {
                                    Bukkit.dispatchCommand(sender, "spartan add-incompatible-item");
                                }
                            } else if (args.length == 5) {
                                if (isPlayer && !Permissions.has((Player) sender, Permission.MANAGE)) {
                                    sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                                    return true;
                                }
                                String seconds = args[4];

                                if (AlgebraUtils.validInteger(seconds) && MinigameMaker.addItem(args[1], args[2], args[3], "%spc%", Math.max(Integer.parseInt(seconds), 1))) {
                                    if (isPlayer) {
                                        SpartanMenu.supportIncompatibleItems.open(player);
                                    } else {
                                        sender.sendMessage(ChatColor.GREEN + "Incompatible item successfully added.");
                                    }
                                } else {
                                    Bukkit.dispatchCommand(sender, "spartan add-incompatible-item");
                                }
                            } else {
                                Bukkit.dispatchCommand(sender, "spartan add-incompatible-item");
                            }
                        } else if (args.length >= 7) {
                            if (isPlayer && !Permissions.has((Player) sender, Permission.CONDITION)) {
                                sender.sendMessage(Config.messages.getColorfulString("no_permission"));
                                return true;
                            }
                            SpartanPlayer t = SpartanBukkit.getPlayer(args[0]);

                            if (t == null) {
                                sender.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("if") && args[5].equalsIgnoreCase("do")) {
                                final String condition = ConfigUtils.replaceWithSyntax(t, args[2], null);
                                final String result = ConfigUtils.replaceWithSyntax(t, args[4], null);

                                argumentsToStringBuilder = new StringBuilder();
                                for (int i = 6; i < args.length; i++) {
                                    argumentsToStringBuilder.append(args[i]).append(" ");
                                }
                                final String command = ConfigUtils.replaceWithSyntax(t, argumentsToStringBuilder.substring(0, argumentsToStringBuilder.length() - 1), null);

                                switch (args[3].toLowerCase()) {
                                    case "equals":
                                    case "=":
                                        if (condition.equalsIgnoreCase(result)) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                        }
                                        break;
                                    case "not-equals":
                                    case "/=":
                                        if (!condition.equalsIgnoreCase(result)) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                        }
                                        break;
                                    case "contains":
                                        if (condition.contains(result)) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                        }
                                        break;
                                    case "is-less-than":
                                    case "<":
                                        if (AlgebraUtils.validInteger(condition) && AlgebraUtils.validInteger(result) && num(condition) < num(result)
                                                || AlgebraUtils.validDecimal(condition) && AlgebraUtils.validDecimal(result) && dbl(condition) < dbl(result)
                                                || AlgebraUtils.validInteger(condition) && AlgebraUtils.validDecimal(result) && num(condition) < dbl(result)
                                                || AlgebraUtils.validDecimal(condition) && AlgebraUtils.validInteger(result) && dbl(condition) < num(result)) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                        }
                                        break;
                                    case "is-greater-than":
                                    case ">":
                                        if (AlgebraUtils.validInteger(condition) && AlgebraUtils.validInteger(result) && num(condition) > num(result)
                                                || AlgebraUtils.validDecimal(condition) && AlgebraUtils.validDecimal(result) && dbl(condition) > dbl(result)
                                                || AlgebraUtils.validInteger(condition) && AlgebraUtils.validDecimal(result) && num(condition) > dbl(result)
                                                || AlgebraUtils.validDecimal(condition) && AlgebraUtils.validInteger(result) && dbl(condition) > num(result)) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    } else {
                        completeMessage(sender, "default");
                    }
                }
            } else if (isPlayer && SpartanMenu.mainMenu.open(player, false)) {
                completeMessage(sender, "commands");
            } else {
                completeMessage(sender, "default");
            }
        }
        return false;
    }
}
