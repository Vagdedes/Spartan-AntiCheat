package com.vagdedes.spartan.compatibility.necessary.bedrock;

import com.vagdedes.spartan.compatibility.necessary.bedrock.plugins.Floodgate;
import com.vagdedes.spartan.compatibility.necessary.bedrock.plugins.ProtocolSupport;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BedrockCompatibility {

    public static boolean isPlayer(Player p) {
        return ProtocolSupport.isBedrockPlayer(p) || Floodgate.isBedrockPlayer(p.getUniqueId(), p.getName());
    }

    public static boolean isPlayer(UUID uuid, String name) {
        return Floodgate.isBedrockPlayer(uuid, name);
    }

    public static boolean isPlayer(String name) {
        return Floodgate.isBedrockPlayer(name);
    }
}
