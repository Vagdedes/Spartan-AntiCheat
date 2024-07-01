package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class Event_Vehicle implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Death(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                ? entity.getPassengers().toArray(new Entity[0])
                : new Entity[]{entity.getPassenger()};

        if (passengers.length > 0) {
            Enums.HackType[] hackTypes = new Enums.HackType[]{
                    Enums.HackType.NoFall,
                    Enums.HackType.IrregularMovements,
            };

            for (Entity passenger : passengers) {
                if (passenger instanceof Player) {
                    Player n = (Player) passenger;
                    SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

                    if (p != null) {
                        for (Enums.HackType hackType : hackTypes) {
                            p.getViolations(hackType).addDisableCause(
                                    hackType.toString(),
                                    null,
                                    1
                            );
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Enter(VehicleEnterEvent e) {
        Entity entity = e.getEntered();

        if (entity instanceof Player) {
            Player n = (Player) entity;

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p == null) {
                return;
            }
            p.trackers.add(Trackers.TrackerType.VEHICLE, "enter", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Exit(VehicleExitEvent e) {
        Entity en = e.getExited();

        if (en instanceof Player) {
            Player n = (Player) en;

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p == null) {
                return;
            }
            p.trackers.add(Trackers.TrackerType.VEHICLE, "exit", 5);
        }
    }

}
