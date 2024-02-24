package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.abstraction.InventoryMenu;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Piston;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
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
        CheckProtection.cancel(60, 30);
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

        if (p.getViolations(Enums.HackType.ItemDrops).process()) {
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
            if (p.getViolations(Enums.HackType.ImpossibleInventory).process()
                    | p.getViolations(Enums.HackType.InventoryClicks).process()) {
                e.setCancelled(true);
            } else if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                for (InventoryMenu menu : SpartanMenu.menus) {
                    if (menu.handle(p, title, item, click, slot)) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        }
    }
}
