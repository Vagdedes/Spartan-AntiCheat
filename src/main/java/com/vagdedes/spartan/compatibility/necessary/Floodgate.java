package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.UUID;

public class Floodgate {

    public static void reload() {
        String message;

        if (PluginUtils.contains("geyser")
                && !PluginUtils.contains("floodgate")) {
            message = AwarenessNotifications.getOptionalNotification(
                    "'Geyser' plugin allows Bedrock players to join your Java Minecraft server. "
                            + "In order for the Bedrock compatibility to enable, you must also install a plugin named 'Floodgate', which will allow Spartan to identify who is a bedrock player.");
        } else {
            message = null;
        }

        if (message != null) {
            List<SpartanPlayer> players = Permissions.getStaff();

            if (!players.isEmpty()) {
                for (SpartanPlayer p : players) {
                    if (AwarenessNotifications.canSend(p.uuid, "floodgate")) {
                        p.sendMessage(message);
                    }
                }
            }
        }
    }

    static boolean isBedrockPlayer(UUID uuid, String name) {
        return Compatibility.CompatibilityType.FLOODGATE.isFunctional()
                && FloodgateApi.getInstance().isFloodgatePlayer(uuid)

                || name != null && isBedrockPlayer(name);
    }

    static boolean isBedrockPlayer(String name) {
        String prefix = Config.settings.getString("Important.bedrock_player_prefix");
        return !prefix.isEmpty() && name.startsWith(prefix);
    }
}
