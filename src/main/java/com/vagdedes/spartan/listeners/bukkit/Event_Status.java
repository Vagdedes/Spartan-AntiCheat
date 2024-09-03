package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class Event_Status implements Listener {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Join(PlayerJoinEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.createProtocol(n).spartanPlayer;

        if (Config.settings.getBoolean("Important.enable_watermark")
                && !Permissions.isStaff(p.getInstance())) {
            p.getInstance().sendMessage("");
            AwarenessNotifications.forcefullySend(
                    p,
                    "\nThis server is protected by the Spartan AntiCheat",
                    false
            );
            p.getInstance().sendMessage("");
        }

        // Detections
        p.getExecutor(Enums.HackType.Speed).handle(false, e);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        SpartanBukkit.runDelayedTask(p, () -> {
            if (p != null) {
                Config.settings.runOnLogin(p);
                CloudBase.announce(p);
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Leave(PlayerQuitEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.deleteProtocol(n);

        if (protocol == null) {
            return;
        }
        SpartanPlayer p = protocol.spartanPlayer;

        // Features
        DetectionNotifications.runOnLeave(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Death(PlayerDeathEvent e) {
        Player n = e.getEntity();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.AutoRespawn).handle(false, null);
        p.getExecutor(Enums.HackType.ImpossibleInventory).handle(false, null);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        // Objects
        p.resetTrackers();
        p.movement.setDetectionLocation();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // Objects
        p.resetTrackers();
        p.movement.setDetectionLocation();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void GameMode(PlayerGameModeChangeEvent e) {
        if (v1_8) {
            Player n = e.getPlayer();

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            // Objects
            if (e.getNewGameMode() == GameMode.SPECTATOR) {
                p.trackers.add(Trackers.TrackerType.SPECTATOR, Integer.MAX_VALUE);
            } else {
                p.trackers.remove(Trackers.TrackerType.SPECTATOR);
            }
        }
    }

}
