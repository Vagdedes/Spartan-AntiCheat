package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class ElytraEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(EntityToggleGlideEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            PlayerProtocol protocol = PluginBase.getProtocol((Player) entity);
            protocol.profile().executeRunners(e.isCancelled(), e);
        }
    }
}
