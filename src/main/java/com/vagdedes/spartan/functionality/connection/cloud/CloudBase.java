package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
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
            disabledDetections = new LinkedHashMap<>(Enums.HackType.values().length),
            specificDisabledDetections = new LinkedHashMap<>(Enums.HackType.values().length),
            allDisabledDetections = new LinkedHashMap<>(Enums.HackType.values().length);

    // Functionality
    private static long
            connectionFailedCooldown = 0L,
            refreshTime = 60_000L;
    private static final long[] connectionRefreshCooldown = new long[2];
    private static int outdatedVersion = AutoUpdater.NOT_CHECKED,
            detectionSlots;

    // Parameters
    static String identification = "", token = null;
    static final double version = Math.abs(JarVerification.version);
    public static final String separator = ">@#&!%<;=";

    static {
        clear(false);

        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> SpartanBukkit.connectionThread.execute(() -> {
                refresh(true);
                refresh(false);
            }), 1L, refreshTime);
        }
    }

    // Separator

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

    private static boolean isInformationCancelled(String[] disabledDetections, String info) {
        if (disabledDetections != null) {
            for (String detection : disabledDetections) {
                if (info.contains(detection)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInformationCancelled(Enums.HackType hackType, String info) {
        return isInformationCancelled(specificDisabledDetections.get(hackType), info);
    }

    public static boolean isPublicInformationCancelled(Enums.HackType hackType, String info) {
        return isInformationCancelled(allDisabledDetections.get(hackType), info);
    }

    public static void announce(SpartanPlayer player) {
        if (Permissions.isStaff(player)) {
            Runnable runnable = () -> {
                String[][] announcements = CloudConnections.getStaffAnnouncements();

                if (announcements.length > 0) {
                    List<SpartanPlayer> players = Permissions.getStaff();

                    if (!players.isEmpty()) {
                        for (String[] announcement : announcements) {
                            if (AwarenessNotifications.canSend(
                                    player.uuid,
                                    "staff-announcement-" + announcement[0],
                                    Integer.parseInt(announcement[2])
                            )) {
                                player.sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                            }
                        }
                    }
                }
            };

            if (SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.connectionThread.execute(runnable);
            }
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
            specificDisabledDetections.clear();
            allDisabledDetections.clear();
        } else {
            identification = "identification=" + IDs.user() + "|" + IDs.nonce();
        }
    }

    public static void refresh(boolean independent) {
        long ms = System.currentTimeMillis();

        if (connectionRefreshCooldown[independent ? 0 : 1] <= ms) {
            connectionRefreshCooldown[independent ? 0 : 1] = ms + refreshTime;

            // Separator
            if (independent) {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(SpartanEdition::refresh);
            } else {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(CloudConnections::punishPlayers);
            }

            // Separator
            if (independent
                    && outdatedVersion == AutoUpdater.NOT_CHECKED) {
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
            if (independent) {
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
                                Config.compatibility.clear();
                                Config.refreshFields(true);
                            }
                        }
                    } catch (Exception e) {
                        throwError(e, "automaticConfigurationChanges:GET");
                    }
                });
            }

            // Separator
            if (!independent) {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=customerSupportCommands&version=" + version + "&value=" + Bukkit.getPort());

                        if (results.length > 0) {
                            for (String result : results) {
                                String[] split = result.split(separator);

                                if (split.length == 2) {
                                    // 0 = user, 1 = functionality
                                    String functionality = StringUtils.decodeBase64(split[1]);

                                    if (!functionality.equals("NULL")) {
                                        AwarenessNotifications.forcefullySend("Customer Support Command: " +
                                                CloudConnections.sendCustomerSupport(null, functionality, "Customer Support Command"));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throwError(e, "customerSupportCommand:GET");
                    }
                });
            }

            // Separator
            if (independent) {
                Map<Enums.HackType, String[]>
                        disabledDetections = new LinkedHashMap<>(),
                        hiddenDisabledDetections = new LinkedHashMap<>(),
                        allDisabledDetections = new LinkedHashMap<>();

                SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=disabledDetections&version=" + version + "&value=" + MultiVersion.versionString());

                        if (results.length > 0) {
                            for (String data : results) {
                                Enums.HackType hackType = null;
                                Set<String>
                                        detections = new LinkedHashSet<>(),
                                        hiddenDetections = new LinkedHashSet<>(),
                                        allDetections = new LinkedHashSet<>();

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
                                        detection = detection.replace("__", " ");

                                        if (detection.startsWith(" ")) {
                                            hiddenDetections.add(detection);
                                        } else {
                                            detections.add(detection);
                                        }
                                        allDetections.add(detection);
                                    }
                                }
                                if (!detections.isEmpty()) {
                                    disabledDetections.put(hackType, detections.toArray(new String[0]));
                                }
                                if (!hiddenDetections.isEmpty()) {
                                    hiddenDisabledDetections.put(hackType, hiddenDetections.toArray(new String[0]));
                                }
                                if (!allDetections.isEmpty()) {
                                    allDisabledDetections.put(hackType, allDetections.toArray(new String[0]));
                                }
                            }
                            CloudBase.disabledDetections.clear();
                            CloudBase.disabledDetections.putAll(disabledDetections);
                            CloudBase.specificDisabledDetections.clear();
                            CloudBase.specificDisabledDetections.putAll(hiddenDisabledDetections);
                            CloudBase.allDisabledDetections.clear();
                            CloudBase.allDisabledDetections.putAll(allDisabledDetections);
                        } else {
                            CloudBase.disabledDetections.clear();
                            CloudBase.specificDisabledDetections.clear();
                            CloudBase.allDisabledDetections.clear();
                        }
                    } catch (Exception e) {
                        throwError(e, "disabledDetections:GET");
                    }
                });
            }

            // Separator
            if (independent) {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                    String[][] announcements = CloudConnections.getStaffAnnouncements();

                    if (announcements.length > 0) {
                        List<SpartanPlayer> players = Permissions.getStaff();

                        if (!players.isEmpty()) {
                            for (String[] announcement : announcements) {
                                for (SpartanPlayer p : players) {
                                    if (AwarenessNotifications.canSend(
                                            p.uuid,
                                            "staff-announcement-" + announcement[0],
                                            Integer.parseInt(announcement[2])
                                    )) {
                                        p.sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                                    }
                                }
                            }
                        }
                    }
                });
            }

            // Separator
            if (independent) {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(
                        () -> detectionSlots = CloudConnections.getDetectionSlots()
                );
            }
        } else {
            clear(true);
        }
    }
}
