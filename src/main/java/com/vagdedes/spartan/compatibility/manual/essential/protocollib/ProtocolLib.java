package com.vagdedes.spartan.compatibility.manual.essential.protocollib;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.entity.Player;

public class ProtocolLib {

    private static Class<?> temporaryPlayer = null;

    public static boolean isTemporaryPLayer(Player p) {
        return Compatibility.CompatibilityType.ProtocolLib.isFunctional()
                && canHaveTemporaryPlayers()
                && p.getClass().isInstance(temporaryPlayer);
    }

    public static void reload() {
        temporaryPlayer = ReflectionUtils.getClass("com.comphenix.protocol.injector.server.TemporaryPlayer");

        try {
            BackgroundProtocolLib.initiate();
        } catch (Exception ignored) {
        }
    }

    public static boolean canHaveTemporaryPlayers() {
        return temporaryPlayer != null;
    }
}
