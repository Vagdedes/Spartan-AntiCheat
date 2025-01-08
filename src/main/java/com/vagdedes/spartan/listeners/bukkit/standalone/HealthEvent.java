package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
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
            SpartanProtocol p = SpartanBukkit.getProtocol((Player) entity);
            boolean cancelled = e.isCancelled();

            // Detections
            p.profile().getRunner(Enums.HackType.FastEat).handle(cancelled, e);
            p.profile().getRunner(Enums.HackType.FastHeal).handle(cancelled, e);

            if (p.profile().getRunner(Enums.HackType.FastEat).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Regen(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanProtocol p = SpartanBukkit.getProtocol((Player) entity);

            // Detections
            p.profile().getRunner(Enums.HackType.FastHeal).handle(e.isCancelled(), e);

            if (p.profile().getRunner(Enums.HackType.FastHeal).prevent()) {
                e.setCancelled(true);
            }
        }
    }

}
