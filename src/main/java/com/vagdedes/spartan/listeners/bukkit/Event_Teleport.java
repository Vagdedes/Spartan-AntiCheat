package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets.MorePackets;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Event_Teleport implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        teleport(e.getPlayer(), false);
    }

    public static void teleport(Player player, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;

            // Object
            p.resetData(false);

            // Detections
            p.getExecutor(Enums.HackType.MorePackets).handle(false, MorePackets.TELEPORT);
            p.getExecutor(Enums.HackType.IrregularMovements).handle(false, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        respawn(e.getPlayer(), false);
    }

    public static void respawn(Player player, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;

            // Objects
            p.resetData(true);
            p.movement.setDetectionLocation();
        }
    }

}
