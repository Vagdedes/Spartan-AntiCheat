package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.List;

public class SpartanEdition {

    private static final Enums.DataType dataType = Enums.DataType.JAVA,
            oppositeType = dataType == Enums.DataType.JAVA ? Enums.DataType.BEDROCK : Enums.DataType.JAVA;

    private static long checkTime = 0L;
    public static final String[] jarNames = new String[]{
            StringUtils.decodeBase64("U3BhcnRhbkJlZHJvY2tFZGl0aW9u"),
            "Spartan",
    };
    public static final String patreonURL = "https://www.idealistic.ai/patreon";
    private static final int notificationCooldown = 60 * 60;
    private static final String
            type = "{type}",
            product = "{product}",
            versionNotificationMessage = "\n§cHey, just a heads up! You have " + type + " players which cannot be checked by the anti-cheat due to missing " + type + " detections."
                    + (SpartanBukkit.canAdvertise ? "\nClick §n" + product + " §rto §lfix this§r." : ""),
            limitNotificationMessage = "\nHey, just a heads up! You have more online players than the anti-cheat can check at once."
                    + "\nClick §n" + patreonURL + "§r to learn how §lDetection Slots §rwork.";
    private static boolean
            notifyCache = false,
            alternativeVersion = false;

    // Verification

    public static void refresh() {
        SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
            if (!alternativeVersion) {
                if (CloudConnections.ownsProduct(
                        getProductID(oppositeType)
                )) {
                    alternativeVersion = true;
                    Config.messages.clear();

                    for (Enums.HackType hackType : Enums.HackType.values()) {
                        hackType.resetCheck();
                    }
                }
            }
        });
    }

    // Detections

    public static boolean hasDetectionsPurchased(Enums.DataType dataType) {
        return SpartanEdition.dataType == dataType || alternativeVersion;
    }

    public static Enums.DataType getMissingDetection() {
        return alternativeVersion ? null : oppositeType;
    }

    // Product

    private static String getProductID(Enums.DataType dataType) {
        switch (dataType) {
            case BEDROCK:
                return "16";
            default:
                return "1";
        }
    }

    public static String getProductID() {
        return getProductID(dataType);
    }

    public static String getProductName() {
        if (alternativeVersion) {
            return "Spartan AntiCheat: Java & Bedrock Edition";
        } else {
            return "Spartan AntiCheat: " + dataType + " Edition";
        }
    }

    // Notifications

    public static boolean attemptNotification(SpartanPlayer player) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() <= CloudBase.getDetectionSlots()
                || !sendLimitNotification(player)) {
            Enums.DataType missingDetection = getMissingDetection();

            if (missingDetection != null) {
                if (notifyCache) {
                    sendVersionNotification(player, missingDetection);
                } else {
                    long time = System.currentTimeMillis();

                    if ((time - checkTime) >= 60_000L) {
                        checkTime = time;
                        boolean bedrockIsMissingDetection = missingDetection == Enums.DataType.BEDROCK;

                        if (bedrockIsMissingDetection == player.bedrockPlayer) {
                            notifyCache = true;
                            sendVersionNotification(player, missingDetection);
                        } else {
                            players.remove(player);
                            int size = players.size();
                            List<PlayerProfile> checkedProfiles;

                            if (size > 0) {
                                checkedProfiles = new ArrayList<>(size);

                                for (SpartanPlayer otherPlayer : players) {
                                    if (bedrockIsMissingDetection == otherPlayer.bedrockPlayer) {
                                        notifyCache = true;
                                        sendVersionNotification(player, missingDetection);
                                        return true;
                                    } else {
                                        checkedProfiles.add(player.getProfile());
                                    }
                                }
                            } else {
                                checkedProfiles = new ArrayList<>(0);
                            }

                            // Separator

                            List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

                            if (!playerProfiles.isEmpty()) {
                                playerProfiles.remove(player.getProfile());
                                playerProfiles.removeAll(checkedProfiles);

                                if (!playerProfiles.isEmpty()) {
                                    for (PlayerProfile profile : playerProfiles) {
                                        if (bedrockIsMissingDetection == profile.isBedrockPlayer()) {
                                            notifyCache = true;
                                            sendVersionNotification(player, missingDetection);
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void sendVersionNotification(SpartanPlayer player, Enums.DataType dataType) {
        if (Permissions.isStaff(player)) {
            String message = AwarenessNotifications.getNotification(
                    versionNotificationMessage
                            .replace(type, dataType.toString())
                            .replace(
                                    product,
                                    IDs.isBuiltByBit()
                                            ? (dataType == Enums.DataType.JAVA
                                            ? "https://builtbybit.com/resources/12832"
                                            : "https://builtbybit.com/resources/11196")
                                            : (dataType == Enums.DataType.JAVA
                                            ? "https://polymart.org/resource/3600"
                                            : "https://polymart.org/resource/350")
                            )
            );

            if (AwarenessNotifications.canSend(player.uuid, "alternative-version", notificationCooldown)) {
                player.sendImportantMessage(message);
            }
        }
    }

    private static boolean sendLimitNotification(SpartanPlayer player) {
        if (Permissions.isStaff(player)) {
            String message = AwarenessNotifications.getNotification(limitNotificationMessage);

            if (AwarenessNotifications.canSend(player.uuid, "limit-notification", notificationCooldown)) {
                player.sendImportantMessage(message);
                return true;
            }
        }
        return false;
    }

}
