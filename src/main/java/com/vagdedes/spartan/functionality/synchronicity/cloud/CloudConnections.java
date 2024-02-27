package com.vagdedes.spartan.functionality.synchronicity.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.handlers.connection.DiscordMemberCount;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.profiling.PlayerViolation;
import com.vagdedes.spartan.objects.profiling.ViolationHistory;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.net.URLEncoder;
import java.util.*;

public class CloudConnections {

    // Once

    public static int getUserIdentification() {
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
                                    String[] reply = RequestUtils.get(StringUtils.decodeBase64(CloudFeature.accountWebsite) + "?token=" + name);

                                    if (reply.length > 0) {
                                        String line = reply[0];

                                        if (AlgebraUtils.validInteger(line)) {
                                            CloudFeature.token = name;
                                            return Integer.parseInt(line);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Separator

            String[] reply = RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website)
                            + "?action=get"
                            + "&data=userIdentification"
                            + "&version=" + CloudFeature.version
                            + (!CloudFeature.identification.isEmpty() ? "&" + CloudFeature.identification : ""),
                    RequestUtils.defaultTimeOut / 2);

            if (reply.length > 0) {
                String line = reply[0];

                if (AlgebraUtils.validInteger(line)) {
                    return Integer.parseInt(line);
                }
            }
        } catch (Exception e) {
            CloudFeature.throwError(e, "userIdentification:GET");
            return 0;
        }
        return -1;
    }

    public static void logServerSpecifications() {
        SpartanBukkit.connectionThread.execute(() -> {
            Runtime runtime = Runtime.getRuntime();
            String motd = StringUtils.getClearColorString(Bukkit.getMotd());

            String specs = MultiVersion.versionString() + CloudFeature.separator
                    + Bukkit.getPort() + CloudFeature.separator
                    + runtime.availableProcessors() + CloudFeature.separator
                    + (runtime.totalMemory() / Cache.gradeDivisor / Cache.gradeDivisor) + CloudFeature.separator
                    + Register.manager.getPlugins().length + CloudFeature.separator
                    + Base64.getEncoder().encodeToString(motd.getBytes());

            try {
                RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + "?" + CloudFeature.identification + "&action=add&data=serverSpecifications"
                        + "&value=" + URLEncoder.encode(specs, "UTF-8") + "&version=" + CloudFeature.version);
            } catch (Exception e) {
                CloudFeature.throwError(e, "serverSpecifications:ADD");
            }
        });
    }

    public static boolean ownsProduct(String productID) { // Once
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + "?" + CloudFeature.identification
                    + "&action=get&data=ownsProduct&version=" + CloudFeature.version + "&value=" + productID);

            if (results.length > 0) {
                return results[0].equals("true");
            }
        } catch (Exception e) {
            CloudFeature.throwError(e, "ownsProduct:GET");
        }
        return false;
    }

    // Multiple

    public static String[] getCrossServerInformation(String type, String name) {
        // Doesn't need ID validation due to its validated method call
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + "?" + CloudFeature.identification
                            + "&action=get&data=crossServerInformation&version=" + CloudFeature.version
                            + "&value=" + URLEncoder.encode(Bukkit.getPort() + CloudFeature.separator + type + (name != null ? (CloudFeature.separator + name) : ""), "UTF-8"),
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
            CloudFeature.throwError(e, "crossServerInformation:GET");
            return new String[]{};
        }
    }

    public static String[][] getStaffAnnouncements() { // Once
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + "?" + CloudFeature.identification
                    + "&action=get&data=staffAnnouncements&version=" + CloudFeature.version);

            if (results.length > 0) {
                String[] announcements = results[0].split(CloudFeature.separator);
                String[][] array = new String[results.length][0];

                for (int i = 0; i < announcements.length; i++) {
                    array[i] = StringUtils.decodeBase64(announcements[i]).split(CloudFeature.separator);
                }
                return array;
            }
        } catch (Exception e) {
            CloudFeature.throwError(e, "staffAnnouncements:GET");
        }
        return new String[][]{};
    }

    public static void sendCrossServerInformation(String type, String name, String[] array) {
        // Doesn't need ID validation due to its validated method call
        try {
            StringBuilder information = new StringBuilder();

            for (String string : array) {
                information.append(StringUtils.encodeBase64(string)).append(CloudFeature.separator);
            }
            int length = information.length();
            int separatorLength = CloudFeature.separator.length();

            if (length > separatorLength) {
                information = new StringBuilder(information.substring(0, length - separatorLength));
                RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + " " +
                                CloudFeature.identification + "&action=add&data=crossServerInformation&version=" + CloudFeature.version
                                + "&value=" + URLEncoder.encode(Bukkit.getPort() + CloudFeature.separator + type + CloudFeature.separator + name + CloudFeature.separator + information, "UTF-8"),
                        "POST", null, RequestUtils.defaultTimeOut / 2);
            }
        } catch (Exception e) {
            CloudFeature.throwError(e, "crossServerInformation:ADD");
        }
    }

    static void punishPlayers() {
        // Doesn't need ID validation due to its validated method call
        if (FalsePositiveDetection.canFunction()) {
            StringBuilder value = new StringBuilder();

            for (PlayerProfile playerProfile : ResearchEngine.getHackers()) {
                SpartanPlayer player = playerProfile.getSpartanPlayer();
                boolean isNull = player == null;

                if (isNull || !Permissions.isStaff(player) && !player.isOp()) {
                    OfflinePlayer offlinePlayer = playerProfile.getOfflinePlayer();

                    if (offlinePlayer != null && !offlinePlayer.isOp()) {
                        UUID uuid = offlinePlayer.getUniqueId();
                        String ipAddress;

                        if (!isNull && offlinePlayer.isOnline()) {
                            ipAddress = player.ipAddress;

                            if (ipAddress == null) {
                                ipAddress = "NULL";
                            }
                        } else {
                            ipAddress = "NULL";
                        }
                        value.append(StringUtils.encodeBase64(uuid + CloudFeature.separator + ipAddress)).append(CloudFeature.separator);
                    }
                }
            }

            if (value.length() > 0) {
                value = new StringBuilder(value.substring(0, value.length() - CloudFeature.separator.length()));

                try {
                    RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + " " +
                                    CloudFeature.identification + "&action=add&data=punishedPlayers&version=" + CloudFeature.version
                                    + "&value=" + URLEncoder.encode(value.toString(), "UTF-8"),
                            "POST");
                } catch (Exception e) {
                    CloudFeature.throwError(e, "punishedPlayers:ADD");
                }
            }
        }
    }

    public static void updatePunishedPlayer(UUID uuid, String ipAddress) {
        if (ipAddress == null) {
            ipAddress = "NULL";
        }
        try {
            RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + "?" + CloudFeature.identification + "&action=get&data=punishedPlayers&version=" + CloudFeature.version
                            + "&value=" + URLEncoder.encode(uuid + CloudFeature.separator + ipAddress, "UTF-8"),
                    RequestUtils.minimumTimeOut);
        } catch (Exception e) {
            CloudFeature.throwError(e, "punishedPlayers:GET");
        }
    }

    public static void executeDiscordWebhook(String webhook, UUID uuid, String name, int x, int y, int z, String type, String information) { // Once
        String url = Config.settings.getString("Discord." + webhook + "_webhook_url");

        if (url.startsWith("https://") || url.startsWith("http://")) {
            String color = Config.settings.getString("Discord.webhook_hex_color");
            int length = color.length();

            if (length >= 3 && length <= 6) {
                Runnable runnable = () -> {
                    try {
                        int webhookVersion = 2;
                        String crossServerInformationOption = CrossServerInformation.getOptionValue();
                        RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + "?" + CloudFeature.identification
                                + "&action=add&data=discordWebhooks&version=" + CloudFeature.version + "&value="
                                + URLEncoder.encode(
                                webhookVersion + CloudFeature.separator
                                        + url + CloudFeature.separator
                                        + color + CloudFeature.separator
                                        + (CrossServerInformation.isOptionValid(crossServerInformationOption) ? crossServerInformationOption : "NULL") + CloudFeature.separator
                                        + name + CloudFeature.separator
                                        + uuid + CloudFeature.separator
                                        + x + CloudFeature.separator
                                        + y + CloudFeature.separator
                                        + z + CloudFeature.separator
                                        + StringUtils.getClearColorString(type) + CloudFeature.separator
                                        + StringUtils.getClearColorString(information) + CloudFeature.separator
                                        + Config.settings.getBoolean(Settings.showEcosystemOption), "UTF-8"));
                    } catch (Exception e) {
                        CloudFeature.throwError(e, "discordWebhooks:ADD");
                    }
                };

                if (SpartanBukkit.isSynchronised()) {
                    SpartanBukkit.connectionThread.execute(runnable);
                } else {
                    runnable.run();
                }
            }
        }
    }

    // Manual

    private static String sendCustomerSupport(String contactPlatform, String contactInformation,
                                              String columnType, String columnInformation,
                                              String userInformation, boolean local) {
        String name = null;

        if (contactInformation == null) {
            contactInformation = "None";
        } else if (contactPlatform.equals("discord")) {
            String[] discordTag;

            if (contactInformation.contains("#")) {
                discordTag = StringUtils.getDiscordTag(contactInformation);

                if (discordTag == null) {
                    return "The Discord tag you provided is incorrect.";
                }
            } else {
                discordTag = new String[]{contactInformation};
            }
            name = discordTag[0];
        }

        for (Enums.HackType hackType : Enums.HackType.values()) {
            Check check = hackType.getCheck();
            String checkName = check.getName();

            if (check.toString().equals(columnInformation) || checkName.equalsIgnoreCase(columnInformation)) {
                ViolationHistory violationHistory = ResearchEngine.getViolationHistory(hackType, ResearchEngine.DataType.Universal, ResearchEngine.getLegitimatePlayers());

                if (violationHistory == null) {
                    return "No useful information was found for this check, try again later.";
                }
                Collection<PlayerViolation> violations = violationHistory.getCollection();

                String comma = ", ", newLine = "\n";
                StringBuilder softwareInformation = new StringBuilder();

                softwareInformation.append("Check:")
                        .append(newLine);
                softwareInformation.append("Type: ").append(hackType)
                        .append(newLine);
                softwareInformation.append("Name: ").append(checkName)
                        .append(newLine);
                softwareInformation.append("Enabled: ").append(check.isEnabled(null, null, null))
                        .append(newLine);
                softwareInformation.append("Preventions: ").append(!check.isSilent(null))
                        .append(newLine);
                softwareInformation.append("Punishments: ").append(check.canPunish)
                        .append(newLine);
                softwareInformation.append("Cancel Violation: ").append(CancelViolation.get(hackType, ResearchEngine.DataType.Universal))
                        .append(newLine);
                softwareInformation.append("Violation Count: ").append(violationHistory.getCount())
                        .append(newLine);

                // Separator

                softwareInformation.append("Server:")
                        .append(newLine);
                softwareInformation.append("Version: ").append(MultiVersion.fork()).append(" ").append(MultiVersion.versionString())
                        .append(newLine);
                softwareInformation.append("Testing Environment: ").append(TestServer.isIdentified())
                        .append(newLine);
                softwareInformation.append("Configuration: ").append(Config.isLegacy() ? "Legacy" : "Normal")
                        .append(newLine).append(newLine);

                softwareInformation.append("Plugins:").append(newLine)
                        .append(StringUtils.toString(Register.manager.getPlugins(), comma))
                        .append(newLine).append(newLine); // 2 lines

                List<Compatibility.CompatibilityType> compatibilities = Config.compatibility.getActiveCompatibilities();
                if (!compatibilities.isEmpty()) {
                    StringBuilder compatibilitiesNames = new StringBuilder();

                    for (Compatibility.CompatibilityType compatibility : compatibilities) {
                        compatibilitiesNames.append(compatibility.toString()).append(comma);
                    }
                    softwareInformation.append("Compatibilities:").append(newLine)
                            .append(compatibilitiesNames.toString(), 0, compatibilitiesNames.length() - comma.length())
                            .append(newLine).append(newLine).append(newLine); // 3 lines
                } else {
                    softwareInformation.append(newLine); // +1 line
                }

                // Separator

                softwareInformation.append("Configuration:").append(newLine);
                String dataFolder = Register.plugin.getDataFolder() + "/";

                for (String fileName : Config.configs) {
                    softwareInformation.append(fileName).append(":").append(newLine);
                    File file = new File(dataFolder + fileName);

                    if (file.exists() && file.isFile()
                            && !fileName.equals(Config.messages.getFile().getName())) {
                        switch (fileName) {
                            case Config.checksFileName:
                                Collection<Map.Entry<String, Object>> options = check.getOptions();

                                if (!options.isEmpty()) {
                                    for (Map.Entry<String, Object> entry : options) {
                                        softwareInformation.append(entry.getKey()).append(": ")
                                                .append(entry.getValue().toString().replace(newLine, "%%__line__%%"))
                                                .append(newLine);
                                    }
                                }
                                break;
                            default:
                                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                                for (String key : configuration.getKeys(true)) {
                                    if (!key.contains("password")) {
                                        Object object = configuration.get(key, null);

                                        if (object instanceof Number
                                                || object instanceof Boolean
                                                || object instanceof String) {
                                            softwareInformation.append(key).append(": ")
                                                    .append(object.toString().replace(newLine, "%%__line__%%"))
                                                    .append(newLine);
                                        }
                                    }
                                }
                                break;
                        }
                    }
                }
                softwareInformation.append(newLine);

                // Separator

                int entriesSize = violations.size(),
                        limit = 250,
                        minViolations = Integer.MAX_VALUE,
                        maxViolations = 0,
                        averageViolations = 0,
                        validViolations = 0,
                        totalViolations = 0;
                softwareInformation.append("Detection Information:").append(newLine);
                Set<Integer> uniqueInformation = new HashSet<>(limit);

                if (entriesSize > 0) {
                    for (PlayerViolation playerViolation : violations) {
                        int violation = playerViolation.level;

                        minViolations = Math.min(minViolations, violation);
                        maxViolations = Math.max(maxViolations, violation);

                        averageViolations += violation;
                        totalViolations++;
                        String information = "x" + playerViolation.level
                                + " (" + playerViolation.information + ")";

                        if (uniqueInformation.add(playerViolation.similarityIdentity)) {
                            softwareInformation.append(information).append(newLine);
                            validViolations++;

                            if (validViolations == limit) {
                                break;
                            }
                        }
                    }
                } else {
                    softwareInformation.append("None").append(newLine);
                }

                softwareInformation.append(newLine).append(newLine) // +2 lines
                        .append("Detection Statistics:").append(newLine)
                        .append("Minimum Violations: ").append(minViolations)
                        .append(newLine)
                        .append("Average Violations: ").append(AlgebraUtils.cut(averageViolations / ((double) totalViolations), 2))
                        .append(newLine)
                        .append("Maximum Violations: ").append(maxViolations)
                        .append(newLine)
                        .append("Problematic Detections: ").append(check.getProblematicDetections());

                try {
                    String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudFeature.website) + " " + CloudFeature.identification
                            + "&action=add&data=customerSupport&version=" + CloudFeature.version
                            + "&value=" + URLEncoder.encode(
                            contactPlatform + CloudFeature.separator + contactInformation + CloudFeature.separator
                                    + columnType + CloudFeature.separator + columnInformation + CloudFeature.separator
                                    + userInformation + CloudFeature.separator + softwareInformation,
                            "UTF-8"), "POST");

                    if (results.length > 0) {
                        String data = results[0];

                        if (data.equals("false")) {
                            return "Failed to complete request, reach support if this continues.";
                        } else if (data.equals("true")) {
                            return "Thanks for reporting" + (name != null ? " " + name : "") + ", we will get back to you if needed. You can join our Discord server at: §n" + DiscordMemberCount.discordURL;
                        }
                    }
                    return "No results were returned from the server, try updating and reach support if this continues.";
                } catch (Exception e) {
                    CloudFeature.throwError(e, "customerSupport:ADD");
                    return "A programming exception has occurred, reach support if this continues.";
                }
            }
        }
        return "Check not found, check your arguments.";
    }

    public static String sendCustomerSupport(String contactInformation, String columnInformation, String userInformation, boolean local) {
        return sendCustomerSupport(
                "discord", contactInformation,
                "functionality", columnInformation,
                userInformation,
                local
        );
    }
}
