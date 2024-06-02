package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CrossServerInformation {


    private static int ticks = 0;
    private static final char colorSyntaxCharacter = '&';
    private static String serverName = null;
    private static final List<String>
            notifications = Collections.synchronizedList(new ArrayList<>()),
            configurations = Collections.synchronizedList(new ArrayList<>());
    private static final Runnable generalTask = () -> {
        if (Config.settings.getBoolean(Settings.cloudSynchroniseFilesOption)) {
            String serverName = getOptionValue();

            if (isOptionValid(serverName)) {
                String type = "configuration";

                if (!configurations.isEmpty()) {
                    String[] configurationsArray;

                    synchronized (configurations) {
                        configurationsArray = configurations.toArray(new String[0]);
                        configurations.clear();
                    }
                    CloudConnections.sendCrossServerInformation(type, serverName, configurationsArray);
                }

                String[] incomingInformation = CloudConnections.getCrossServerInformation(type, serverName);

                if (incomingInformation.length > 0) {
                    for (String information : incomingInformation) {
                        String[] split = information.split(CloudBase.separator, 4);

                        if (split.length == 3) {
                            File file = new File(split[0]);

                            if (file.exists()) {
                                String key = split[1];

                                if (!key.equals(Settings.cloudServerNameOption)
                                        && ConfigUtils.has(file, key)) {
                                    String value = split[2];

                                    if (value.equals("NULL")) {
                                        ConfigUtils.set(file, key, null);
                                    } else if (value.equals("true") || value.equals("false")) {
                                        ConfigUtils.set(file, key, Boolean.parseBoolean(value));
                                    } else {
                                        Double decimal = AlgebraUtils.returnValidDecimal(value);

                                        if (decimal != null) {
                                            ConfigUtils.set(file, key, decimal);
                                        } else if (AlgebraUtils.validInteger(value)) {
                                            ConfigUtils.set(file, key, Integer.parseInt(value));
                                        } else {
                                            ConfigUtils.set(file, key, value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Config.refreshFields(false);
                    Config.createConfigurations(true);
                }
            }
        } else {
            synchronized (configurations) {
                configurations.clear();
            }
        }
    };
    private static final Runnable notificationsTask = () -> {
        String type = "notification";

        if (!notifications.isEmpty()) {
            String[] notificationsArray;

            synchronized (notifications) {
                notificationsArray = notifications.toArray(new String[0]);
                notifications.clear();
            }
            CloudConnections.sendCrossServerInformation(type, serverName, notificationsArray);
        }
        List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(true);

        if (!notificationPlayers.isEmpty()) {
            String[] incomingInformation = CloudConnections.getCrossServerInformation(type, null);

            if (incomingInformation.length > 0) {
                for (String information : incomingInformation) {
                    String[] split = information.split(CloudBase.separator, 3);

                    if (split.length == 2) {
                        String notification = "§3(" + split[0] + ")§f " + split[1];

                        for (SpartanPlayer p : notificationPlayers) {
                            p.sendMessage(ChatColor.translateAlternateColorCodes(colorSyntaxCharacter, notification));
                        }
                    }
                }
            }
        }
    };

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            if (ticks == 0) {
                ticks = 1200;

                SpartanBukkit.connectionThread.execute(() -> {
                    generalTask.run();
                    notificationsTask.run();
                });
            } else {
                ticks -= 1;

                if (ticks % 200 == 0) {
                    SpartanBukkit.connectionThread.execute(notificationsTask::run);
                }
            }
        }, 1L, 1L);
    }

    public static void refresh() {
        serverName = null;
    }

    public static void clear() {
        synchronized (notifications) {
            synchronized (configurations) {
                notifications.clear();
                configurations.clear();
            }
        }
    }

    public static boolean queueNotification(String string, boolean absent) {
        string = string.replace(ChatColor.COLOR_CHAR, colorSyntaxCharacter);

        synchronized (notifications) {
            return absent
                    ? !notifications.contains(string) && notifications.add(string)
                    : notifications.add(string);
        }
    }

    public static boolean isOptionValid(String option) {
        return !option.isEmpty()
                && !option.equals("specify server name")
                && !option.equals("false");
    }

    public static String getOptionValue() {
        if (serverName != null) {
            return serverName;
        }
        String option = Config.settings.getString(Settings.cloudServerNameOption);
        int length = option.length();

        if (length > 0) {
            if (length > 32) {
                option = option.substring(0, 32);
            }
            return serverName = StringUtils.getClearColorString(option);
        }
        return serverName = "";
    }

    // Separator

    public static void sendConfiguration(File file) {
        if (isOptionValid(getOptionValue())
                && Config.settings.getBoolean(Settings.cloudSynchroniseFilesOption)
                && file.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = configuration.getKeys(true);
            int size = keys.size();

            if (size > 0) {
                List<String> list = new ArrayList<>(size);

                for (String key : keys) {
                    Object value = configuration.get(key);
                    list.add(file.getPath() + CloudBase.separator + key + CloudBase.separator + (value == null ? "NULL" : value.toString()));
                }
                synchronized (configurations) {
                    configurations.addAll(list);
                }
            }
        }
    }
}
