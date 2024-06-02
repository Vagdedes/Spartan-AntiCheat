package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.Shared;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Event_Shared implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Join(PlayerJoinEvent e) {
        if (!SpartanBukkit.packetsEnabled(e.getPlayer())) {
            Shared.join(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        if (!SpartanBukkit.packetsEnabled(e.getPlayer())) {
            Shared.move(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        if (!SpartanBukkit.packetsEnabled(e.getPlayer())) {
            Shared.teleport(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Velocity(PlayerVelocityEvent e) {
        if (!SpartanBukkit.packetsEnabled(e.getPlayer())) {
            Shared.velocity(e);
        }
    }

}
