package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.simple.VehicleAccess;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class EventsHandler8 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void InventoryOpen(InventoryOpenEvent e) {
        HumanEntity he = e.getPlayer();

        if (he instanceof Player) {
            Player n = (Player) he;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.setInventory(n.getInventory(), n.getOpenInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void InventoryClose(InventoryCloseEvent e) {
        HumanEntity he = e.getPlayer();

        if (he instanceof Player) {
            Player n = (Player) he;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.setInventory(n.getInventory(), null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void SprintEvent(PlayerToggleSprintEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        if (e.isCancelled()) {
            // Objects
            p.setSprinting(n.isSprinting());
        } else {
            boolean sprinting = e.isSprinting();

            // Utils
            if (!sprinting) {
                p.setLastOffSprint();
            }

            // Objects
            p.setSprinting(sprinting);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void SneakEvent(PlayerToggleSneakEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        if (e.isCancelled()) {
            // Objects
            p.setSneaking(false);
            p.setEyeHeight(n.getEyeHeight());
        } else {
            boolean sneaking = e.isSneaking();

            // Objects
            p.setSneaking(sneaking);
            p.setEyeHeight(n.getEyeHeight());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void VehicleEnter(VehicleEnterEvent e) {
        Entity en = e.getEntered();

        if (en instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) en);

            if (p == null) {
                return;
            }
            if (p.getViolations(Enums.HackType.Speed).process()
                    || p.getViolations(Enums.HackType.IrregularMovements).process()) {
                e.setCancelled(true);
            } else {
                // Handlers
                VehicleAccess.runEnter(p, e.getEntered(), true);
            }
        }
    }
}
