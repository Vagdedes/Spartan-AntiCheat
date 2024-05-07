package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.Collection;
import java.util.List;

public class EventsHandler8 implements Listener {

    private static long InventoryMoveItemEventCooldown = 0L;

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

        // Objects
        if (e.isCancelled()) {
            p.movement.setSprinting(n.isSprinting());
        } else {
            p.movement.setSprinting(e.isSprinting());
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
            p.movement.setSneaking(false);
            p.setEyeHeight(n.getEyeHeight());
        } else {
            boolean sneaking = e.isSneaking();

            // Objects
            p.movement.setSneaking(sneaking);
            p.setEyeHeight(n.getEyeHeight());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void InventoryMove(InventoryMoveItemEvent e) {
        long ms = System.currentTimeMillis();

        if (InventoryMoveItemEventCooldown <= ms) {
            InventoryMoveItemEventCooldown = ms + 50L;

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) { // It's faster to loop through Bukkit instead of using Bukkit's search method
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();

                if (!players.isEmpty()) {
                    for (Player n : players) {
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

                        if (p != null) {
                            p.setInventory(n.getInventory(), n.getOpenInventory());
                        }
                    }
                }
            } else {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    for (SpartanPlayer p : players) {
                        Player n = p.getPlayer();

                        if (n != null && n.isOnline()) {
                            p.setInventory(n.getInventory(), n.getOpenInventory());
                        }
                    }
                }
            }
        }
    }

}
