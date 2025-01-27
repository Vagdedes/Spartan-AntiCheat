package com.vagdedes.spartan.functionality.moderation;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.PluginBase;
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
        PluginBase.runRepeatingTask(() -> PluginBase.connectionThread.executeIfFree(() -> {
            if (Config.sql.isEnabled()) {
                List<PlayerProtocol> protocols = DetectionNotifications.getPlayers();

                if (!protocols.isEmpty()) {
                    run(protocols);
                }
            }
        }), 1L, 1L);
    }

    @SneakyThrows
    private static void run(List<PlayerProtocol> protocols) {
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

                                if (certaintyString != null) {
                                    notification = "§l[" + serverName + "]§r " + notification;

                                    for (PlayerProtocol protocol : protocols) {
                                        CheckDetection
                                                staffDetection = protocol.profile().getRunner(hackType).getDetection(detection),
                                                playerDetection = ResearchEngine.getPlayerProfile(playerName).getRunner(hackType).getDetection(detection);

                                        if (staffDetection != null
                                                && playerDetection != null
                                                && staffDetection.canSendNotification(
                                                playerDetection,
                                                System.currentTimeMillis(),
                                                Double.parseDouble(certaintyString)
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
    }

    public static String getServerName() {
        return Config.settings.getString(Settings.crossServerNotificationsName);
    }

}
