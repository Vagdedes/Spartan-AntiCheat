package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.ElytraUse;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class EventsHandler_1_9 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void GlideEvent(EntityToggleGlideEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            boolean gliding = e.isCancelled() ? n.isGliding() : n.isGliding() || e.isGliding();

            // Objects
            ElytraUse.judge(p, gliding, true);
        }
    }
}
