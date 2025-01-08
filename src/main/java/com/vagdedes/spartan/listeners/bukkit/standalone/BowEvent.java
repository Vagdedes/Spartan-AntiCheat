package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

public class BowEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(EntityShootBowEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanProtocol p = SpartanBukkit.getProtocol((Player) entity, true);

            // Detections
            p.profile().getRunner(Enums.HackType.FastBow).handle(e.isCancelled(), e);

            if (p.profile().getRunner(Enums.HackType.FastBow).prevent()) {
                e.setCancelled(true);
            }
        }
    }

}
