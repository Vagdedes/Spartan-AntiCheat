package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ProtocolLib {

    private static boolean
            temporaryClass = false,
            bukkit = false;

    private static void checkClass() {
        temporaryClass = ReflectionUtils.classExists(
                "com.comphenix.protocol.injector.temporary.TemporaryPlayer"
        );
    }

    public static void run() {
        checkClass();

        if (!bukkit) {
            BackgroundProtocolLib.run();
        }
    }

    public static void otherwise() {
        checkClass();
        Event_Chunks.clear();
        bukkit = true;
        String message = AwarenessNotifications.getOptionalNotification(
                "Anti-Cheat checks work significantly better with ProtocolLib installed "
                        + "as it allows the use of packets which helps identify information earlier "
                        + "and more accurately."
        );

        if (message != null) {
            List<SpartanPlayer> players = Permissions.getStaff();

            if (!players.isEmpty()) {
                for (SpartanPlayer p : players) {
                    if (AwarenessNotifications.canSend(p.protocol.getUUID(), "protocol-lib", 0)) {
                        p.getInstance().sendMessage(message);
                    }
                }
            }
        }
    }

    public static boolean isTemporary(Player player) {
        return temporaryClass && player instanceof TemporaryPlayer;
    }

    public static UUID getUUID(Entity entity) {
        if (entity instanceof Player) {
            if (ProtocolLib.isTemporary((Player) entity)) {
                return UUID.randomUUID();
            } else {
                return entity.getUniqueId();
            }
        } else {
            return entity.getUniqueId();
        }
    }

    public static int getEntityID(Entity entity) {
        if (entity instanceof Player) {
            if (ProtocolLib.isTemporary((Player) entity)) {
                return new Random().nextInt();
            } else {
                return entity.getEntityId();
            }
        } else {
            return entity.getEntityId();
        }
    }

    public static Location getLocation(Entity entity) {
        if (entity instanceof Player) {
            return getLocation((Player) entity);
        } else {
            return entity.getLocation();
        }
    }

    public static Location getLocation(Player player) {
        if (ProtocolLib.isTemporary(player)) {
            return new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
        } else {
            return player.getLocation();
        }
    }
}
