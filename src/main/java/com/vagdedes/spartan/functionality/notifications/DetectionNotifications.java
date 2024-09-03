package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
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

    public static List<SpartanPlayer> getPlayers() {
        if (!notifications.isEmpty()) {
            List<SpartanPlayer> list = new ArrayList<>(notifications.size());

            for (UUID uuid : notifications.keySet()) {
                SpartanProtocol p = SpartanBukkit.getProtocol(uuid);

                if (p != null) {
                    list.add(p.spartanPlayer);
                }
            }
            return list;
        }
        return new ArrayList<>(0);
    }

    public static boolean isEnabled(SpartanPlayer p) {
        return notifications.containsKey(p.getInstance().getUniqueId());
    }

    public static boolean hasPermission(SpartanPlayer p) {
        return Permissions.has(p.getInstance(), Enums.Permission.NOTIFICATIONS);
    }

    public static Integer getFrequency(SpartanPlayer p) {
        return notifications.get(p.getInstance().getUniqueId());
    }

    public static void remove(SpartanPlayer p) {
        notifications.remove(p.getInstance().getUniqueId());
    }

    public static void set(SpartanPlayer p, int i) {
        Integer frequency = notifications.put(p.getInstance().getUniqueId(), i);

        if (frequency == null) {
            p.getInstance().sendMessage(Config.messages.getColorfulString("notifications_enable"));
        } else if (frequency != i) {
            p.getInstance().sendMessage(Config.messages.getColorfulString("notifications_modified"));
        } else {
            notifications.remove(p.getInstance().getUniqueId());
            p.getInstance().sendMessage(Config.messages.getColorfulString("notifications_disable"));
        }
    }

    // Separator

    public static void runOnLeave(SpartanPlayer p) {
        if (notifications.containsKey(p.getInstance().getUniqueId())
                && !DetectionNotifications.hasPermission(p)) {
            notifications.remove(p.getInstance().getUniqueId());
        }
    }

}
