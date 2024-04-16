package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Collection;
import java.util.List;

public class EventsHandler9 implements Listener {

    private static long InventoryMoveItemEventCooldown = 0L;

    @EventHandler
    private void PluginEnable(PluginEnableEvent e) {

        // Utils
        PluginUtils.clear();

        // System
        Config.compatibility.fastClear();
    }

    @EventHandler
    private void PluginDisable(PluginDisableEvent e) {

        // Utils
        PluginUtils.clear();

        // System
        Config.compatibility.fastClear();
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
