package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.gui.configuration.ManageConfiguration;
import me.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
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

            // Features
            ManageConfiguration.save(p, false);
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

            // Utils
            if (sneaking) {
                p.getBuffer().start("sneaking-counter", 5);
            }

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
            if (HackPrevention.canCancel(p, Enums.HackType.EntityMove)) {
                e.setCancelled(true);
            } else {
                // Handlers
                VehicleAccess.runEnter(p, e.getEntered(), true);
            }
        }
    }
}
