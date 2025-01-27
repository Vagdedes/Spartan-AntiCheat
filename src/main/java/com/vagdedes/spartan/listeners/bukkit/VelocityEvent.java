package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;

public class VelocityEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerVelocityEvent e) {
        event(
                new CPlayerVelocityEvent(
                        e.getPlayer(),
                        e.getVelocity()
                ),
                false
        );
    }

    public static void event(CPlayerVelocityEvent e, boolean packets) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            protocol.claimedVelocity = e;
            protocol.claimedVeloGravity.add(e);

            if (protocol.claimedVeloGravity.size() > 2) {
                protocol.claimedVeloGravity.remove(0);
            }
            protocol.claimedVeloSpeed.add(e);

            if (protocol.claimedVeloSpeed.size() > 2) {
                protocol.claimedVeloSpeed.remove(0);
            }
            protocol.profile().executeRunners(e.isCancelled(), e);
        }
    }

}
