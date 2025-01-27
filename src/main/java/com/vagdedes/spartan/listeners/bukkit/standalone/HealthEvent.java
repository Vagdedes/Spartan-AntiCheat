package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HealthEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Food(FoodLevelChangeEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            PlayerProtocol p = PluginBase.getProtocol((Player) entity);
            p.profile().executeRunners(e.isCancelled(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Regen(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            PlayerProtocol p = PluginBase.getProtocol((Player) entity);
            p.profile().executeRunners(e.isCancelled(), e);
        }
    }

}
