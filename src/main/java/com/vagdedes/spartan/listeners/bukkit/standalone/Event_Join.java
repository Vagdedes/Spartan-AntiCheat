package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Event_Join implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerJoinEvent e) {
        SpartanProtocol protocol = SpartanBukkit.createProtocol(e.getPlayer());
        SpartanPlayer p = protocol.spartanPlayer;

        if (Config.settings.getBoolean("Important.enable_watermark")
                && !Permissions.isStaff(p.getInstance())) {
            p.getInstance().sendMessage("");
            AwarenessNotifications.forcefullySend(
                    p,
                    "\nThis server is protected by the Spartan AntiCheat",
                    false
            );
            p.getInstance().sendMessage("");
        }

        SpartanBukkit.runDelayedTask(p, () -> {
            Config.settings.runOnLogin(protocol);
            CloudBase.announce(p);
        }, 10L);
    }

}
