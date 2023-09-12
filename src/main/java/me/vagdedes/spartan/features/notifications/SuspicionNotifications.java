package me.vagdedes.spartan.features.notifications;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudConnections;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.profiling.PlayerEvidence;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.util.*;

public class SuspicionNotifications {

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(SuspicionNotifications::run, 1L, 300L);
        }
    }

    private static final Map<UUID, Map<UUID, Long>> notified = Cache.store(new LinkedHashMap<>());

    public static void run() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                SpartanBukkit.runTask(p, () -> run(p, true));
            }
        } else {
            run(null, true);
        }
    }

    private static boolean run(SpartanPlayer p, boolean scheduler) {
        boolean send;
        boolean isNull = p == null;

        if (scheduler) {
            send = true;

            if (!isNull) {
                if (!TestServer.isIdentified()
                        && SpartanBukkit.isProductionServer()
                        && ResearchEngine.enoughData()
                        && !Settings.getBoolean("Notifications.individual_only_notifications")) {
                    Integer divisor = DetectionNotifications.getDivisor(p, false);

                    if (divisor == null || divisor != 0) {
                        isNull = true;
                    }
                } else {
                    isNull = true;
                }
            }
        } else {
            send = !Settings.getBoolean("Notifications.individual_only_notifications")
                    && DetectionNotifications.hasPermission(p);
        }

        if (send) {
            UUID staffUUID = isNull ? SpartanBukkit.uuid : p.getUniqueId();
            String comma = ", ";
            StringBuilder players = new StringBuilder();
            int size = 0, commaLength = comma.length();
            long currentTime = System.currentTimeMillis(),
                    extraTime = (Check.violationCycleSeconds * 5);

            for (SpartanPlayer player : SpartanBukkit.getPlayers()) {
                if (player.canRunChecks(false)) {
                    UUID detectedUUID = player.getUniqueId();

                    if (isNull || detectedUUID != staffUUID) {
                        PlayerProfile profile = player.getProfile();
                        boolean shouldList, hacker;

                        if (profile.isHacker()) {
                            Set<Map.Entry<Enums.HackType, Integer>> violationEntries = Check.getViolationsEntry(detectedUUID);

                            if (!violationEntries.isEmpty()) {
                                shouldList = false;
                                PlayerEvidence evidence = profile.getEvidence();

                                for (Map.Entry<Enums.HackType, Integer> violationEntry : violationEntries) {
                                    Enums.HackType hackType = violationEntry.getKey();

                                    if (evidence.has(hackType)
                                            && violationEntry.getValue() >= hackType.getCheck().getDefaultCancelViolation()) {
                                        shouldList = true;
                                        break;
                                    }
                                }
                            } else {
                                shouldList = false;
                            }
                            hacker = true;
                        } else if (profile.isSuspected()) {
                            Set<Map.Entry<Enums.HackType, Integer>> violationEntries = Check.getViolationsEntry(detectedUUID);

                            if (!violationEntries.isEmpty()) {
                                shouldList = false;
                                ResearchEngine.DataType dataType = player.getDataType();
                                PlayerEvidence evidence = profile.getEvidence();

                                for (Map.Entry<Enums.HackType, Integer> violationEntry : violationEntries) {
                                    Enums.HackType hackType = violationEntry.getKey();

                                    if (evidence.has(hackType)
                                            && violationEntry.getValue() >= CancelViolation.get(hackType, dataType)) {
                                        shouldList = true;
                                        break;
                                    }
                                }
                            } else {
                                shouldList = false;
                            }
                            hacker = false;
                        } else {
                            shouldList = false;
                            hacker = false;
                        }

                        if (shouldList) {
                            boolean add = true;
                            Map<UUID, Long> map = notified.get(staffUUID);

                            if (map == null) {
                                map = new LinkedHashMap<>();
                                map.put(detectedUUID, currentTime + extraTime);
                                notified.put(staffUUID, map);
                            } else {
                                Long lastTime = map.get(detectedUUID);

                                if (lastTime == null || lastTime <= currentTime) {
                                    map.put(detectedUUID, currentTime + extraTime);
                                } else {
                                    add = false;
                                }
                            }

                            if (add) {
                                StringBuilder evidence = new StringBuilder();
                                Collection<Enums.HackType> evidenceDetails = profile.getEvidence().getKnowledgeList();

                                if (!evidenceDetails.isEmpty()) {
                                    size++;
                                    players.append(player.getName()).append(comma);

                                    for (Enums.HackType hackType : evidenceDetails) {
                                        evidence.append(hackType.getCheck().getName()).append(comma);
                                    }
                                    evidence = new StringBuilder(evidence.substring(0, evidence.length() - commaLength));
                                    SpartanLocation location = player.getLocation();
                                    CloudConnections.executeDiscordWebhook(
                                            "checks",
                                            detectedUUID, player.getName(),
                                            location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                                            (hacker ? "Hacker" : "Suspected"), evidence.toString()
                                    );
                                }
                            }
                        }
                    }
                }

                // Separator
                if (size > 0) {
                    String message = Messages.get("suspicion_notification")
                            .replace("{size}", String.valueOf(size))
                            .replace("{players}", players.substring(0, players.length() - comma.length()));

                    if (!isNull) {
                        p.sendMessage(message);
                    }
                    CrossServerInformation.queueNotification(message, true);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean run(SpartanPlayer p) {
        return run(p, false);
    }
}
