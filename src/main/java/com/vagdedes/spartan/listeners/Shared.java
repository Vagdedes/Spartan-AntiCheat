package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Shared {

    public static void velocity(PlayerVelocityEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        if (!e.isCancelled()) {
            p.trackers.add(
                    Trackers.TrackerType.ABSTRACT_VELOCITY,
                    (int) (Math.ceil(e.getVelocity().length()) * TPS.maximum)
            );
        }

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Simulation).handle(cancelled, e);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    public static void attack(PlayerAttackEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        boolean cancelled = e.isCancelled();

        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
        p.getExecutor(Enums.HackType.HitReach).handle(cancelled, e);
    }

    public static void useEntity(PlayerAttackEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        boolean cancelled = e.isCancelled();

        p.getExecutor(Enums.HackType.KillAura).handle(cancelled, e);
    }

    public static void movement(PlayerMoveEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation vehicle = p.movement.getVehicleLocation();
        SpartanLocation
                to = vehicle != null ? vehicle : new SpartanLocation(nto),
                from = new SpartanLocation(e.getFrom());

        // Values
        double xDiff = to.getX() - from.getX(),
                zDiff = to.getZ() - from.getZ(),
                preXZ = (xDiff * xDiff) + (zDiff * zDiff),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getSquare(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);

        if (!p.movement.processLastMoveEvent(
                nto,
                vehicle,
                to,
                from,
                dis,
                hor,
                ver,
                xDiff,
                zDiff,
                box
        )) {
            return;
        }
        MovementProcessing.run(p, to, ver, box);

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.NoFall).run(cancelled);
        p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.KillAura).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Simulation).handle(cancelled, e);
    }

}
