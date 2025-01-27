package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.moderation.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ProtocolLib {

    private static boolean
            temporaryClass = false,
            bukkit = false;

    public static boolean isPacketSupported(String packet) {
        return BackgroundProtocolLib.isPacketSupported(packet);
    }

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
        bukkit = true;
        String message = AwarenessNotifications.getOptionalNotification(
                "Anti-Cheat checks work significantly better with ProtocolLib installed "
                        + "as it allows the use of packets which helps identify information earlier "
                        + "and more accurately."
        );

        if (message != null) {
            List<PlayerProtocol> players = Permissions.getStaff();

            if (!players.isEmpty()) {
                for (PlayerProtocol p : players) {
                    if (AwarenessNotifications.canSend(p.getUUID(), "protocol-lib", 0)) {
                        p.bukkit().sendMessage(message);
                    }
                }
            }
        }
    }

    public static boolean isTemporary(OfflinePlayer player) {
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

    public static Location getLocationOrNull(Entity entity) {
        if (entity instanceof Player) {
            return getLocationOrNull((Player) entity);
        } else {
            return entity == null
                    ? null
                    : entity.getLocation();
        }
    }

    public static Location getLocationOrNull(Player player) {
        if (ProtocolLib.isTemporary(player)) {
            return null;
        } else {
            return player == null
                    ? null
                    : player.getLocation();
        }
    }

    public static Entity getVehicle(Entity entity) {
        if (entity instanceof Player) {
            if (ProtocolLib.isTemporary((Player) entity)) {
                return null;
            } else {
                return entity.getVehicle();
            }
        } else {
            return entity.getVehicle();
        }
    }

    public static World getWorld(Entity entity) {
        if (entity instanceof Player) {
            if (ProtocolLib.isTemporary((Player) entity)) {
                return Bukkit.getWorlds().get(0);
            } else {
                return entity.getWorld();
            }
        } else {
            return entity.getWorld();
        }
    }

}
