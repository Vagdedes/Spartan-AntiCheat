package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
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
        PlayerProtocol p = PluginBase.getProtocol(e.getPlayer(), true);
        p.profile().executeRunners(e.isCancelled(), e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void InventoryClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();

        if (BlockUtils.hasMaterial(item)) {
            Player n = (Player) e.getWhoClicked();
            PlayerProtocol p = PluginBase.getProtocol(n, true);
            p.profile().executeRunners(false, e);

            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                ClickType click = e.getClick();
                String title = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                        ? StringUtils.getClearColorString(n.getOpenInventory().getTitle())
                        : n.getOpenInventory().getTitle();
                int slot = e.getSlot();

                for (InventoryMenu menu : PluginBase.menus) {
                    if (menu.handle(p, title, item, click, slot)) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        }
    }

}
