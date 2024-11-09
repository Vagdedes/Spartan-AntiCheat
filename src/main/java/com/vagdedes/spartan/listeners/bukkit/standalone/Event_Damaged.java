package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class Event_Damaged implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(EntityDamageEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol((Player) entity);

            protocol.spartan.handleReceivedDamage();
        } else {
            handlePassengers(entity, false, e);
        }
    }

    public static void handlePassengers(Entity entity, boolean packets, EntityDamageEvent e) {
        Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                ? entity.getPassengers().toArray(new Entity[0])
                : new Entity[]{entity.getPassenger()};

        if (passengers.length > 0) {
            for (Entity passenger : passengers) {
                if (passenger instanceof Player) {
                    // Objects
                    SpartanProtocol protocol = SpartanBukkit.getProtocol((Player) passenger);

                    if (protocol.packetsEnabled() == packets) {
                        protocol.spartan.handleReceivedDamage();
                    }
                }
            }
        }
    }

}
