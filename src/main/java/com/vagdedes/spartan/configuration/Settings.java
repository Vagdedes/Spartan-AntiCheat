package com.vagdedes.spartan.configuration;

import com.vagdedes.spartan.abstraction.ConfigurationBuilder;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.commands.RawCommands;
import com.vagdedes.spartan.functionality.moderation.PlayerReports;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.functionality.synchronicity.DiscordWebhooks;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.interfaces.commands.CommandExecution;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
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

        addOption("Punishments.broadcast_on_punishment", true);
        addOption("Punishments.report_reasons", StringUtils.toString(PlayerReports.reasons, PlayerReports.separator));
        RawCommands.create();

        addOption("Logs.log_file", true);
        addOption("Logs.log_console", true);

        if (TestServer.isIdentified()) {
            addOption("Notifications.individual_only_notifications", false);
        }
        addOption("Notifications.enable_notifications_on_login", true);
        addOption("Notifications.awareness_notifications", true);
        addOption("Notifications.message_clickable_command", "/teleport {player}");

        addOption("Chat.message_cooldown", 0);
        addOption("Chat.command_cooldown", 0);
        addOption("Chat.prevent_same_message", false);
        addOption("Chat.blocked_words", "blockedWord1, blockedWord2");
        addOption("Chat.blocked_commands", "blockedCommand1, blockedCommand2");
        addOption("Chat.staff_chat_character", "@");

        addOption("Protections.reconnect_cooldown", 1);
        addOption(tpsProtectionOption, true); // test server
        addOption("Protections.max_supported_player_latency", 500); // test server
        addOption("Protections.use_teleport_protection", false); // test server
        addOption("Protections.avoid_self_bow_damage", true);
        addOption("Protections.player_limit_per_ip", 0);
        addOption("Protections.interactions_per_tick", 10);
        addOption("Protections.disallowed_building", true); // test server

        addOption("Important.op_bypass", false);
        addOption("Important.enable_permissions", true);
        addOption("Important.violations_reset_on_kick", false); // test server
        addOption("Important.modify_server_configuration", false);
        addOption("Important.refresh_inventory_menu", true);
        addOption("Important.enable_developer_api", true);
        addOption(showEcosystemOption, true);
        addOption("Important.bedrock_player_prefix", ".");
        addOption("Important.inventory_menu_empty_heads", true);
        addOption("Important.load_player_head_textures", false);

        addOption("Cloud.server_name", "specify server name");
        addOption("Cloud.synchronise_files", true);

        addOption("Detections.ground_teleport_on_detection", true); // test server
        addOption("Detections.fall_damage_on_teleport", false); // test server
        addOption("Detections.allow_cancelled_hit_checking", false); // test server alternative
        addOption("Detections.update_blocks_upon_violation", false);

        addOption("Performance.enable_false_positive_detection", true); // test server
        addOption("Performance.use_vanilla_ground_method", false); // test server
        addOption(MaximumCheckedPlayers.option, 100); // test server alternative

        addOption(DiscordWebhooks.configurationSection + ".webhook_hex_color", "4caf50");
        addOption(DiscordWebhooks.configurationSection + ".checks_webhook_url", "");
        addOption(DiscordWebhooks.configurationSection + ".punishments_webhook_url", "");
        addOption(DiscordWebhooks.configurationSection + ".reports_webhook_url", "");
        addOption(DiscordWebhooks.configurationSection + ".communication_webhook_url", "");

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
