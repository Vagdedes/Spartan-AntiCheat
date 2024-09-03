package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CloudBase {

    // URLs
    static final String website = "aHR0cHM6Ly93d3cudmFnZGVkZXMuY29tL21pbmVjcmFmdC9jbG91ZC8=",
            accountWebsite = "aHR0cHM6Ly93d3cuaWRlYWxpc3RpYy5haS9hcGkvdjEvcHJvZHVjdC92ZXJpZnlEb3dubG9hZC8=";
    public static final String downloadWebsite = "aHR0cHM6Ly93d3cuaWRlYWxpc3RpYy5haS9hcGkvdjEvcHJvZHVjdC9kb3dubG9hZEZpbGUvP3Rva2VuPQ==";

    // Cache
    private static final Map<Enums.HackType, String[]>
            disabledDetections = new LinkedHashMap<>(Enums.HackType.values().length);

    // Functionality
    private static long
            connectionRefreshCooldown = 0L,
            connectionFailedCooldown = 0L;
    private static final long refreshTime = 60_000L;
    private static int outdatedVersion = AutoUpdater.NOT_CHECKED,
            detectionSlots;

    // Parameters
    static String identification = "", token = null;
    static final String version = calculateVersion();
    static final String separator = ">@#&!%<;=";

    static {
        clear(false);

        SpartanBukkit.runRepeatingTask(() -> SpartanBukkit.connectionThread.execute(CloudBase::refresh), 1L, refreshTime);
    }

    // Separator

    private static String calculateVersion() {
        String[] verstionString = Register.plugin.getDescription().getVersion().split(" ");

        for (String s : verstionString) {
            if (AlgebraUtils.validInteger(s) || AlgebraUtils.validDecimal(s)) {
                return s;
            }
        }
        return "0";
    }

    public static boolean hasToken() {
        return token != null && !IDs.hasUserIDByDefault;
    }

    public static String getToken() {
        return IDs.hasUserIDByDefault ? null : token;
    }

    public static String getRawToken() {
        return token;
    }

    // Separator

    public static int getDetectionSlots() {
        return detectionSlots;
    }

    // Separator

    public static String[] getShownDisabledDetections(Enums.HackType check) {
        return disabledDetections.get(check);
    }

    public static boolean isInformationCancelled(Enums.HackType hackType, String info) {
        String[] detections = disabledDetections.get(hackType);

        if (detections != null) {
            for (String detection : detections) {
                if (info.contains(detection)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void announce(SpartanPlayer player) {
        if (Permissions.isStaff(player.getInstance())) {
            SpartanBukkit.connectionThread.execute(() -> {
                String[][] announcements = CloudConnections.getStaffAnnouncements();

                if (announcements.length > 0) {
                    List<SpartanPlayer> players = Permissions.getStaff();

                    if (!players.isEmpty()) {
                        for (String[] announcement : announcements) {
                            if (AwarenessNotifications.canSend(
                                    player.getInstance().getUniqueId(),
                                    "staff-announcement-" + announcement[0],
                                    Integer.parseInt(announcement[2])
                            )) {
                                player.getInstance().sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                            }
                        }
                    }
                }
            });
        }
    }

    // Separator

    public static void throwError(Exception object, String function) {
        long ms = System.currentTimeMillis();

        if (connectionFailedCooldown >= ms) {
            connectionFailedCooldown = ms + refreshTime;

            if (AwarenessNotifications.areEnabled()) {
                AwarenessNotifications.forcefullySend("(" + function + ") Failed to connect to the Game Cloud.");
                AwarenessNotifications.forcefullySend("Error: " + object.getMessage());
                AwarenessNotifications.forcefullySend("In Depth: " + object);
            }
        }
    }

    public static void clear(boolean cache) {
        if (cache) {
            disabledDetections.clear();
        } else {
            identification = "identification=" + IDs.platform() + "|" + IDs.user() + "|" + IDs.file();
        }
    }

    public static void refresh() {
        long ms = System.currentTimeMillis();

        if (connectionRefreshCooldown <= ms) {
            connectionRefreshCooldown = ms + refreshTime;

            // Separator
            SpartanBukkit.connectionThread.executeIfSyncElseHere(SpartanEdition::refresh);

            // Separator
            if (outdatedVersion == AutoUpdater.NOT_CHECKED) {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=outdatedVersionCheck&version=" + version);

                        if (results.length > 0) {
                            String data = results[0];

                            if (data.equals("true")) {
                                String token = getToken();

                                if (token != null) {
                                    if (AutoUpdater.downloadFile(token)) {
                                        outdatedVersion = AutoUpdater.UPDATE_SUCCESS;
                                    } else {
                                        outdatedVersion = AutoUpdater.UPDATE_FAILURE;
                                    }
                                } else {
                                    outdatedVersion = AutoUpdater.OUTDATED;
                                }
                            } else {
                                outdatedVersion = AutoUpdater.NOT_OUTDATED;
                            }
                        }
                    } catch (Exception e) {
                        throwError(e, "outdatedVersionCheck:GET");
                    }
                });
            }

            // Separator
            SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                try {
                    String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                            + "&action=get&data=automaticConfigurationChanges&version=" + version);

                    if (results.length > 0) {
                        boolean changed = false;

                        for (String data : results) {
                            String[] split = data.split("\\|");
                            boolean valueCondition = split.length == 3;

                            if (valueCondition || split.length == 2) {
                                File file = new File(Register.plugin.getDataFolder() + "/" + split[0] + ".yml");

                                if (file.exists()) {
                                    String desiredValue = valueCondition ? split[2] : null;
                                    split = split[1].split(":");

                                    if (split.length == 2) {
                                        String option = split[0];
                                        YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);

                                        for (String key : filea.getKeys(true)) {
                                            if (key.contains(option)) {
                                                String value = split[1];
                                                Object currentValue;

                                                if (!valueCondition // There is no if-value statement
                                                        || (currentValue = filea.get(option)) == null // Current value did not return correctly or is non-existent
                                                        || currentValue.toString().equals(desiredValue)) { // Current value matches desired value
                                                    Double validDecimal = AlgebraUtils.returnValidDecimal(value);
                                                    filea.set(key, validDecimal != null ? validDecimal :
                                                            AlgebraUtils.validInteger(value) ? Integer.parseInt(value) :
                                                                    value.equals("true") || value.equals("false") ? Boolean.parseBoolean(value) :
                                                                            value);
                                                    filea.save(file);
                                                    changed = true;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (changed) {
                            Config.settings.clear();
                            Config.messages.clear();
                            Config.sql.refreshConfiguration();
                            Config.compatibility.clearCache();
                            Config.compatibility.fastRefresh();

                            for (Enums.HackType hackType : Enums.HackType.values()) {
                                hackType.resetCheck();
                            }
                        }
                    }
                } catch (Exception e) {
                    throwError(e, "automaticConfigurationChanges:GET");
                }
            });

            // Separator
            Map<Enums.HackType, String[]> disabledDetections = new LinkedHashMap<>();

            SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                try {
                    String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                            + "&action=get&data=disabledDetections&version=" + version + "&value=" + MultiVersion.versionString());

                    if (results.length > 0) {
                        for (String data : results) {
                            Enums.HackType hackType = null;
                            Set<String> detections = new LinkedHashSet<>();

                            for (String detection : data.split("\\|")) {
                                if (hackType == null) {
                                    for (Enums.HackType original : Enums.HackType.values()) {
                                        if (detection.equalsIgnoreCase(original.toString())) {
                                            hackType = original;
                                            break;
                                        }
                                    }

                                    // Not Found
                                    if (hackType == null) {
                                        break;
                                    }
                                } else {
                                    detections.add(detection.replace("__", " "));
                                }
                            }
                            if (!detections.isEmpty()) {
                                disabledDetections.put(hackType, detections.toArray(new String[0]));
                            }
                        }
                        CloudBase.disabledDetections.clear();
                        CloudBase.disabledDetections.putAll(disabledDetections);
                    } else {
                        CloudBase.disabledDetections.clear();
                    }
                } catch (Exception e) {
                    throwError(e, "disabledDetections:GET");
                }
            });

            // Separator
            SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                List<SpartanPlayer> players = Permissions.getStaff();

                if (!players.isEmpty()) {
                    String[][] announcements = CloudConnections.getStaffAnnouncements();

                    if (announcements.length > 0) {
                        for (String[] announcement : announcements) {
                            for (SpartanPlayer p : players) {
                                if (AwarenessNotifications.canSend(
                                        p.getInstance().getUniqueId(),
                                        "staff-announcement-" + announcement[0],
                                        Integer.parseInt(announcement[2])
                                )) {
                                    p.getInstance().sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                                }
                            }
                        }
                    } else {
                        for (SpartanPlayer p : players) {
                            SpartanEdition.attemptNotifications(p);
                        }
                    }
                }
            });

            // Separator
            SpartanBukkit.connectionThread.executeIfSyncElseHere(
                    () -> detectionSlots = CloudConnections.getDetectionSlots()
            );
        }
    }

}
