package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class Event_Vehicle implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void enter(VehicleEnterEvent e) {
        if (!e.isCancelled()) {
            Entity entity = e.getEntered();

            if (entity instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;
                p.trackers.add(Trackers.TrackerType.VEHICLE, "enter", 5);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void exit(VehicleExitEvent e) {
        if (!e.isCancelled()) {
            Entity en = e.getExited();

            if (en instanceof Player) {
                exit(SpartanBukkit.getProtocol((Player) en).spartanPlayer);
            }
        }
    }

    public static void exit(SpartanPlayer player) {
        player.trackers.add(Trackers.TrackerType.VEHICLE, "exit", 5);
    }

}
