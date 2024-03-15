package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

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

    public static boolean areEnabled() {
        return Config.settings.getBoolean("Notifications.awareness_notifications");
    }

    public static boolean canSend(UUID uuid, String key, int secondsCooldown) {
        boolean hasCooldown = secondsCooldown != Integer.MAX_VALUE;
        Map<String, Long> childMap = map.get(uuid);
        boolean send;

        if (childMap != null) {
            Long time = childMap.get(key);

            if (time == null) {
                childMap.put(key, hasCooldown ? System.currentTimeMillis() + (secondsCooldown * 1_000L) : 0L);
                send = true;
            } else if (time == 0L) {
                send = false;
            } else {
                long currentTime = System.currentTimeMillis();

                if (currentTime >= time) {
                    childMap.put(key, hasCooldown ? currentTime + (secondsCooldown * 1_000L) : 0L);
                    send = true;
                } else {
                    send = false;
                }
            }
        } else {
            childMap = new LinkedHashMap<>();
            childMap.put(key, hasCooldown ? System.currentTimeMillis() + (secondsCooldown * 1_000L) : 0L);
            map.put(uuid, childMap);
            send = true;
        }
        return send;
    }

    public static boolean canSend(UUID uuid, String key) {
        return canSend(uuid, key, 24 * 60 * 60);
    }

    public static String getNotification(String s, boolean disableMessage) {
        return Config.messages.getColorfulString("awareness_notification").replace(":", "§8:§7").replace("{info}", s)
                + (disableMessage ? AwarenessNotifications.disableMessage : "");
    }

    public static String getNotification(String s) {
        return getNotification(s, false);
    }

    public static String getOptionalNotification(String s) {
        return !areEnabled() || s == null ? null : getNotification(s, true);
    }

    public static void forcefullySend(Object sender, String message) {
        message = getNotification(message);

        if (sender != null) {
            if (sender instanceof CommandSender) { // Player
                CommandSender commandSender = (CommandSender) sender;
                commandSender.sendMessage(message);

                if (commandSender != Bukkit.getConsoleSender()) {
                    Bukkit.getConsoleSender().sendMessage("(" + commandSender.getName() + ") " + message);
                }
            } else if (sender instanceof SpartanPlayer) {
                SpartanPlayer spartanPlayer = (SpartanPlayer) sender;
                spartanPlayer.sendMessage(message);
                Bukkit.getConsoleSender().sendMessage("(" + spartanPlayer.name + ") " + message);
            }
        } else { // Console
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    public static void forcefullySend(String message) {
        Bukkit.getConsoleSender().sendMessage(getNotification(message));
    }
}
