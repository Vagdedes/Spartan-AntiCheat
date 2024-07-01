package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.listeners.bukkit.Event_Chunks;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class ProtocolLib {

    private static boolean temporaryClass = false;

    private static void checkClass() {
        temporaryClass = ReflectionUtils.classExists(
                "com.comphenix.protocol.injector.temporary.TemporaryPlayer"
        );
    }

    public static void run() {
        checkClass();
        BackgroundProtocolLib.run();
    }

    public static void otherwise() {
        checkClass();
        Event_Chunks.clear();
        String message = AwarenessNotifications.getOptionalNotification(
                "Anti-Cheat checks work significantly better with ProtocolLib installed "
                        + "as it allows the use of packets which helps identify information earlier "
                        + "and more accurately."
        );

        if (message != null) {
            List<SpartanPlayer> players = Permissions.getStaff();

            if (!players.isEmpty()) {
                for (SpartanPlayer p : players) {
                    if (AwarenessNotifications.canSend(p.uuid, "protocol-lib", 0)) {
                        p.sendMessage(message);
                    }
                }
            }
        }
    }

    public static boolean isTemporary(Player player) {
        return temporaryClass && player instanceof TemporaryPlayer;
    }
}
