package com.vagdedes.spartan.listeners.bukkit;

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
import org.bukkit.event.player.PlayerAnimationEvent;

public class Events_Player implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Food(FoodLevelChangeEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            boolean cancelled = e.isCancelled();

            // Detections
            p.getExecutor(Enums.HackType.FastEat).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastHeal).handle(cancelled, e);

            if (p.getViolations(Enums.HackType.FastEat).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Regen(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }

            // Detections
            p.getExecutor(Enums.HackType.FastHeal).handle(e.isCancelled(), e);

            if (p.getViolations(Enums.HackType.FastHeal).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }

}
