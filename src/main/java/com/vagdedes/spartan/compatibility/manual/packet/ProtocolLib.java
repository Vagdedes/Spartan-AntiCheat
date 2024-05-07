package com.vagdedes.spartan.compatibility.manual.packet;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.entity.Player;

public class ProtocolLib {

    private static Class<?> temporaryPlayer = null;

    public static boolean isTemporaryPLayer(Player p) {
        return Compatibility.CompatibilityType.PROTOCOL_LIB.isFunctional()
                && temporaryPlayer != null
                && p.getClass().isInstance(temporaryPlayer);
    }

    public static void reload() {
        temporaryPlayer = ReflectionUtils.getClass("com.comphenix.protocol.injector.server.TemporaryPlayer");
    }
}
