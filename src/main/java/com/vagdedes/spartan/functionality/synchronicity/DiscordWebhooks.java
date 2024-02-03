package com.vagdedes.spartan.functionality.synchronicity;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public class DiscordWebhooks {

    public static final String configurationSection = "Discord";

    public static boolean isUsing() {
        File file = Config.settings.getFile();

        if (file.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = configuration.getConfigurationSection(configurationSection);

            if (section != null) {
                for (String key : section.getKeys(false)) {
                    if (key.endsWith("_url")) {
                        key = configurationSection + "." + key;

                        if (Config.settings.exists(key) && Config.settings.getString(key).length() > 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    static void send(UUID uuid, String name,
                     int x, int y, int z,
                     String type, String string) {
        String webhook;

        switch (type.split(" ")[0]) {
            case "Ban":
            case "Kick":
            case "Punishment":
                webhook = "punishments";
                break;
            case "Warning":
            case "Report":
                webhook = "reports";
                break;
            default:
                switch (type) {
                    case "Staff Chat":
                        webhook = "communication";
                        break;
                    default:
                        webhook = null;
                        break;
                }
                break;
        }

        if (webhook != null) {
            CloudConnections.executeDiscordWebhook(
                    webhook,
                    uuid, (name == null ? "-" : name),
                    x, y, z,
                    type, string
            );
        }
    }
}
