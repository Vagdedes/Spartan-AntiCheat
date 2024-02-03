package com.vagdedes.spartan.functionality.synchronicity.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.synchronicity.AutoUpdater;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.connection.Piracy;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CloudFeature {

    // URLs
    static final String website = "aHR0cHM6Ly93d3cudmFnZGVkZXMuY29tL21pbmVjcmFmdC9jbG91ZC8=",
            accountWebsite = "aHR0cHM6Ly93d3cuaWRlYWxpc3RpYy5haS9hcGkvdjEvcHJvZHVjdC92ZXJpZnlEb3dubG9hZC8=";
    public static final String downloadWebsite = "aHR0cHM6Ly93d3cuaWRlYWxpc3RpYy5haS9hcGkvdjEvcHJvZHVjdC9kb3dubG9hZEZpbGUvP3Rva2VuPQ==";

    // Cache
    private static final Map<Enums.HackType, String[]>
            disabledDetections = new LinkedHashMap<>(Enums.HackType.values().length),
            specificDisabledDetections = new LinkedHashMap<>(Enums.HackType.values().length),
            allDisabledDetections = new LinkedHashMap<>(Enums.HackType.values().length);
    static final CopyOnWriteArrayList<UUID> punishedPlayers = new CopyOnWriteArrayList<>(),
            updatedPunishedPlayers = new CopyOnWriteArrayList<>();

    // Exception
    static final int exceptionCooldownMinutes = 5 * 1200;
    static int cloudExceptionCooldown = 0;

    // Frequencies, Cooldowns & Timers
    private static final int connectionRefreshFrequencyInMinutes = 10;
    private static int connectionRefreshTimer = connectionRefreshFrequencyInMinutes * 1200;
    private static long connectionFailedCooldown = 0L;
    private static final long[] connectionRefreshCooldown = new long[2];

    // Features
    static int outdatedVersion = AutoUpdater.NOT_CHECKED;
    static boolean ignoreServerLimits = false, serverLimited = false;

    // Arguments
    static String identification = "", token = null;
    static final double version = Math.abs(Piracy.version);
    public static final String separator = ">@#&!%<;=";

    static {
        clear(false);

        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (cloudExceptionCooldown > 0) {
                    cloudExceptionCooldown -= 1;

                    if (cloudExceptionCooldown == 0) {
                        SpartanBukkit.connectionThread.execute(() -> {
                            refresh(true);
                            refresh(false);
                        });
                    }
                } else if (cloudExceptionCooldown == 0) {
                    if (connectionRefreshTimer == 0) {
                        SpartanBukkit.connectionThread.execute(() -> {
                            refresh(true);
                            refresh(false);
                        });
                    } else {
                        connectionRefreshTimer -= 1;
                    }
                }
            }, 1L, 1L);
        }
    }

    public static String getMaximumServerLimitMessage() {
        return "You have reached your Maximum Server Limit. " +
                "Register your account on §nhttps://www.vagdedes.com/account/profile§r " +
                "§lOR§r join the Discord server " +
                "§lOR§r use the " + IDs.getPlatform(true) + " platform to verify";
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

    public static boolean canSynchroniseFiles(boolean exception) {
        return Config.settings.getBoolean("Cloud.synchronise_files")
                && !IDs.isPreview()
                && (!exception || !hasException());
    }

    // Separator

    public static boolean hasException() {
        return cloudExceptionCooldown != 0;
    }

    public static boolean isServerLimited() { // Once
        return serverLimited;
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

    public static boolean announce(SpartanPlayer player) {
        if (Permissions.isStaff(player)) {
            String[][] announcements = CloudConnections.getStaffAnnouncements();

            if (announcements.length > 0) {
                List<SpartanPlayer> players = Permissions.getStaff();

                if (!players.isEmpty()) {
                    boolean result = false;

                    for (String[] announcement : announcements) {
                        if (AwarenessNotifications.canSend(
                                player.getUniqueId(),
                                "staff-announcement-" + announcement[0],
                                Integer.parseInt(announcement[2])
                        )) {
                            player.sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                            result = true;
                        }
                    }
                    return result;
                }
            }
        }
        return false;
    }

    // Separator

    static boolean recentError(long ms, boolean includePreview) {
        return connectionFailedCooldown >= ms && (includePreview || !IDs.isPreview());
    }

    static boolean recentError(long ms) {
        return recentError(ms, false);
    }

    public static void throwError(Exception object, String function) {
        long ms = System.currentTimeMillis();

        if (!recentError(ms)) {
            connectionFailedCooldown = ms + (connectionRefreshFrequencyInMinutes * 60_000L);

            if (!hasException() && AwarenessNotifications.areEnabled() && SpartanBukkit.isProductionServer()) {
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
            punishedPlayers.clear();
            updatedPunishedPlayers.clear();
        } else {
            identification = "identification=" + IDs.user() + "|" + IDs.nonce();
        }
    }

    public static void refresh(boolean independent) {
        long ms = System.currentTimeMillis();

        if (!recentError(ms) && connectionRefreshCooldown[independent ? 0 : 1] <= ms) {
            connectionRefreshCooldown[independent ? 0 : 1] = ms + 60_000L;
            connectionRefreshTimer = connectionRefreshFrequencyInMinutes * 1200;
            boolean async = !SpartanBukkit.isSynchronised(),
                    validIDs = IDs.isValid();

            // Separator
            if (independent) {
                if (async) {
                    if (validIDs) {
                        SpartanEdition.refresh();
                    }
                } else if (validIDs) {
                    SpartanBukkit.connectionThread.execute(SpartanEdition::refresh);
                }
            } else if (async) {
                if (validIDs) {
                    CloudConnections.punishPlayers();
                }
            } else if (validIDs) {
                SpartanBukkit.connectionThread.execute(CloudConnections::punishPlayers);
            }

            // Separator
            if (independent
                    && validIDs
                    && outdatedVersion == AutoUpdater.NOT_CHECKED) {
                Runnable runnable = () -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=outdatedVersionCheck&version=" + version);

                        if (results.length > 0) {
                            String data = results[0];

                            if (data.equals("exception")) {
                                cloudExceptionCooldown = exceptionCooldownMinutes;
                            } else if (data.equals("true")) {
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
                        throwError(e, "OVC:GET");
                    }
                };

                if (async) {
                    runnable.run();
                } else {
                    SpartanBukkit.connectionThread.execute(runnable);
                }
            }

            // Separator
            if (independent && validIDs && canSynchroniseFiles(true)) {
                Runnable runnable = () -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=automaticConfigurationChanges&version=" + version + "&value=" + Bukkit.getPort());

                        if (results.length > 0) {
                            boolean changed = false;

                            for (String data : results) {
                                if (data.contains("exception")) {
                                    cloudExceptionCooldown = exceptionCooldownMinutes;
                                    break;
                                } else {
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
                            }

                            if (changed) {
                                SpartanMenu.manageConfiguration.reload(true);
                            }
                        }
                    } catch (Exception e) {
                        throwError(e, "ACC:GET");
                    }
                };

                if (async) {
                    runnable.run();
                } else {
                    SpartanBukkit.connectionThread.execute(runnable);
                }
            }

            // Separator
            if (!independent && validIDs) {
                Runnable runnable = () -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=customerSupportCommands&version=" + version + "&value=" + Bukkit.getPort());

                        if (results.length > 0) {
                            if (results[0].equals("exception")) {
                                cloudExceptionCooldown = exceptionCooldownMinutes;
                            } else {
                                for (String result : results) {
                                    String[] split = result.split(separator);

                                    if (split.length == 2) {
                                        // 0 = user, 1 = functionality
                                        String functionality = StringUtils.decodeBase64(split[1]);

                                        if (!functionality.equals("NULL")) {
                                            AwarenessNotifications.forcefullySend("Customer Support Command: " +
                                                    CloudConnections.sendCustomerSupport(null, functionality, "Customer Support Command", true));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throwError(e, "CSC:GET");
                    }
                };

                if (async) {
                    runnable.run();
                } else {
                    SpartanBukkit.connectionThread.execute(runnable);
                }
            }

            // Separator
            if (independent) {
                Map<Enums.HackType, String[]>
                        disabledDetections = new LinkedHashMap<>(),
                        hiddenDisabledDetections = new LinkedHashMap<>(),
                        allDisabledDetections = new LinkedHashMap<>();

                // Doesn't need ID validation due to its unique anti-piracy purpose
                Runnable runnable = () -> {
                    try {
                        String[] results = RequestUtils.get(StringUtils.decodeBase64(website) + "?" + identification
                                + "&action=get&data=disabledDetections&version=" + version + "&value=" + MultiVersion.versionString());

                        if (results.length > 0) {
                            for (String data : results) {
                                if (data.contains("exception")) {
                                    cloudExceptionCooldown = exceptionCooldownMinutes;
                                    break;
                                } else {
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
                            }
                            CloudFeature.disabledDetections.clear();
                            CloudFeature.disabledDetections.putAll(disabledDetections);
                            CloudFeature.specificDisabledDetections.clear();
                            CloudFeature.specificDisabledDetections.putAll(hiddenDisabledDetections);
                            CloudFeature.allDisabledDetections.clear();
                            CloudFeature.allDisabledDetections.putAll(allDisabledDetections);
                        } else {
                            CloudFeature.disabledDetections.clear();
                            CloudFeature.specificDisabledDetections.clear();
                            CloudFeature.allDisabledDetections.clear();
                        }
                    } catch (Exception e) {
                        throwError(e, "DD:GET");
                    }
                };

                if (async) {
                    runnable.run();
                } else {
                    SpartanBukkit.connectionThread.execute(runnable);
                }
            }

            // Separator
            if (independent && validIDs) {
                String[][] announcements = CloudConnections.getStaffAnnouncements();

                if (announcements.length > 0) {
                    List<SpartanPlayer> players = Permissions.getStaff();

                    if (!players.isEmpty()) {
                        for (String[] announcement : announcements) {
                            for (SpartanPlayer p : players) {
                                if (AwarenessNotifications.canSend(
                                        p.getUniqueId(),
                                        "staff-announcement-" + announcement[0],
                                        Integer.parseInt(announcement[2])
                                )) {
                                    p.sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            clear(true);
        }
    }
}
