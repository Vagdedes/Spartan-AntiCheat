package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import org.bukkit.entity.Player;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolSupportAPI;

public class ProtocolSupport {

    static boolean isBedrockPlayer(Player p) {
        if (Compatibility.CompatibilityType.ProtocolSupport.isFunctional()) {
            try {
                Connection c = ProtocolSupportAPI.getConnection(p);
                return c != null && c.getVersion().toString().contains("_PE_");
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
