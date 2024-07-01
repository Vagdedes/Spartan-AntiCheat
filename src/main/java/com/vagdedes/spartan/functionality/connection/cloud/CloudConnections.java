package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CloudConnections {

    // Once

    static int getUserIdentification() {
        try {
            File path = new File("plugins/");

            if (path.exists()
                    && path.isDirectory()) {
                File[] files = path.listFiles();
                String pluginName = Register.plugin.getName();
                String fileType = ".jar";

                if (files != null
                        && files.length > 0) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String name = file.getName();

                            if (name.startsWith(pluginName) && name.endsWith(fileType)) {
                                name = name.replace(fileType, "");

                                for (String jarName : SpartanEdition.jarNames) {
                                    name = name.replace(jarName, "");
                                }

                                if (!name.isEmpty()) {
                                    String[] reply = RequestUtils.get(StringUtils.decodeBase64(CloudBase.accountWebsite) + "?token=" + name);

                                    if (reply.length > 0) {
                                        String line = reply[0];

                                        if (AlgebraUtils.validInteger(line)) {
                                            int id = Integer.parseInt(line);
                                            CloudBase.token = name;
                                            IDs.set(id, name.hashCode());
                                            return id;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Separator

            String[] reply = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website)
                            + "?action=get"
                            + "&data=userIdentification"
                            + "&version=" + CloudBase.version,
                    RequestUtils.defaultTimeOut);

            if (reply.length > 0) {
                String line = reply[0];

                if (AlgebraUtils.validInteger(line)) {
                    int id = Integer.parseInt(line);
                    IDs.set(id, id);
                    return id;
                }
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "userIdentification:GET");
            return 0;
        }
        return -1;
    }

    static boolean ownsProduct(String productID) { // Once
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification
                    + "&action=get&data=ownsProduct&version=" + CloudBase.version + "&value=" + productID);

            if (results.length > 0) {
                return results[0].equals("true");
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "ownsProduct:GET");
        }
        return false;
    }

    // Multiple

    static int getDetectionSlots() { // Once
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification
                    + "&action=get&data=detectionSlots&version=" + CloudBase.version
                    + "&value=" + URLEncoder.encode(Bukkit.getPort() + CloudBase.separator + SpartanBukkit.getPlayerCount(), "UTF-8"));

            if (results.length == 1 && AlgebraUtils.validInteger(results[0])) {
                return Integer.parseInt(results[0]);
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "detectionSlots:GET");
        }
        return 5;
    }

    public static String[] getCrossServerInformation(String type, String name) {
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification
                            + "&action=get&data=crossServerInformation&version=" + CloudBase.version
                            + "&value=" + URLEncoder.encode(Bukkit.getPort() + CloudBase.separator + type + (name != null ? (CloudBase.separator + name) : ""), "UTF-8"),
                    "GET", null, RequestUtils.defaultTimeOut / 2);

            if (results.length > 0) {
                List<String> list = new LinkedList<>();

                for (String result : results) {
                    list.add(StringUtils.decodeBase64(result));
                }
                return list.toArray(new String[0]);
            } else {
                return new String[]{};
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "crossServerInformation:GET");
            return new String[]{};
        }
    }

    static String[][] getStaffAnnouncements() { // Once
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification
                    + "&action=get&data=staffAnnouncements&version=" + CloudBase.version);

            if (results.length > 0) {
                String[] announcements = results[0].split(CloudBase.separator);
                String[][] array = new String[results.length][0];

                for (int i = 0; i < announcements.length; i++) {
                    array[i] = StringUtils.decodeBase64(announcements[i]).split(CloudBase.separator);
                }
                return array;
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "staffAnnouncements:GET");
        }
        return new String[][]{};
    }

    static void sendCrossServerInformation(String type, String name, String[] array) {
        try {
            StringBuilder information = new StringBuilder();

            for (String string : array) {
                information.append(StringUtils.encodeBase64(string)).append(CloudBase.separator);
            }
            int length = information.length();
            int separatorLength = CloudBase.separator.length();

            if (length > separatorLength) {
                information = new StringBuilder(information.substring(0, length - separatorLength));
                RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + " " +
                                CloudBase.identification + "&action=add&data=crossServerInformation&version=" + CloudBase.version
                                + "&value=" + URLEncoder.encode(Bukkit.getPort() + CloudBase.separator + type + CloudBase.separator + name + CloudBase.separator + information, "UTF-8"),
                        "POST", null, RequestUtils.defaultTimeOut / 2);
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "crossServerInformation:ADD");
        }
    }

    static void punishPlayers() {
        StringBuilder value = new StringBuilder();

        for (PlayerProfile playerProfile : ResearchEngine.getPlayerProfiles()) {
            if (!playerProfile.isLegitimate()) {
                SpartanPlayer player = playerProfile.getSpartanPlayer();
                boolean isNull = player == null;

                if (isNull || !Permissions.isStaff(player)) {
                    OfflinePlayer offlinePlayer = playerProfile.getOfflinePlayer();

                    if (offlinePlayer != null && !offlinePlayer.isOp()) {
                        UUID uuid = offlinePlayer.getUniqueId();
                        String ipAddress;

                        if (!isNull && offlinePlayer.isOnline()) {
                            ipAddress = player.getIpAddress();

                            if (ipAddress == null) {
                                ipAddress = "NULL";
                            }
                        } else {
                            ipAddress = "NULL";
                        }
                        value.append(StringUtils.encodeBase64(uuid + CloudBase.separator + ipAddress)).append(CloudBase.separator);
                    }
                }
            }
        }

        if (value.length() > 0) {
            value = new StringBuilder(value.substring(0, value.length() - CloudBase.separator.length()));

            try {
                RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + " " +
                                CloudBase.identification + "&action=add&data=punishedPlayers&version=" + CloudBase.version
                                + "&value=" + URLEncoder.encode(value.toString(), "UTF-8"),
                        "POST");
            } catch (Exception e) {
                CloudBase.throwError(e, "punishedPlayers:ADD");
            }
        }
    }

    public static void updatePunishedPlayer(UUID uuid, String ipAddress) {
        if (ipAddress == null) {
            ipAddress = "NULL";
        }
        try {
            RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification + "&action=get&data=punishedPlayers&version=" + CloudBase.version
                            + "&value=" + URLEncoder.encode(uuid + CloudBase.separator + ipAddress, "UTF-8"),
                    RequestUtils.minimumTimeOut);
        } catch (Exception e) {
            CloudBase.throwError(e, "punishedPlayers:GET");
        }
    }

    public static void executeDiscordWebhook(String webhook, UUID uuid, String name, int x, int y, int z, String type, String information) { // Once
        String url = Config.settings.getString("Discord." + webhook + "_webhook_url");

        if (url.startsWith("https://") || url.startsWith("http://")) {
            String color = Config.settings.getString("Discord.webhook_hex_color");
            int length = color.length();

            if (length >= 3 && length <= 6) {
                SpartanBukkit.connectionThread.executeIfSyncElseHere(() -> {
                    try {
                        int webhookVersion = 2;
                        String crossServerInformationOption = CrossServerInformation.getOptionValue();
                        RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification
                                + "&action=add&data=discordWebhooks&version=" + CloudBase.version + "&value="
                                + URLEncoder.encode(
                                webhookVersion + CloudBase.separator
                                        + url + CloudBase.separator
                                        + color + CloudBase.separator
                                        + (CrossServerInformation.isOptionValid(crossServerInformationOption) ? crossServerInformationOption : "NULL") + CloudBase.separator
                                        + name + CloudBase.separator
                                        + uuid + CloudBase.separator
                                        + x + CloudBase.separator
                                        + y + CloudBase.separator
                                        + z + CloudBase.separator
                                        + StringUtils.getClearColorString(type) + CloudBase.separator
                                        + StringUtils.getClearColorString(information), "UTF-8"));
                    } catch (Exception e) {
                        CloudBase.throwError(e, "discordWebhooks:ADD");
                    }
                });
            }
        }
    }

}
