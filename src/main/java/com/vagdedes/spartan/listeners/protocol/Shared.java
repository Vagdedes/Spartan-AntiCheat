package com.vagdedes.spartan.listeners.protocol;

import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.event.PlayerStayEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Combat;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Shared {

    public static void join(PlayerJoinEvent e) {
        Player n = e.getPlayer();

        // Utils
        if (PlayerLimitPerIP.add(n)) {
            e.setJoinMessage(null);
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // System
        PlayerDetectionSlots.add(p);

        if (!SpartanEdition.attemptNotification(p)
                && Config.settings.getBoolean("Important.enable_watermark")) {
            p.sendMessage("");
            AwarenessNotifications.forcefullySend(
                    p,
                    "\nThis server is protected by the Spartan AntiCheat",
                    false
            );
            p.sendMessage("");
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

    public static void velocity(PlayerVelocityEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        p.addReceivedVelocity(e);

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    public static void stay(PlayerStayEvent e) { // Packets Only
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.Speed).handle(false, e);
    }

    public static void attack(PlayerAttackEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        boolean cancelled = e.isCancelled();

        for (Enums.HackType hackType : Event_Combat.handledChecks) {
            p.getExecutor(hackType).handle(cancelled, e);
        }
    }

}
