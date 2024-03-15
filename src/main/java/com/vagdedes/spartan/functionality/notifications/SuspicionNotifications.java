package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TestServer;
import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SuspicionNotifications {

    public static void run() {
        SpartanBukkit.runRepeatingTask(() -> { // Here because there are no other class calls
            if (!TestServer.isIdentified()
                    && ResearchEngine.enoughData()
                    && !Config.settings.getBoolean("Notifications.individual_only_notifications")) {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();
                Iterator<SpartanPlayer> iterator = players.iterator();

                while (iterator.hasNext()) {
                    Integer divisor = DetectionNotifications.getDivisor(iterator.next(), false);

                    if (divisor == null || divisor != 0) {
                        iterator.remove();
                    }
                }
                run(players);
            }
        }, 1L, 300L);
    }

    private static void run(List<SpartanPlayer> playersList) {
        String comma = ", ";
        StringBuilder players = new StringBuilder();
        int size = 0, commaLength = comma.length();

        for (SpartanPlayer player : SpartanBukkit.getPlayers()) {
            if (player.canRunChecks(false)) {
                PlayerProfile profile = player.getProfile();
                boolean shouldList;

                if (profile.isHacker()) {
                    shouldList = true;
                } else if (profile.isSuspected()) {
                    shouldList = false;

                    for (Enums.HackType hackType : profile.evidence.getKnowledgeList()) {
                        if (player.getViolations(hackType).hasLevel()) {
                            shouldList = true;
                            break;
                        }
                    }
                } else {
                    shouldList = false;
                }

                if (shouldList) {
                    StringBuilder evidence = new StringBuilder();
                    Collection<Enums.HackType> evidenceDetails = profile.evidence.getKnowledgeList();

                    if (!evidenceDetails.isEmpty()) {
                        size++;
                        players.append(player.name).append(comma);

                        for (Enums.HackType hackType : evidenceDetails) {
                            evidence.append(hackType.getCheck().getName()).append(comma);
                        }
                        evidence = new StringBuilder(evidence.substring(0, evidence.length() - commaLength));
                        SpartanLocation location = player.getLocation();
                        CloudConnections.executeDiscordWebhook(
                                "checks",
                                player.uuid, player.name,
                                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                                (profile.isHacker() ? "Hacker" : "Suspected"), evidence.toString()
                        );
                    }
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
