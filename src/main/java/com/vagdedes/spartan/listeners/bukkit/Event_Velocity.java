package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Event_Velocity implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerVelocityEvent e) {
        event(e, false);
    }

    public static void event(PlayerVelocityEvent e, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());
        protocol.claimedVelocity = e;

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;

            // Object
            if (!e.isCancelled()) {
                p.trackers.add(
                        PlayerTrackers.TrackerType.ABSTRACT_VELOCITY,
                        (int) (Math.ceil(e.getVelocity().length()) * TPS.maximum)
                );
            }

            // Detections
            boolean cancelled = e.isCancelled();
            p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
            p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
            p.getExecutor(Enums.HackType.Simulation).handle(cancelled, e);
        }
    }
    public static void claim(CPlayerVelocityEvent e) {
        // gravity
    }

}
