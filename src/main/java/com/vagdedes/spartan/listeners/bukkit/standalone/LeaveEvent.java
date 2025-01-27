package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.moderation.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LeaveEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerQuitEvent e) {
        PlayerProtocol protocol = PluginBase.deleteProtocol(e.getPlayer());

        if (protocol == null) {
            return;
        }
        DetectionNotifications.runOnLeave(protocol);
    }

}
