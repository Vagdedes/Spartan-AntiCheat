package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class Event_Health implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Food(FoodLevelChangeEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;
            boolean cancelled = e.isCancelled();

            // Detections
            p.getExecutor(Enums.HackType.FastEat).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastHeal).handle(cancelled, e);

            if (p.getExecutor(Enums.HackType.FastEat).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Regen(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;

            // Detections
            p.getExecutor(Enums.HackType.FastHeal).handle(e.isCancelled(), e);

            if (p.getExecutor(Enums.HackType.FastHeal).prevent()) {
                e.setCancelled(true);
            }
        }
    }

}