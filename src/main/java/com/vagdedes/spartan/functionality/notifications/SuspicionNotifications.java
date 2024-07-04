package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SuspicionNotifications {

    private static final String comma = ", ";

    static void run() {
        SpartanBukkit.runRepeatingTask(() -> { // Here because there are no other class calls
            if (ResearchEngine.enoughData()
                    && !Config.settings.getBoolean("Notifications.individual_only_notifications")) {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();
                Iterator<SpartanPlayer> iterator = players.iterator();

                while (iterator.hasNext()) {
                    Integer divisor = DetectionNotifications.getFrequency(iterator.next(), false);

                    if (divisor == null || divisor != DetectionNotifications.defaultFrequency) {
                        iterator.remove();
                    }
                }
                run(players);
            }
        }, 1L, 300L);
    }

    private static void run(List<SpartanPlayer> playersList) {
        StringBuilder players = new StringBuilder();
        int size = 0, commaLength = comma.length();

        for (SpartanPlayer player : SpartanBukkit.getPlayers()) {
            Collection<Enums.HackType> list = player.protocol.getProfile().evidence.getKnowledgeList(false);

            if (!list.isEmpty()) {
                StringBuilder evidence = new StringBuilder();

                for (Enums.HackType hackType : list) {
                    if (player.getViolations(hackType).hasLevel()) {
                        evidence = new StringBuilder(
                                evidence
                                        .append(hackType.getCheck().getName())
                                        .append(comma)
                                        .substring(0, evidence.length() - commaLength)
                        );
                    }
                }

                if (evidence.length() > 0) {
                    size++;
                    players.append(player.name).append(comma);
                    SpartanLocation location = player.movement.getLocation();
                    CloudConnections.executeDiscordWebhook(
                            "checks",
                            player.uuid, player.name,
                            location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                            "Suspected For Hacking", evidence.toString()
                    );
                }
            }
        }

        if (size > 0) {
            String message = Config.messages.getColorfulString("suspicion_notification")
                    .replace("{size}", String.valueOf(size))
                    .replace("{players}", players.substring(0, players.length() - comma.length()));

            if (!playersList.isEmpty()) {
                for (SpartanPlayer staff : playersList) {
                    staff.sendMessage(message);
                }
            }
        }
    }

}
