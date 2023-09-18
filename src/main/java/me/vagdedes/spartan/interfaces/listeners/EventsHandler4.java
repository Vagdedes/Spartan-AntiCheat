package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.moderation.PlayerReports;
import me.vagdedes.spartan.functionality.moderation.Spectate;
import me.vagdedes.spartan.gui.configuration.ManageChecks;
import me.vagdedes.spartan.gui.configuration.ManageConfiguration;
import me.vagdedes.spartan.gui.configuration.ManageOptions;
import me.vagdedes.spartan.gui.info.DebugMenu;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.gui.spartan.SpartanMenu;
import me.vagdedes.spartan.gui.spartan.SupportIncompatibleItems;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Piston;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
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
        if (!e.isCancelled()) {
            // Objects
            p.setInventory(n.getInventory(), n.getOpenInventory());

            // Detections
            p.getExecutor(Enums.HackType.ItemDrops).run();

            if (p.getViolations(Enums.HackType.ItemDrops).process()) {
                e.setCancelled(true);
            }
        } else {
            // Detections
            p.getExecutor(Enums.HackType.ItemDrops).run();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void ItemPickUp(PlayerPickupItemEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        if (Spectate.isUsing(p)) {
            e.setCancelled(true);
        } else {
            // Objects
            p.setInventory(n.getInventory(), n.getOpenInventory());
        }
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
            ClickType click = e.getClick();
            String title = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? StringUtils.getClearColorString(n.getOpenInventory().getTitle()) : n.getOpenInventory().getTitle();
            int slot = e.getSlot();

            // Objects
            if (!e.isCancelled()) {
                p.setInventory(n.getInventory(), n.getOpenInventory());

                // Detections
                p.getExecutor(Enums.HackType.ImpossibleInventory).handle(e);
                p.getExecutor(Enums.HackType.InventoryClicks).handle(e);
            }

            // GUIs
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && (SpartanMenu.run(p, item, title, click) | ManageChecks.run(p, item, title, click)
                    | ManageOptions.run(p, item, title) | PlayerInfo.run(p, item, title, click)
                    | DebugMenu.run(p, item, title) | ManageConfiguration.run(p, item, title, click, slot) |
                    PlayerReports.run(p, item, title) | SupportIncompatibleItems.run(p, item, title, click))
                    | p.getViolations(Enums.HackType.ImpossibleInventory).process()
                    | p.getViolations(Enums.HackType.InventoryClicks).process()) {
                e.setCancelled(true);
            }
        }
    }
}
