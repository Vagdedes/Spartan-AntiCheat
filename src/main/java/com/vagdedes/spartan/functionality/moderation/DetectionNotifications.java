package com.vagdedes.spartan.functionality.moderation;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionNotifications {

    static {
        SuspicionNotifications.run();
    }

    public static final int defaultFrequency = Integer.MIN_VALUE;

    private static final Map<UUID, Integer> notifications = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> verbose = new ConcurrentHashMap<>();

    public static List<PlayerProtocol> getPlayers() {
        if (!notifications.isEmpty()) {
            List<PlayerProtocol> list = new ArrayList<>(notifications.size());

            for (UUID uuid : notifications.keySet()) {
                PlayerProtocol protocol = PluginBase.getProtocol(uuid);

                if (protocol != null) {
                    list.add(protocol);
                }
            }
            return list;
        }
        return new ArrayList<>(0);
    }

    public static boolean isEnabled(PlayerProtocol p) {
        return notifications.containsKey(p.getUUID());
    }

    public static boolean isVerboseEnabled(PlayerProtocol p) {
        return verbose.containsKey(p.getUUID());
    }

    public static boolean hasPermission(PlayerProtocol p) {
        return Permissions.has(p.bukkit(), Enums.Permission.NOTIFICATIONS);
    }

    public static Integer getFrequency(PlayerProtocol p) {
        return notifications.get(p.getUUID());
    }

    public static void remove(PlayerProtocol p) {
        notifications.remove(p.getUUID());
    }

    public static void removeVerbose(PlayerProtocol p) {
        UUID uuid = p.getUUID();

        if (verbose.containsKey(uuid)) {
            verbose.remove(uuid);
            p.bukkit().sendMessage(Config.messages.getColorfulString("verbose_disable"));
        }
    }

    public static void set(PlayerProtocol p, int i) {
        Integer frequency = notifications.put(p.getUUID(), i);

        if (frequency == null) {
            p.bukkit().sendMessage(Config.messages.getColorfulString("notifications_enable"));
        } else if (frequency != i) {
            p.bukkit().sendMessage(Config.messages.getColorfulString("notifications_modified"));
        } else {
            notifications.remove(p.getUUID());
            p.bukkit().sendMessage(Config.messages.getColorfulString("notifications_disable"));
        }
    }

    public static void addVerbose(PlayerProtocol p) {
        UUID uuid = p.getUUID();

        if (!verbose.containsKey(uuid)) {
            verbose.put(uuid, true);
            p.bukkit().sendMessage(Config.messages.getColorfulString("verbose_enable"));
        }
    }

    // Separator

    public static void runOnLeave(PlayerProtocol p) {
        if (notifications.containsKey(p.getUUID())
                && !DetectionNotifications.hasPermission(p)) {
            notifications.remove(p.getUUID());
        }
    }

}
