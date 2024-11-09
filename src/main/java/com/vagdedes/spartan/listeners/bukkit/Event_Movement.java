package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class Event_Movement implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void MoveEvent(PlayerMoveEvent e) {
        event(e, false);
        SpartanBukkit.getProtocol(e.getPlayer()).spartan.getExecutor(
                Enums.HackType.MorePackets
        ).handle(e.isCancelled(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WorldEvent(PlayerChangedWorldEvent e) {
        SpartanBukkit.getProtocol(e.getPlayer()).spartan.resetCrucialData();
    }

    public static void event(PlayerMoveEvent e, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartan;
            Location nto = e.getTo();

            if (nto == null) {
                p.movement.judgeGround();
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
                p.movement.judgeGround();
                return;
            }
            MovementProcessing.run(protocol, to, ver, box);

            // Detections
            boolean cancelled = e.isCancelled();
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
            p.getExecutor(Enums.HackType.Exploits).handle(cancelled, e);
            p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
            p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
            p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
            p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
            p.getExecutor(Enums.HackType.KillAura).handle(cancelled, e);
            p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);
            p.getExecutor(Enums.HackType.Simulation).handle(cancelled, e);
            p.getExecutor(Enums.HackType.MorePackets).handle(cancelled, null);
        }
    }

    public static void tick(PlayerTickEvent tickEvent) {
        SpartanProtocol protocol = tickEvent.protocol;
        SpartanPlayer p = protocol.spartan;
        p.getExecutor(Enums.HackType.MorePackets).handle(false, tickEvent);
    }

}
