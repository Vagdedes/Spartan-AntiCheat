package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.connection.DiscordMemberCount;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SpartanEdition {

    private static final Check.DataType
            currentType = Check.DataType.JAVA,
            oppositeType = currentType == Check.DataType.JAVA
                    ? Check.DataType.BEDROCK
                    : Check.DataType.JAVA;

    private static long checkTime = 0L;
    public static final String[] jarNames = new String[]{
            StringUtils.decodeBase64("U3BhcnRhbkJlZHJvY2tFZGl0aW9u"),
            "Spartan",
    };
    public static final String patreonURL = "https://www.vagdedes.com/patreon";
    private static final int notificationCooldown = 60 * 60;
    private static final String
            type = "{type}",
            product = "{product}",
            versionNotificationMessage = "\n§cHey, just a heads up! You have " + type + " players which cannot be checked by the anti-cheat due to missing " + type + " detections.",
            limitNotificationMessage = "\n§cHey, just a heads up! You have more online players than the anti-cheat can check at once."
                    + "\nClick §n" + patreonURL + "§r§c to learn how §lDetection Slots §r§cwork.";
    private static boolean
            firstLoad = true,
            notifyCache = false,
            currentVersion = true,
            alternativeVersion = false;

    // Verification

    public static void refresh() {
        if (firstLoad) {
            firstLoad = false;
            currentVersion = !CloudBase.hasToken();
        }
        SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
            if (!currentVersion
                    && CloudConnections.ownsProduct(
                    getProductID(currentType)
            )) {
                currentVersion = true;
            }
            if (!alternativeVersion
                    && CloudConnections.ownsProduct(
                    getProductID(oppositeType)
            )) {
                alternativeVersion = true;
            }
        });
    }

    // Detections

    public static boolean hasDetectionsPurchased(Check.DataType dataType) {
        return SpartanEdition.currentType == dataType
                ? currentVersion
                : alternativeVersion;
    }

    private static Check.DataType[] getMissingDetections() {
        return !currentVersion && !alternativeVersion
                ? new Check.DataType[]{currentType, oppositeType}
                : !currentVersion
                ? new Check.DataType[]{currentType}
                : !alternativeVersion
                ? new Check.DataType[]{oppositeType}
                : new Check.DataType[]{};
    }

    // Product

    private static String getProductID(Check.DataType dataType) {
        switch (dataType) {
            case JAVA:
                return "1";
            case BEDROCK:
                return "16";
            default:
                return "27";
        }
    }

    public static String getProductID() {
        return getProductID(
                currentVersion
                        ? currentType
                        : (alternativeVersion ? oppositeType : Check.DataType.UNIVERSAL)
        );
    }

    public static String getProductName() {
        if (currentVersion && alternativeVersion) {
            return "Spartan AntiCheat: Java & Bedrock Edition";
        } else if (currentVersion) {
            return "Spartan AntiCheat: Java Edition";
        } else {
            return "Spartan AntiCheat: Bedrock Edition";
        }
    }

    // Notifications

    public static void attemptNotification(SpartanPlayer player) {
        Check.DataType[] missingDetections = getMissingDetections();

        if (missingDetections.length == ResearchEngine.usableDataTypes.length) {
            sendVersionNotification(player, null);
        } else {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();
            int detectionSlots = CloudBase.getDetectionSlots();

            if (missingDetections.length > 0
                    && (detectionSlots <= 0
                    || players.size() <= detectionSlots
                    || !sendLimitNotification(player))) {
                if (notifyCache) {
                    sendVersionNotification(player, missingDetections[0]);
                } else {
                    long time = System.currentTimeMillis();

                    if ((time - checkTime) >= 60_000L) {
                        checkTime = time;

                        if (missingDetections[0] == player.dataType) {
                            notifyCache = true;
                            sendVersionNotification(player, missingDetections[0]);
                        } else {
                            players.remove(player);
                            int size = players.size();
                            List<PlayerProfile> checkedProfiles;

                            if (size > 0) {
                                checkedProfiles = new ArrayList<>(size);

                                for (SpartanPlayer otherPlayer : players) {
                                    if (missingDetections[0] == otherPlayer.dataType) {
                                        notifyCache = true;
                                        sendVersionNotification(player, missingDetections[0]);
                                        return;
                                    } else {
                                        checkedProfiles.add(player.protocol.getProfile());
                                    }
                                }
                            } else {
                                checkedProfiles = new ArrayList<>(0);
                            }

                            // Separator

                            List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

                            if (!playerProfiles.isEmpty()) {
                                playerProfiles.remove(player.protocol.getProfile());
                                playerProfiles.removeAll(checkedProfiles);

                                if (!playerProfiles.isEmpty()) {
                                    for (PlayerProfile profile : playerProfiles) {
                                        if (profile.hasData(missingDetections[0])) {
                                            notifyCache = true;
                                            sendVersionNotification(player, missingDetections[0]);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void sendVersionNotification(SpartanPlayer player, Check.DataType dataType) {
        if (Permissions.isStaff(player)) {
            String message;

            if (dataType == null) {
                message = "\n§cHey, just a heads up!"
                        + " Your owned editions of Spartan could not be verified."
                        + " Visit §n" + DiscordMemberCount.discordURL + "§r§c to fix this.";
            } else {
                message = AwarenessNotifications.getNotification(
                        (versionNotificationMessage
                                + (IDs.canAdvertise() ? "\nClick §n" + product + "§r§c to §lfix this§r§c." : ""))
                                .replace(type, dataType.toString())
                                .replace(
                                        product,
                                        IDs.isBuiltByBit()
                                                ? (dataType == Check.DataType.JAVA
                                                ? "https://builtbybit.com/resources/12832"
                                                : "https://builtbybit.com/resources/11196")
                                                : (dataType == Check.DataType.JAVA
                                                ? "https://polymart.org/resource/3600"
                                                : "https://polymart.org/resource/350")
                                )
                );
            }

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
