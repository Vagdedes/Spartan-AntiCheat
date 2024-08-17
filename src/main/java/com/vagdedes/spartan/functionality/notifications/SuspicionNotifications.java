package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerEvidence;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SuspicionNotifications {

    private static final String comma = ", ";

    static void run() {
        SpartanBukkit.runRepeatingTask(() -> { // Here because there are no other class calls
            if (!Config.settings.getBoolean("Notifications.individual_only_notifications")) {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    List<SpartanPlayer> staff = new ArrayList<>(players);
                    Iterator<SpartanPlayer> iterator = staff.iterator();

                    while (iterator.hasNext()) {
                        Integer frequency = DetectionNotifications.getFrequency(iterator.next());

                        if (frequency == null || frequency < 100) {
                            iterator.remove();
                        }
                    }
                    run(staff, players);
                }
            }
        }, 1L, 300L);
    }

    private static void run(List<SpartanPlayer> staff, List<SpartanPlayer> online) {
        StringBuilder players = new StringBuilder();
        int size = 0, commaLength = comma.length();

        for (SpartanPlayer player : online) {
            Collection<Enums.HackType> list = player.protocol.getProfile().evidence.getKnowledgeList(
                    PlayerEvidence.notificationProbability,
                    PlayerEvidence.notificationRatio
            );

            if (!list.isEmpty()) {
                StringBuilder evidence = new StringBuilder();

                for (Enums.HackType hackType : list) {
                    if (player.getExecutor(hackType).hasLevel()) {
                        evidence.append(hackType.getCheck().getName()).append(comma);
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
                            "Suspected for", evidence.substring(0, evidence.length() - commaLength)
                    );
                }
            }
        }

        if (size > 0) {
            String message = Config.messages.getColorfulString("suspicion_notification")
                    .replace("{size}", String.valueOf(size))
                    .replace("{players}", players.substring(0, players.length() - comma.length()));

            if (!staff.isEmpty()) {
                for (SpartanPlayer player : staff) {
                    player.sendMessage(message);
                }
            }
        }
    }

}
