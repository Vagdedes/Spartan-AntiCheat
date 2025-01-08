package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
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

public class InventoryEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ItemDrop(PlayerDropItemEvent e) {
        SpartanProtocol p = SpartanBukkit.getProtocol(e.getPlayer(), true);

        // Detections
        p.profile().getRunner(Enums.HackType.FastClicks).handle(false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void InventoryClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();

        if (BlockUtils.hasMaterial(item)) {
            Player n = (Player) e.getWhoClicked();
            SpartanProtocol p = SpartanBukkit.getProtocol(n, true);
            boolean cancelled = e.isCancelled();
            ClickType click = e.getClick();
            String title = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? StringUtils.getClearColorString(n.getOpenInventory().getTitle()) : n.getOpenInventory().getTitle();
            int slot = e.getSlot();

            // Detections
            p.profile().getRunner(Enums.HackType.ImpossibleInventory).handle(cancelled, e);
            p.profile().getRunner(Enums.HackType.InventoryClicks).handle(cancelled, e);

            // GUIs
            if (p.profile().getRunner(Enums.HackType.ImpossibleInventory).prevent()
                    | p.profile().getRunner(Enums.HackType.InventoryClicks).prevent()) {
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
