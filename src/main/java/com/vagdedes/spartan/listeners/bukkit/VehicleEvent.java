package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.PlayerBukkit;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void enter(VehicleEnterEvent e) {
        if (!e.isCancelled()) {
            Entity entity = e.getEntered();

            if (entity instanceof Player) {
                PlayerBukkit p = PluginBase.getProtocol((Player) entity, true).bukkitExtra;
                p.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "enter", 5);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void exit(VehicleExitEvent e) {
        if (!e.isCancelled()) {
            Entity en = e.getExited();

            if (en instanceof Player) {
                exit(PluginBase.getProtocol((Player) en, true).bukkitExtra);
            }
        }
    }

    public static void exit(PlayerBukkit player) {
        player.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "exit", 5);
        player.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "exit_tp", 1);
        player.trackers.add(PlayerTrackers.TrackerType.VEHICLE, "vh_tp", 1);
    }

}
