package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
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

    public static List<SpartanPlayer> getPlayers(boolean all) {
        if (!notifications.isEmpty()) {
            List<SpartanPlayer> list = new ArrayList<>(notifications.size());

            for (Map.Entry<UUID, Integer> entry : notifications.entrySet()) {
                SpartanProtocol p = SpartanBukkit.getProtocol(entry.getKey());

                if (p != null && canAcceptMessages(p.spartanPlayer, entry.getValue(), all)) {
                    list.add(p.spartanPlayer);
                }
            }
            return list;
        }
        return new ArrayList<>(0);
    }

    public static boolean isEnabled(SpartanPlayer p) {
        return notifications.containsKey(p.uuid);
    }

    public static boolean hasPermission(SpartanPlayer p) {
        return Permissions.has(p, Enums.Permission.NOTIFICATIONS);
    }

    public static Integer getFrequency(SpartanPlayer p, boolean absolute) {
        Integer frequency = notifications.get(p.uuid);
        return frequency != null ? (absolute ? Math.abs(frequency) : frequency) : null;
    }

    public static boolean canAcceptMessages(SpartanPlayer p, Integer frequency, boolean all) {
        return frequency != null
                && (frequency < 0
                && frequency != defaultFrequency

                || (all
                || frequency >= 0
                || !ResearchEngine.enoughData()
                || Config.settings.getBoolean("Notifications.individual_only_notifications"))
                && hasPermission(p));
    }

    public static void set(SpartanPlayer p, int i) {
        Integer frequency = notifications.put(p.uuid, i);

        if (frequency == null) {
            p.sendMessage(Config.messages.getColorfulString("notifications_enable"));
        } else if (frequency != i) {
            p.sendMessage(Config.messages.getColorfulString("notifications_modified"));
        } else {
            notifications.remove(p.uuid);
            p.sendMessage(Config.messages.getColorfulString("notifications_disable"));
        }
    }

}
