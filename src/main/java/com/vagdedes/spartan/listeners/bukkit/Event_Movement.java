package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Event_Movement implements Listener {

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

        for (Enums.HackType hackType : handledChecks) {
            if (p.getViolations(hackType).prevent()) {
                break;
            }
        }

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.NoFall).run(cancelled);
        p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).run(cancelled);
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.KillAura).handle(cancelled, e);
        p.getExecutor(Enums.HackType.MorePackets).run(cancelled);
        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        p.movement.judgeGround();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

}
