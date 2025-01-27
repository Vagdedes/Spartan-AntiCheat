package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTransactionEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerBukkit;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
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
        PluginBase.getProtocol(e.getPlayer(), true).bukkitExtra.resetCrucialData();
    }

    public static void event(PlayerMoveEvent e, boolean packets) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            PlayerBukkit p = protocol.bukkitExtra;

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
            protocol.profile().executeRunners(e.isCancelled(), e);
        }
    }

    public static void tick(PlayerTickEvent tickEvent) {
        PlayerProtocol protocol = tickEvent.protocol;
        protocol.lastTickEvent = tickEvent;
        protocol.packetWorld.tick(tickEvent);
        protocol.profile().executeRunners(false, tickEvent);
    }

    public static void transaction(PlayerTransactionEvent event) {
        PlayerProtocol protocol = event.protocol;
        protocol.profile().executeRunners(false, event);
    }

}
