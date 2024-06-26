package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.Elytra;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class EventsHandler_1_9 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void GlideEvent(EntityToggleGlideEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p == null) {
                return;
            }
            Elytra.judge(p, true);
        }
    }
}
