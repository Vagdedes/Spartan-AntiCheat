package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CheckDelay;
import com.vagdedes.spartan.functionality.tracking.Piston;
import com.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;

public class EventsHandler4 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldSave(WorldSaveEvent e) {
        CheckDelay.cancel(60, 30);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ItemDrop(PlayerDropItemEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        boolean cancelled = e.isCancelled();

        if (!cancelled) {
            // Objects
            p.setInventory(n.getInventory(), n.getOpenInventory());
        }

        // Detections
        p.getExecutor(Enums.HackType.ItemDrops).run(cancelled);

        if (p.getViolations(Enums.HackType.ItemDrops).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void ItemPickUp(PlayerPickupItemEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Objects
        p.setInventory(n.getInventory(), n.getOpenInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void PistonEvent(BlockPistonExtendEvent e) {
        // Handlers
        Piston.run(e.getBlock(), e.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void InventoryClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();

        if (item != null && item.getType() != Material.AIR) {
            Player n = (Player) e.getWhoClicked();
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            boolean cancelled = e.isCancelled();
            ClickType click = e.getClick();
            String title = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? StringUtils.getClearColorString(n.getOpenInventory().getTitle()) : n.getOpenInventory().getTitle();
            int slot = e.getSlot();

            // Objects
            if (!cancelled) {
                p.setInventory(n.getInventory(), n.getOpenInventory());
            }
            p.getExecutor(Enums.HackType.ImpossibleInventory).handle(cancelled, e);
            p.getExecutor(Enums.HackType.InventoryClicks).handle(cancelled, e);

            // GUIs
            if (p.getViolations(Enums.HackType.ImpossibleInventory).prevent()
                    | p.getViolations(Enums.HackType.InventoryClicks).prevent()) {
                e.setCancelled(true);
            } else if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                for (InventoryMenu menu : InteractiveInventory.menus) {
                    if (menu.handle(p, title, item, click, slot)) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        }
    }
}
