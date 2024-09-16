package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

public class BedrockCompatibility {

    public static boolean isPlayer(Player p) {
        return ProtocolSupport.isBedrockPlayer(p)
                
                || !ProtocolLib.isTemporary(p)
                && Floodgate.isBedrockPlayer(p.getUniqueId(), p.getName())

                || Config.settings.getBoolean("Important.bedrock_client_permission")
                && Permissions.onlyHas(p, Enums.Permission.BEDROCK);
    }

}
