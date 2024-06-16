package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.Shared;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Event_Shared implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Join(PlayerJoinEvent e) {
        if (!SpartanBukkit.packetsEnabled()) {
            Shared.join(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Velocity(PlayerVelocityEvent e) {
        if (!SpartanBukkit.packetsEnabled()) {
            Shared.velocity(e);
        }
    }

}
