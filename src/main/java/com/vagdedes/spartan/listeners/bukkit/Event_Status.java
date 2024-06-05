package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerFight;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.net.InetAddress;

public class Event_Status implements Listener {

    @EventHandler
    private void PreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            InetAddress address = e.getAddress();
            String ipAddress = address != null ? PlayerLimitPerIP.get(address) : null;
            CloudConnections.updatePunishedPlayer(e.getUniqueId(), ipAddress);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Leave(PlayerQuitEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.removePlayer(n);

        // Features
        PlayerLimitPerIP.remove(n);

        if (p == null) {
            return;
        }
        // Features
        PlayerDetectionSlots.remove(p);
        ChatProtection.remove(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Death(PlayerDeathEvent e) {
        Player n = e.getEntity();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Detections
        p.getExecutor(Enums.HackType.AutoRespawn).run(false);
        p.getExecutor(Enums.HackType.ImpossibleInventory).handle(false, null);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        // Objects
        Player killer = n.getKiller();

        if (killer != null && killer.isOnline()) {
            SpartanPlayer p2 = SpartanBukkit.getPlayer(killer);

            if (p2 != null) {
                PlayerFight fight = p.getProfile().playerCombat.getFight(p2);

                if (fight != null) {
                    fight.setWinner(p2);
                }
            }
        }
        p.resetTrackers();
        p.movement.setDetectionLocation(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }

        // Objects
        p.movement.setDetectionLocation(true);

        // Protections
        p.resetTrackers();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

}
