package me.vagdedes.spartan.configuration;

import me.vagdedes.spartan.abstraction.ConfigurationBuilder;
import me.vagdedes.spartan.functionality.chat.ChatProtection;
import me.vagdedes.spartan.functionality.commands.RawCommands;
import me.vagdedes.spartan.functionality.moderation.PlayerReports;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.functionality.synchronicity.DiscordWebhooks;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.interfaces.commands.CommandExecution;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Settings extends ConfigurationBuilder {

    public Settings() {
        super("settings");
    }

    public static final String explosionOption = "Protections.explosion_detection_cooldown",
            permissionOption = "Important.use_permission_cache", // test server
            showEcosystemOption = "Important.show_ecosystem",
            tpsProtectionOption = "Protections.use_tps_protection";
    private static int usingCustomPunishmentCommands = 0;

    @Override
    public void clear() {
        usingCustomPunishmentCommands = 0;
        internalClear();

        // Systems based in this configuration
        ChatProtection.clear();
    }

    @Override
    public void create(boolean local) {
        file = new File(directory);
        boolean exists = file.exists();
        clear();

        ConfigUtils.add(file, "Punishments.broadcast_on_punishment", false);
        ConfigUtils.add(file, "Punishments.report_reasons", StringUtils.toString(PlayerReports.reasons, PlayerReports.separator));
        RawCommands.create();

        ConfigUtils.add(file, "Logs.log_file", true);
        ConfigUtils.add(file, "Logs.log_console", true);

        if (TestServer.isIdentified()) {
            ConfigUtils.add(file, "Notifications.individual_only_notifications", false);
        }
        ConfigUtils.add(file, "Notifications.enable_notifications_on_login", true);
        ConfigUtils.add(file, "Notifications.awareness_notifications", true);
        ConfigUtils.add(file, "Notifications.message_clickable_command", "/teleport {player}");

        ConfigUtils.add(file, "Chat.message_cooldown", 0);
        ConfigUtils.add(file, "Chat.command_cooldown", 0);
        ConfigUtils.add(file, "Chat.prevent_same_message", false);
        ConfigUtils.add(file, "Chat.blocked_words", "blockedWord1, blockedWord2");
        ConfigUtils.add(file, "Chat.blocked_commands", "blockedCommand1, blockedCommand2");
        ConfigUtils.add(file, "Chat.staff_chat_character", "@");

        ConfigUtils.add(file, "Protections.reconnect_cooldown", 1);
        ConfigUtils.add(file, tpsProtectionOption, true); // test server
        ConfigUtils.add(file, "Protections.max_supported_player_latency", 500); // test server
        ConfigUtils.add(file, "Protections.use_teleport_protection", false); // test server
        ConfigUtils.add(file, "Protections.avoid_self_bow_damage", true);
        ConfigUtils.add(file, "Protections.player_limit_per_ip", 0);
        ConfigUtils.add(file, "Protections.interactions_per_tick", 10);
        ConfigUtils.add(file, "Protections.disallowed_building", true); // test server

        ConfigUtils.add(file, "Important.op_bypass", false);
        ConfigUtils.add(file, "Important.enable_permissions", true);
        ConfigUtils.add(file, "Important.violations_reset_on_kick", false); // test server
        ConfigUtils.add(file, "Important.modify_server_configuration", false);
        ConfigUtils.add(file, "Important.refresh_inventory_menu", true);
        ConfigUtils.add(file, "Important.enable_developer_api", true);
        ConfigUtils.add(file, showEcosystemOption, true);
        ConfigUtils.add(file, "Important.bedrock_player_prefix", ".");
        ConfigUtils.add(file, "Important.inventory_menu_empty_heads", true);
        ConfigUtils.add(file, "Important.load_player_head_textures", false);

        ConfigUtils.add(file, "Cloud.server_name", "specify server name");
        ConfigUtils.add(file, "Cloud.synchronise_files", true);

        ConfigUtils.add(file, "Detections.ground_teleport_on_detection", true); // test server
        ConfigUtils.add(file, "Detections.fall_damage_on_teleport", false); // test server
        ConfigUtils.add(file, "Detections.allow_cancelled_hit_checking", false); // test server alternative
        ConfigUtils.add(file, "Detections.update_blocks_upon_violation", false);

        ConfigUtils.add(file, "Performance.enable_false_positive_detection", true); // test server
        ConfigUtils.add(file, "Performance.use_vanilla_ground_method", false); // test server
        ConfigUtils.add(file, MaximumCheckedPlayers.option, 100); // test server alternative

        ConfigUtils.add(file, DiscordWebhooks.configurationSection + ".webhook_hex_color", "4caf50");
        ConfigUtils.add(file, DiscordWebhooks.configurationSection + ".checks_webhook_url", "");
        ConfigUtils.add(file, DiscordWebhooks.configurationSection + ".punishments_webhook_url", "");
        ConfigUtils.add(file, DiscordWebhooks.configurationSection + ".reports_webhook_url", "");
        ConfigUtils.add(file, DiscordWebhooks.configurationSection + ".communication_webhook_url", "");

        if (!local && exists) {
            CrossServerInformation.sendConfiguration(file);
        }
    }

    public void runOnLogin(SpartanPlayer p) {
        if (getBoolean("Notifications.enable_notifications_on_login")
                && DetectionNotifications.hasPermission(p)) {
            DetectionNotifications.set(p, true, 0);
        }
    }

    public void runOnLogin() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                runOnLogin(p);
            }
        }
    }

    public List<String> getPunishments() {
        String sectionString = "Punishments.Commands";
        ConfigurationSection section = getPath().getConfigurationSection(sectionString);

        if (section != null) {
            List<String> list = new LinkedList<>();
            Set<String> keys = section.getKeys(true);

            if (!keys.isEmpty()) {
                sectionString += ".";

                for (String key : keys) {
                    if (AlgebraUtils.validInteger(key)) {
                        int number = Integer.parseInt(key);

                        if (number >= 1 && number <= Check.maxCommands) {
                            String command = getString(sectionString + number);

                            if (command != null && command.length() > 0) {
                                list.add(command);
                            }
                        }
                    }
                }
            }
            return list;
        }
        return new LinkedList<>();
    }

    public boolean isUsingCustomPunishmentCommands() {
        if (usingCustomPunishmentCommands != 0) {
            return usingCustomPunishmentCommands == 1;
        }
        if (Config.isLegacy()) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                Check check = hackType.getCheck();

                for (int i = 1; i < Check.maxViolationsPerCycle; i++) {
                    String[] commands = check.getLegacyCommands(i);

                    if (commands.length > 0) {
                        for (String command : commands) {
                            if (CommandExecution.isCustomCommand(command)) {
                                usingCustomPunishmentCommands = 1;
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                String[] commands = hackType.getCheck().getCommands();

                if (commands.length > 0) {
                    for (String command : commands) {
                        if (CommandExecution.isCustomCommand(command)) {
                            usingCustomPunishmentCommands = 1;
                            return true;
                        }
                    }
                }
            }
        }
        usingCustomPunishmentCommands = -1;
        return false;
    }
}
