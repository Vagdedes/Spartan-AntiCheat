package com.vagdedes.spartan.configuration;

import com.vagdedes.spartan.abstraction.ConfigurationBuilder;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.functionality.synchronicity.DiscordWebhooks;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Settings extends ConfigurationBuilder {

    public Settings() {
        super("settings");
    }

    public static final String explosionOption = "Protections.explosion_detection_cooldown",
            permissionOption = "Important.use_permission_cache", // test server
            showEcosystemOption = "Important.show_ecosystem",
            tpsProtectionOption = "Protections.use_tps_protection",
            maxSupportedLatencyOption = "Protections.max_supported_player_latency",
            cloudServerNameOption = "Cloud.server_name",
            cloudSynchroniseFilesOption = "Cloud.synchronise_files";
    private static final List<String> defaultPunishments = new ArrayList<>(Check.maxCommands);
    private Collection<String> punishments = null;

    static {
        defaultPunishments.add("spartan kick {player} {detections}");

        for (int position = (defaultPunishments.size() + 1); position <= Check.maxCommands; position++) {
            defaultPunishments.add("");
        }
    }

    @Override
    public void clear() {
        punishments = null;
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
        addOption(maxSupportedLatencyOption, 500); // test server
        addOption("Protections.avoid_self_bow_damage", true);
        addOption("Protections.player_limit_per_ip", 0);
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

        addOption(cloudServerNameOption, "specify server name");
        addOption(cloudSynchroniseFilesOption, true);

        addOption("Detections.ground_teleport_on_detection", true); // test server
        addOption("Detections.fall_damage_on_teleport", false); // test server
        addOption("Detections.update_blocks_upon_violation", false);

        addOption("Performance.enable_false_positive_detection", true); // test server
        addOption("Performance.use_vanilla_ground_method", false); // test server
        addOption(MaximumCheckedPlayers.option, 100); // test server alternative

        addOption(DiscordWebhooks.configurationSection + ".webhook_hex_color", "4caf50");
        addOption(DiscordWebhooks.configurationSection + ".checks_webhook_url", "");
        addOption(DiscordWebhooks.configurationSection + ".punishments_webhook_url", "");
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

    public List<String> getDefaultPunishmentCommands() {
        return defaultPunishments;
    }

    public Collection<String> getPunishmentCommands() {
        if (punishments != null) {
            return punishments;
        } else {
            String sectionString = "Punishments.Commands";

            try {
                if (file.exists() || file.createNewFile()) {
                    for (int position = 0; position < defaultPunishments.size(); position++) {
                        ConfigUtils.add(file, sectionString + "." + (position + 1), defaultPunishments.get(position));
                    }
                } else {
                    AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
                }
            } catch (Exception ex) {
                AwarenessNotifications.forcefullySend("Failed to find/create the '" + file.getName() + "' file.");
            }
            ConfigurationSection section = getPath().getConfigurationSection(sectionString);

            if (section != null) {
                Collection<String> list = new ArrayList<>();
                Set<String> keys = section.getKeys(true);

                if (!keys.isEmpty()) {
                    sectionString += ".";

                    for (String key : keys) {
                        if (AlgebraUtils.validInteger(key)) {
                            int number = Integer.parseInt(key);

                            if (number >= 1 && number <= Check.maxCommands) {
                                String command = getString(sectionString + number);

                                if (command != null && !command.isEmpty()) {
                                    list.add(command);
                                }
                            }
                        }
                    }
                }
                return punishments = list;
            } else {
                return punishments = new ArrayList<>(0);
            }
        }
    }
}
