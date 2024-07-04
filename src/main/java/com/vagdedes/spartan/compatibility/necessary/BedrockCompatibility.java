package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BedrockCompatibility {

    public static boolean isPlayer(Player p) {
        return ProtocolSupport.isBedrockPlayer(p)
                || Floodgate.isBedrockPlayer(p.getUniqueId(), p.getName())

                || Config.settings.getBoolean("Important.bedrock_client_permission")
                && Permissions.onlyHas(p, Enums.Permission.BEDROCK);
    }

    public static boolean isPlayer(UUID uuid, String name) {
        return Floodgate.isBedrockPlayer(uuid, name);
    }

    public static boolean isPlayer(String name) {
        return Floodgate.isBedrockPlayer(name);
    }
}
