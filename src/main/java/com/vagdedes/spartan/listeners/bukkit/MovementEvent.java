package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTransactionEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void MoveEvent(PlayerMoveEvent e) {
        event(e, false);
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
            SpartanLocation to = vehicle != null
                    ? new SpartanLocation(vehicle)
                    : new SpartanLocation(nto);

            // Values
            double toY = to.getY(),
                    box = toY - to.getBlockY(),
                    ver = toY - nfrom.getY();

            if (!p.movement.processLastMoveEvent(
                    nto,
                    vehicle,
                    to,
                    e.getFrom(),
                    packets
            )) {
                p.movement.judgeGround();
                return;
            }
            MovementProcessing.run(protocol, to, ver, box);

            // Detections
            boolean cancelled = e.isCancelled();
            protocol.profile().getRunner(Enums.HackType.Exploits).handle(cancelled, null);
            protocol.profile().getRunner(Enums.HackType.Exploits).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.ImpossibleInventory).run(cancelled);
            protocol.profile().getRunner(Enums.HackType.IrregularMovements).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.Velocity).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.KillAura).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.Criticals).handle(cancelled, e);
        }
    }

    public static void tick(PlayerTickEvent tickEvent) {
        SpartanProtocol protocol = tickEvent.protocol;
        protocol.lastTickEvent = tickEvent;
        protocol.packetWorld.tick(tickEvent);
        protocol.profile().getRunner(Enums.HackType.MorePackets).handle(false, tickEvent);
        protocol.profile().getRunner(Enums.HackType.FastClicks).handle(false, tickEvent);
        protocol.profile().getRunner(Enums.HackType.IrregularMovements).handle(false, tickEvent);
    }

    public static void transaction(PlayerTransactionEvent event) {
        SpartanProtocol protocol = event.protocol;
        protocol.profile().getRunner(Enums.HackType.Velocity).handle(false, event);
    }

}
