package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;

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
            PlayerProfile profile = protocol.profile();
            Collection<Enums.HackType> list = profile.getEvidenceList(
                    PlayerEvidence.notificationProbability
            );

            if (!list.isEmpty()) {
                StringBuilder evidence = new StringBuilder();

                for (Enums.HackType hackType : list) {
                    CheckRunner runner = protocol.spartan.getRunner(hackType);
                    boolean sufficientData = runner.hasSufficientData(
                            protocol.spartan.dataType,
                            PlayerEvidence.dataRatio
                    );
                    long remainingTime = sufficientData
                            ? 0L
                            : profile.getRunner(hackType).getRemainingCompletionTime(profile.getLastDataType());
                    evidence.append(
                            hackType.getCheck().getName()
                                    + (sufficientData
                                    ? " (" + AlgebraUtils.integerRound(
                                    PlayerEvidence.probabilityToCertainty(
                                            runner.getExtremeProbability(protocol.spartan.dataType)
                                    ) * 100.0) + "%)"
                                    : remainingTime > 0L
                                    ? " (Data pending: " + TimeUtils.convertMilliseconds(remainingTime) + ")"
                                    : " [Unlikely (Data pending)]")
                    ).append(
                            comma
                    );
                }

                if (evidence.length() > 0) {
                    size++;
                    players.append(protocol.bukkit.getName()).append(comma);
                    Location location = protocol.getLocation();
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
