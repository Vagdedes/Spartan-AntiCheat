package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class Floodgate {

    private static boolean classExists = false;

    public static void reload() {
        classExists = ReflectionUtils.classExists(
                "org.geysermc.floodgate.api.FloodgateApi"
        );
    }

    static boolean isBedrockPlayer(UUID uuid, String name) {
        return Compatibility.CompatibilityType.FLOODGATE.isFunctional()
                && classExists
                && FloodgateApi.getInstance().isFloodgatePlayer(uuid)

                || name != null && isBedrockPlayer(name);
    }

    private static boolean isBedrockPlayer(String name) {
        String prefix = Config.settings.getString("Important.bedrock_player_prefix");
        return !prefix.isEmpty() && name.startsWith(prefix);
    }
}
