package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.java.OverflowMap;
import lombok.Cleanup;
import lombok.SneakyThrows;
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
                List<SpartanProtocol> protocols = DetectionNotifications.getPlayers();
                if (!protocols.isEmpty()) result(protocols);
            }
        }), 1L, 1L);
    }

    @SneakyThrows
    private static void result(List<SpartanProtocol> protocols) {
        @Cleanup
        ResultSet rs = Config.sql.query("SELECT "
                        + "id, player_name, server_name, notification, information, functionality"
                        + " FROM " + Config.sql.getTable()
                        + " WHERE notification IS NOT NULL"
                        + " ORDER BY id DESC LIMIT " + rowLimit + ";");

        if (rs != null) {
            while (rs.next()) {
                long id = rs.getLong("id");

                if (!processed.containsKey(id)) {
                    String functionality = rs.getString("functionality"),
                                    playerName = rs.getString("player_name");

                    if (playerName != null) {
                        for (Enums.HackType hackType : Enums.HackType.values()) {
                            if (hackType.toString().equals(functionality)) {
                                String notification = rs.getString("notification"),
                                                serverName = rs.getString("server_name"),
                                                detection = ResearchEngine.findInformation(
                                                                rs.getString("information"),
                                                                CheckDetection.detectionIdentifier
                                                ),
                                                certaintyString = ResearchEngine.findInformation(
                                                                rs.getString("information"),
                                                                CheckDetection.certaintyIdentifier
                                                );

                                notification = "§l[" + serverName + "]§r " + notification;

                                for (SpartanProtocol protocol : protocols) {
                                    if (protocol.profile().getRunner(hackType).getDetection(detection).canSendNotification(
                                                    ResearchEngine.getPlayerProfile(playerName).getRunner(hackType).getDetection(detection),
                                                    System.currentTimeMillis(),
                                                    certaintyString == null ? -1.0 : Double.parseDouble(certaintyString)
                                    )) {
                                        protocol.bukkit().sendMessage(notification);
                                        processed.put(id, true);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static String getServerName() {
        return Config.settings.getString(Settings.crossServerNotificationsName);
    }

}
