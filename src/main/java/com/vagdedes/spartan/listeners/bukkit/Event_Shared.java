package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.Shared;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Event_Shared implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Velocity(PlayerVelocityEvent e) {
        if (!SpartanBukkit.packetsEnabled()) {
            if (ProtocolLib.isTemporary(e.getPlayer())) {
                return;
            }
            Shared.velocity(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        if (!SpartanBukkit.packetsEnabled()) {
            if (ProtocolLib.isTemporary(e.getPlayer())) {
                return;
            }
            Shared.movement(e);
        }
        SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer.getExecutor(
                Enums.HackType.MorePackets
        ).handle(e.isCancelled(), null);
    }

}
