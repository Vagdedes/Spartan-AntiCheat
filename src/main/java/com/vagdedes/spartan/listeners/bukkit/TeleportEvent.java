package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        teleport(e.getPlayer(), false, e);
    }

    public static void teleport(Player player, boolean packets, Object object) {
        PlayerProtocol protocol = PluginBase.getProtocol(player, true);

        if (protocol.packetsEnabled() == packets) {
            protocol.bukkitExtra.resetCrucialData();
            protocol.profile().executeRunners(null, object);
            protocol.bukkitExtra.trackers.add(PlayerTrackers.TrackerType.TELEPORT, "tp", 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        respawn(e.getPlayer(), false, e);
    }

    public static void respawn(Player player, boolean packets, Object object) {
        PlayerProtocol protocol = PluginBase.getProtocol(player, true);

        if (protocol.packetsEnabled() == packets) {
            protocol.bukkitExtra.resetCrucialData();
            protocol.profile().executeRunners(null, object);
        }
    }

}
