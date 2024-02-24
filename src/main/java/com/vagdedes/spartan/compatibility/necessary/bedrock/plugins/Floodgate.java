package com.vagdedes.spartan.compatibility.necessary.bedrock.plugins;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.server.PluginUtils;
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
                    if (AwarenessNotifications.canSend(p.getUniqueId(), "floodgate")) {
                        p.sendMessage(message);
                    }
                }
            }
        }
    }

    public static boolean isUsingBedrockDetections(boolean checkPlayers) {
        if (Compatibility.CompatibilityType.Floodgate.isFunctional()) {
            if (checkPlayers) {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (players.size() > 0) {
                    for (SpartanPlayer p : players) {
                        if (p.isBedrockPlayer()) {
                            return true;
                        }
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean isBedrockPlayer(UUID uuid, String name) {
        return Compatibility.CompatibilityType.Floodgate.isFunctional()
                && FloodgateApi.getInstance().isFloodgatePlayer(uuid)

                || name != null && isBedrockPlayer(name);
    }

    public static boolean isBedrockPlayer(String name) {
        String prefix = Config.settings.getString("Important.bedrock_player_prefix");
        return !prefix.isEmpty() && name.startsWith(prefix);
    }
}
