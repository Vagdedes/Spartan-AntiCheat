package me.vagdedes.spartan.features.synchronicity;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.notifications.DetectionNotifications;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudConnections;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.IDs;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class CrossServerInformation {


    private static int max = 200, ticks = 0, importantTicks = 0;
    private static final char colorSyntaxCharacter = '&';
    private static long cooldown = 0L;
    private static String serverName = null;
    private static final String[] options = new String[]{
            "Notifications.cross_server_notifications",
            "Important.server_name",
            "Cloud.server_name"
    };

    private static final CopyOnWriteArrayList<String>
            notifications = new CopyOnWriteArrayList<>(),
            logs = new CopyOnWriteArrayList<>(),
            configurations = new CopyOnWriteArrayList<>();

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (IDs.isValid()) {
                    if (ticks == 0) {
                        ticks = max;

                        SpartanBukkit.connectionThread.execute(() -> {
                            String serverName = getOptionValue();

                            if (isOptionValid(serverName)) {
                                boolean isImportant = importantTicks >= 1200,
                                        synchroniseFiles = CloudFeature.canSynchroniseFiles(true);

                                if (isImportant) {
                                    importantTicks = 0;
                                }
                                String type = "configuration";

                                if (synchroniseFiles) {
                                    if (isImportant) {
                                        if (configurations.size() > 0) {
                                            String[] configurationsArray = configurations.toArray(new String[0]);
                                            configurations.clear();

                                            if (!CloudConnections.sendCrossServerInformation(type, serverName, configurationsArray)) {
                                                return;
                                            }
                                        }

                                        String[] incomingInformation = CloudConnections.getCrossServerInformation(type, serverName);

                                        if (incomingInformation != null) {
                                            if (incomingInformation.length > 0) {
                                                for (String information : incomingInformation) {
                                                    String[] split = information.split(CloudFeature.separator, 4);

                                                    if (split.length == 3) {
                                                        File file = new File(split[0]);

                                                        if (file.exists()) {
                                                            String key = split[1];

                                                            if (!key.equals(getOption())) {
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
                                                Config.refreshVariables(false);
                                                Config.createConfigurations(true);
                                            }
                                        } else {
                                            configurations.clear();
                                            return;
                                        }
                                    }
                                } else {
                                    configurations.clear();
                                }

                                // Separator
                                type = "notification";

                                if (notifications.size() > 0) {
                                    String[] notificationsArray = notifications.toArray(new String[0]);
                                    notifications.clear();

                                    if (!CloudConnections.sendCrossServerInformation(type, serverName, notificationsArray)) {
                                        return;
                                    }
                                }
                                String[] incomingInformation = CloudConnections.getCrossServerInformation(type, null);

                                if (incomingInformation != null) {
                                    List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(true);

                                    if (notificationPlayers.size() > 0 && incomingInformation.length > 0) {
                                        for (String information : incomingInformation) {
                                            String[] split = information.split(CloudFeature.separator, 3);

                                            if (split.length == 2) {
                                                String notification = "§3(" + split[0] + ")§f " + split[1];

                                                for (SpartanPlayer p : notificationPlayers) {
                                                    p.sendMessage(ChatColor.translateAlternateColorCodes(colorSyntaxCharacter, notification));
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    return;
                                }

                                // Separator
                                if (isImportant && synchroniseFiles) {
                                    type = "log";

                                    if (logs.size() > 0) {
                                        String[] logsArray = logs.toArray(new String[0]);
                                        logs.clear();

                                        if (!CloudConnections.sendCrossServerInformation(type, serverName, logsArray)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        ticks -= 1;
                        importantTicks += 1;
                    }
                } else {
                    ticks = max;
                    importantTicks = 0;
                    clear();
                }
            }, 1L, 1L);
        }
    }

    // Separator

    public static void refresh() {
        serverName = null;
        ticks = 50;
    }

    public static void clear() {
        refresh();
        notifications.clear();
        logs.clear();
        configurations.clear();
    }

    // Separator

    public static boolean queueLog(String string) {
        if (string.length() <= ResearchEngine.maxDataLength) {
            logs.add(string);
            return true;
        }
        return false;
    }

    // Separator

    public static boolean queueNotification(String string, boolean absent) {
        if (string.length() <= ResearchEngine.maxDataLength) {
            string = string.replace(ChatColor.COLOR_CHAR, colorSyntaxCharacter);
            return absent ? notifications.addIfAbsent(string) : notifications.add(string);
        }
        return false;
    }

    public static void queueNotificationWithWebhook(UUID uuid, String name, int x, int y, int z, String type, String string, boolean absent) {
        long ms = System.currentTimeMillis();

        if (ms >= cooldown && queueNotification(string, absent)) {
            cooldown = ms + 500L; // Always First
            DiscordWebhooks.send(uuid, name, x, y, z, type, string);
        }
    }

    public static void queueWebhook(UUID uuid, String name, int x, int y, int z, String type, String string) {
        long ms = System.currentTimeMillis();

        if (ms >= cooldown) {
            cooldown = ms + 500L; // Always First
            DiscordWebhooks.send(uuid, name, x, y, z, type, string);
        }
    }

    // Separator

    public static boolean isEnabled() {
        return isOptionValid(getOptionValue());
    }

    public static boolean isOptionValid(String option) {
        return option.length() > 0 && !option.equals("specify server name") && !option.equals("false") && !option.equals(getOption());
    }

    public static String getOption() {
        for (String option : options) {
            String value = Settings.getString(option);

            if (!option.equals(value)) {
                return option;
            }
        }
        return options[options.length - 1];
    }

    public static String getOptionValue() {
        if (serverName != null) {
            return serverName;
        }
        String option = Settings.getString(getOption());
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
        if (isEnabled() && CloudFeature.canSynchroniseFiles(true) && file.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = configuration.getKeys(true);
            int size = keys.size();

            if (size > 0) {
                List<String> list = new ArrayList<>(size);

                for (String key : keys) {
                    Object value = configuration.get(key);
                    list.add(file.getPath() + CloudFeature.separator + key + CloudFeature.separator + (value == null ? "NULL" : value.toString()));
                }
                configurations.addAll(list);
            }
        }
    }
}
