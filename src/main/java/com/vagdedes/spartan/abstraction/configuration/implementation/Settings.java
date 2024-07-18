package com.vagdedes.spartan.abstraction.configuration.implementation;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
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

    public static final String crossServerNotificationsName = "Important.server_name";
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
    }

    @Override
    public void create() {
        file = new File(directory);
        clear();

        addOption("Punishments.broadcast_on_punishment", true);

        addOption("Logs.log_file", true);
        addOption("Logs.log_console", true);

        addOption("Notifications.individual_only_notifications", false);
        addOption("Notifications.enable_notifications_on_login", true);
        addOption("Notifications.awareness_notifications", true);
        addOption("Notifications.message_clickable_command", "/teleport {player}");

        addOption("Protections.max_supported_player_latency", TPS.tickTimeInteger * 100);
        addOption("Protections.player_limit_per_ip", 0);

        addOption("Important.op_bypass", false);
        addOption("Important.bedrock_client_permission", false);
        addOption("Important.enable_developer_api", true);
        addOption("Important.bedrock_player_prefix", ".");
        addOption("Important.enable_npc", true);
        addOption("Important.enable_watermark", true);
        addOption(crossServerNotificationsName, "");

        addOption("Detections.ground_teleport_on_detection", true);
        addOption("Detections.fall_damage_on_teleport", false);

        addOption("Discord.webhook_hex_color", "4caf50");
        addOption("Discord.checks_webhook_url", "");
        addOption("Discord.punishments_webhook_url", "");
    }

    public void runOnLogin(SpartanPlayer p) {
        if (getBoolean("Notifications.enable_notifications_on_login")
                && DetectionNotifications.hasPermission(p)
                && !DetectionNotifications.isEnabled(p)) {
            DetectionNotifications.set(p, DetectionNotifications.defaultFrequency);
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

    public List<String> getPunishmentCommands() {
        if (punishments != null) {
            return new ArrayList<>(punishments);
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
                return new ArrayList<>(punishments = list);
            } else {
                return new ArrayList<>(punishments = new ArrayList<>(0));
            }
        }
    }
}
