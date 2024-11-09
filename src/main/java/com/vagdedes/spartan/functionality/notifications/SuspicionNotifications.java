package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
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
                List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

                if (!protocols.isEmpty()) {
                    List<SpartanProtocol> staff = new ArrayList<>(protocols);
                    Iterator<SpartanProtocol> iterator = staff.iterator();

                    while (iterator.hasNext()) {
                        Integer frequency = DetectionNotifications.getFrequency(iterator.next());

                        if (frequency == null || frequency < 100) {
                            iterator.remove();
                        }
                    }
                    run(staff, protocols);
                }
            }
        }, 1L, 300L);
    }

    private static void run(List<SpartanProtocol> staff, List<SpartanProtocol> online) {
        StringBuilder players = new StringBuilder();
        int size = 0, commaLength = comma.length();

        for (SpartanProtocol protocol : online) {
            Collection<Enums.HackType> list = protocol.getProfile().getEvidenceList(
                    PlayerEvidence.notificationProbability
            );

            if (!list.isEmpty()) {
                StringBuilder evidence = new StringBuilder();

                for (Enums.HackType hackType : list) {
                    evidence.append(
                            hackType.getCheck().getName()
                                    + (protocol.spartan.getExecutor(hackType).hasSufficientData(protocol.spartan.dataType)
                                    ? ""
                                    : " (Unlikely)")
                    ).append(
                            comma
                    );
                }

                if (evidence.length() > 0) {
                    size++;
                    players.append(protocol.bukkit.getName()).append(comma);
                    SpartanLocation location = protocol.spartan.movement.getLocation();
                    CloudConnections.executeDiscordWebhook(
                            "checks",
                            protocol.getUUID(),
                            protocol.bukkit.getName(),
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ(),
                            "Suspected for",
                            evidence.substring(0, evidence.length() - commaLength)
                    );
                }
            }
        }

        if (size > 0) {
            String message = Config.messages.getColorfulString("suspicion_notification")
                    .replace("{size}", String.valueOf(size))
                    .replace("{players}", players.substring(0, players.length() - comma.length()));

            if (!staff.isEmpty()) {
                for (SpartanProtocol protocol : staff) {
                    protocol.bukkit.sendMessage(message);
                }
            }
        }
    }

}
