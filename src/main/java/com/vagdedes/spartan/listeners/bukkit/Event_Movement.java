package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTransactionEvent;
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
        SpartanBukkit.getProtocol(e.getPlayer(), true).spartan.getRunner(
                Enums.HackType.MorePackets
        ).handle(e.isCancelled(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WorldEvent(PlayerChangedWorldEvent e) {
        SpartanBukkit.getProtocol(e.getPlayer(), true).spartan.resetCrucialData();
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
            Location vehicle = protocol.getVehicleLocation(),
                    nfrom = e.getFrom();
            SpartanLocation
                    to = vehicle != null ? new SpartanLocation(vehicle) : new SpartanLocation(nto),
                    from = new SpartanLocation(nfrom);

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
                    nto,
                    nfrom,
                    dis,
                    hor,
                    ver,
                    xDiff,
                    zDiff,
                    box,
                    packets
            )) {
                p.movement.judgeGround();
                return;
            }
            MovementProcessing.run(protocol, to, ver, box);

            // Detections
            boolean cancelled = e.isCancelled();
            p.getRunner(Enums.HackType.Exploits).handle(cancelled, null);
            p.getRunner(Enums.HackType.Exploits).handle(cancelled, e);
            p.getRunner(Enums.HackType.ImpossibleInventory).run(cancelled);
            p.getRunner(Enums.HackType.IrregularMovements).run(cancelled);
            p.getRunner(Enums.HackType.IrregularMovements).handle(cancelled, e);
            p.getRunner(Enums.HackType.Velocity).handle(cancelled, e);
            p.getRunner(Enums.HackType.KillAura).handle(cancelled, e);
            p.getRunner(Enums.HackType.Criticals).handle(cancelled, e);
            p.getRunner(Enums.HackType.Simulation).handle(cancelled, e);
            p.getRunner(Enums.HackType.MorePackets).handle(cancelled, null);
        }
    }

    public static void tick(PlayerTickEvent tickEvent) {
        SpartanProtocol protocol = tickEvent.protocol;
        protocol.lastTickEvent = tickEvent;
        protocol.packetWorld.tick(tickEvent);
        SpartanPlayer p = protocol.spartan;
        p.getRunner(Enums.HackType.MorePackets).handle(false, tickEvent);
    }

    public static void transaction(PlayerTransactionEvent event) {
        SpartanProtocol protocol = event.protocol;
        protocol.spartan.getRunner(Enums.HackType.Velocity).handle(false, event);
        //protocol.bukkit.sendMessage("delay: " + event.delay);
        // stub
    }

}
