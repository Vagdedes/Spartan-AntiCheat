package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;

import java.util.List;

public class ProtocolLib {

    public static void run() {
        if (SpartanBukkit.packetsForcedState) {
            BackgroundProtocolLib.run();
        }
    }

    public static void otherwise() {
        if (SpartanBukkit.packetsForcedState) {
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
    }
}
