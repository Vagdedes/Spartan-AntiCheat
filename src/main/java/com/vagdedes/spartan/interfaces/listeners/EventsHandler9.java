package com.vagdedes.spartan.interfaces.listeners;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.protections.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.net.InetAddress;

public class EventsHandler9 implements Listener {

    @EventHandler
    private void PreLoginEvent(AsyncPlayerPreLoginEvent e) {
        // Features
        if (IDs.isValid() && e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            InetAddress address = e.getAddress();
            String ipAddress = address != null ? PlayerLimitPerIP.get(address) : null;
            CloudConnections.updatePunishedPlayer(e.getUniqueId(), ipAddress);
        }
    }

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
    private void VehicleExit(VehicleExitEvent e) {
        Entity en = e.getExited();

        if (en instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) en);

            if (p == null) {
                return;
            }
            // Protections
            VehicleAccess.runExit(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void InventoryInteract(InventoryInteractEvent e) {
        HumanEntity he = e.getWhoClicked();

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
}
