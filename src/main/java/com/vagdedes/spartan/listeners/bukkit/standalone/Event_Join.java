package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Event_Join implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerJoinEvent e) {
        Player n = e.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(n);

        if (Config.settings.getBoolean("Important.enable_watermark")
                && !Permissions.isStaff(n)) {
            n.sendMessage("");
            AwarenessNotifications.forcefullySend(
                    protocol,
                    "\nThis server is protected by the Spartan AntiCheat",
                    false
            );
            n.sendMessage("");
        }

        SpartanBukkit.runDelayedTask(protocol.spartan, () -> {
            Config.settings.runOnLogin(protocol);
            CloudBase.announce(protocol);
        }, 10L);
    }

}
