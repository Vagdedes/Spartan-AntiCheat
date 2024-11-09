package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Vehicle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class Event_VehicleDeath implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                ? entity.getPassengers().toArray(new Entity[0])
                : new Entity[]{entity.getPassenger()};

        if (passengers.length > 0) {
            for (Entity passenger : passengers) {
                if (passenger instanceof Player) {
                    Event_Vehicle.exit(SpartanBukkit.getProtocol((Player) passenger).spartan);
                }
            }
        }
    }

}
