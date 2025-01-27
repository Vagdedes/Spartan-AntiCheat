package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.moderation.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CloudBase {

    // Cache
    private static final Map<Enums.HackType, String[]>
            disabledDetections = new ConcurrentHashMap<>(Enums.HackType.values().length);

    // Functionality
    private static long
            connectionRefreshCooldown = 0L,
            connectionFailedCooldown = 0L;
    private static final long refreshTime = 60_000L;

    // Parameters
    static final String separator = ">@#&!%<;=";

    static {
        PluginBase.runRepeatingTask(
                () -> PluginBase.connectionThread.execute(CloudBase::refresh),
                1L,
                refreshTime
        );
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

    public static void announce(PlayerProtocol protocol) {
        if (Permissions.isStaff(protocol.bukkit())) {
            PluginBase.connectionThread.execute(() -> {
                String[][] announcements = CloudConnections.getStaffAnnouncements();

                if (announcements.length > 0) {
                    for (String[] announcement : announcements) {
                        if (AwarenessNotifications.canSend(
                                protocol.getUUID(),
                                "staff-announcement-" + announcement[0],
                                Integer.parseInt(announcement[2])
                        )) {
                            protocol.bukkit().sendMessage(AwarenessNotifications.getNotification(announcement[1]));
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
            String message = "(" + function + ") Failed to connect to the Game Cloud."
                    + "\nError: " + object.getMessage()
                    + "\nIn Depth: " + object;
            AwarenessNotifications.optionallySend(message);
        }
    }

    public static void clear() {
        disabledDetections.clear();
    }

    public static String identification() {
        return "identification=" + IDs.platform() + "|" + IDs.user() + "|" + IDs.file();
    }

    public static void refresh() {
        long ms = System.currentTimeMillis();

        if (connectionRefreshCooldown <= ms) {
            connectionRefreshCooldown = ms + refreshTime;

            // Separator
            PluginBase.connectionThread.executeIfUnknownThreadElseHere(SpartanEdition::refresh);

            // Separator
            PluginBase.connectionThread.executeIfUnknownThreadElseHere(() -> {
                try {
                    String[] results = RequestUtils.get(StringUtils.decodeBase64(JarVerification.website) + "?" + identification()
                            + "&action=get&data=automaticConfigurationChanges&version=" + Register.plugin.getDescription().getVersion());

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
                            for (ConfigurationBuilder configuration : Config.configurations) {
                                configuration.clear();
                            }
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

            PluginBase.connectionThread.executeIfUnknownThreadElseHere(() -> {
                try {
                    String[] results = RequestUtils.get(StringUtils.decodeBase64(JarVerification.website) + "?" + identification()
                            + "&action=get&data=disabledDetections&version=" + Register.plugin.getDescription().getVersion() + "&value="
                            + MultiVersion.serverVersion.toString());

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
            PluginBase.connectionThread.executeIfUnknownThreadElseHere(() -> {
                List<PlayerProtocol> protocols = Permissions.getStaff();

                if (!protocols.isEmpty()) {
                    String[][] announcements = CloudConnections.getStaffAnnouncements();

                    if (announcements.length > 0) {
                        for (String[] announcement : announcements) {
                            for (PlayerProtocol p : protocols) {
                                if (AwarenessNotifications.canSend(
                                        p.getUUID(),
                                        "staff-announcement-" + announcement[0],
                                        Integer.parseInt(announcement[2])
                                )) {
                                    p.bukkit().sendMessage(AwarenessNotifications.getNotification(announcement[1]));
                                }
                            }
                        }
                    } else {
                        for (PlayerProtocol p : protocols) {
                            SpartanEdition.attemptNotifications(p);
                        }
                    }
                }
            });

        }
    }

}
