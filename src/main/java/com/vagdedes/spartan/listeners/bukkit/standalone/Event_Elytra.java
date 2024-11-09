package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.Elytra;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class Event_Elytra implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(EntityToggleGlideEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Elytra.judge(SpartanBukkit.getProtocol((Player) entity));
        }
    }
}
