package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.moderation.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerJoinEvent e) {
        Player n = e.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(n);

        if (Config.settings.getBoolean("Important.enable_watermark")
                && !Permissions.isStaff(n)) {
            n.sendMessage("");
            AwarenessNotifications.forcefullySend(
                    protocol,
                    "\nThis server is protected by the " + Register.pluginName + " AntiCheat",
                    false
            );
            n.sendMessage("");
        }

        PluginBase.runDelayedTask(protocol, () -> {
            Config.settings.runOnLogin(protocol);
            CloudBase.announce(protocol);
        }, 10L);
    }

}
