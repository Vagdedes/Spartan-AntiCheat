package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.TridentUse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;

public class Event_Trident implements Listener {

    @EventHandler
    private void Event(PlayerRiptideEvent e) {
        event(e, false);
    }

    public static void event(PlayerRiptideEvent e, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            TridentUse.run(protocol);
        }
    }

}
