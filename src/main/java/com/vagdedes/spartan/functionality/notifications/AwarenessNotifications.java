package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AwarenessNotifications {

    private static final Map<UUID, Map<String, Long>> map = new LinkedHashMap<>();
    private static final String disableMessage = " You can disable Awareness Notifications via " + Config.settings.getFile().getName() + ".";
    public static final UUID uuid = UUID.randomUUID();

    public static void clear() {
        map.clear();
    }

    public static void refresh() {
        map.remove(uuid);
    }

    private static boolean areEnabled() {
        return Config.settings.getBoolean("Notifications.awareness_notifications");
    }

    public static boolean canSend(UUID uuid, String key, int secondsCooldown) {
        Map<String, Long> childMap = map.get(uuid);
        boolean send;

        if (childMap != null) {
            Long time = childMap.get(key);

            if (time == null) {
                childMap.put(key, System.currentTimeMillis() + (secondsCooldown * 1_000L));
                send = true;
            } else if (time == 0L) {
                send = false;
            } else {
                long currentTime = System.currentTimeMillis();

                if (currentTime >= time) {
                    childMap.put(key, currentTime + (secondsCooldown * 1_000L));
                    send = true;
                } else {
                    send = false;
                }
            }
        } else {
            childMap = new LinkedHashMap<>();
            childMap.put(key, System.currentTimeMillis() + (secondsCooldown * 1_000L));
            map.put(uuid, childMap);
            send = true;
        }
        return send;
    }

    private static String getNotification(String s, boolean disableMessage) {
        return Config.messages.getColorfulString("awareness_notification").replace(":", "§8:§7").replace("{info}", s)
                + (disableMessage ? AwarenessNotifications.disableMessage : "");
    }

    public static String getNotification(String s) {
        return getNotification(s, false);
    }

    public static String getOptionalNotification(String s) {
        return !areEnabled() || s == null ? null : getNotification(s, true);
    }

    public static void forcefullySend(SpartanProtocol protocol, String message, boolean console) {
        message = getNotification(message);

        if (protocol != null) {
            protocol.bukkit.sendMessage(message);

            if (console) {
                Bukkit.getConsoleSender().sendMessage("(" + protocol.bukkit.getName() + ") " + message);
            }
        } else if (console) {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    public static void forcefullySend(String message) {
        Bukkit.getConsoleSender().sendMessage(getNotification(message));
    }

    public static void optionallySend(String message) {
        message = getOptionalNotification(message);

        if (message != null) {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }
    
}
