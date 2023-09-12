package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import java.util.Collection;
import java.util.List;

public class EventsHandler10 implements Listener {

    private static long InventoryMoveItemEventCooldown = 0L;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void InventoryMove(InventoryMoveItemEvent e) {
        long ms = System.currentTimeMillis();

        if (InventoryMoveItemEventCooldown <= ms) {
            InventoryMoveItemEventCooldown = ms + 50L;

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) { // It's faster to loop through Bukkit instead of using Bukkit's search method
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();

                if (players.size() > 0) {
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
