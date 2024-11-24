package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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
        protocol.claimedVeloGravity.add(e);
        if (protocol.claimedVeloGravity.size() > 2)
            protocol.claimedVeloGravity.remove(0);

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartan;

            // Detections
            boolean cancelled = e.isCancelled();
            p.getRunner(Enums.HackType.Speed).handle(cancelled, e);
            p.getRunner(Enums.HackType.Velocity).handle(cancelled, e);
            p.getRunner(Enums.HackType.Simulation).handle(cancelled, e);
        }
    }
    public static void claim(CPlayerVelocityEvent e) {
        // gravity
    }

}
