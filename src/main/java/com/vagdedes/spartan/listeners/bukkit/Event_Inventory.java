package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class Event_Inventory implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ItemDrop(PlayerDropItemEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.ItemDrops).run(e.isCancelled());

        if (p.getViolations(Enums.HackType.ItemDrops).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void InventoryClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();

        if (BlockUtils.hasMaterial(item)) {
            Player n = (Player) e.getWhoClicked();
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p == null) {
                return;
            }
            boolean cancelled = e.isCancelled();
            ClickType click = e.getClick();
            String title = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? StringUtils.getClearColorString(n.getOpenInventory().getTitle()) : n.getOpenInventory().getTitle();
            int slot = e.getSlot();

            // Detections
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
