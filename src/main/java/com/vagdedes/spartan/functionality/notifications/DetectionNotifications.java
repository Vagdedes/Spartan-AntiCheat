package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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

    public static List<SpartanProtocol> getPlayers() {
        if (!notifications.isEmpty()) {
            List<SpartanProtocol> list = new ArrayList<>(notifications.size());

            for (UUID uuid : notifications.keySet()) {
                SpartanProtocol protocol = SpartanBukkit.getProtocol(uuid);

                if (protocol != null) {
                    list.add(protocol);
                }
            }
            return list;
        }
        return new ArrayList<>(0);
    }

    public static boolean isEnabled(SpartanProtocol p) {
        return notifications.containsKey(p.getUUID());
    }

    public static boolean hasPermission(SpartanProtocol p) {
        return Permissions.has(p.bukkit, Enums.Permission.NOTIFICATIONS);
    }

    public static Integer getFrequency(SpartanProtocol p) {
        return notifications.get(p.getUUID());
    }

    public static void remove(SpartanProtocol p) {
        notifications.remove(p.getUUID());
    }

    public static void set(SpartanProtocol p, int i) {
        Integer frequency = notifications.put(p.getUUID(), i);

        if (frequency == null) {
            p.bukkit.sendMessage(Config.messages.getColorfulString("notifications_enable"));
        } else if (frequency != i) {
            p.bukkit.sendMessage(Config.messages.getColorfulString("notifications_modified"));
        } else {
            notifications.remove(p.getUUID());
            p.bukkit.sendMessage(Config.messages.getColorfulString("notifications_disable"));
        }
    }

    // Separator

    public static void runOnLeave(SpartanProtocol p) {
        if (notifications.containsKey(p.getUUID())
                && !DetectionNotifications.hasPermission(p)) {
            notifications.remove(p.getUUID());
        }
    }

}
