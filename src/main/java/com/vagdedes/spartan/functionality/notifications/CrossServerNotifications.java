package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import me.vagdedes.spartan.system.Enums;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrossServerNotifications {

    private static final int rowLimit = Enums.HackType.values().length;
    private static final Map<Long, Boolean> processed = new OverflowMap<>(
            new ConcurrentHashMap<>(),
            rowLimit
    );

    static {
        SpartanBukkit.runRepeatingTask(() -> SpartanBukkit.connectionThread.executeIfFree(() -> {
            if (Config.sql.isEnabled()) {
                List<SpartanPlayer> players = DetectionNotifications.getPlayers();

                if (!players.isEmpty()) {
                    try {
                        ResultSet rs = Config.sql.query("SELECT "
                                + "id, player_name, server_name, notification, functionality"
                                + " FROM " + Config.sql.getTable()
                                + " WHERE notification IS NOT NULL"
                                + " ORDER BY id DESC LIMIT " + rowLimit + ";");

                        if (rs != null) {
                            while (rs.next()) {
                                long id = rs.getLong("id");

                                if (!processed.containsKey(id)) {
                                    String functionality = rs.getString("functionality");

                                    for (Enums.HackType hackType : Enums.HackType.values()) {
                                        if (hackType.toString().equals(functionality)) {
                                            String notification = rs.getString("notification"),
                                                    serverName = rs.getString("server_name"),
                                                    playerName = rs.getString("player_name");
                                            notification = "§l[" + serverName + "]§r " + notification;

                                            for (SpartanPlayer player : players) {
                                                if (player.getExecutor(hackType).getDetection().canSendNotification(playerName)) {
                                                    player.getInstance().sendMessage(notification);
                                                    processed.put(id, true);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }), 1L, 1L);
    }

    public static String getServerName() {
        return Config.settings.getString(Settings.crossServerNotificationsName);
    }

}
