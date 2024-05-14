package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class EventsHandler7 implements Listener {

    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.NoFall,
            Enums.HackType.IrregularMovements,
            Enums.HackType.Speed,
            Enums.HackType.MorePackets,
            Enums.HackType.ImpossibleInventory,
            Enums.HackType.Exploits
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation vehicle = p.movement.getVehicleLocation(n);
        SpartanLocation
                to = vehicle != null ? vehicle : new SpartanLocation(p, nto),
                from = new SpartanLocation(p, e.getFrom());
        from.retrieveDataFrom(to);

        // Values
        double preXZ = AlgebraUtils.getSquare(to.getX(), from.getX()) + AlgebraUtils.getSquare(to.getZ(), from.getZ()),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getSquare(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);

        if (!p.movement.processLastMoveEvent(to, from, dis, hor, ver, box)) {
            return;
        }
        MovementProcessing.run(p, to, ver, box);

        // Patterns
        for (Enums.HackType hackType : handledChecks) {
            if (p.getViolations(hackType).prevent()) {
                break;
            }
        }

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.KillAura).run(cancelled);
        p.getExecutor(Enums.HackType.NoFall).run(cancelled);
        p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).run(cancelled);
        p.getExecutor(Enums.HackType.MorePackets).run(cancelled);
    }

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
                    SpartanPlayer p = SpartanBukkit.getPlayer(n);

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
    private void Vehicle(VehicleEnterEvent e) {
        Entity entity = e.getEntered();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p == null) {
                return;
            }
            p.getTrackers().add(Trackers.TrackerType.VEHICLE, "enter", 5);
        }
    }

}
