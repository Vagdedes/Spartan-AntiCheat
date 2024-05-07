package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.profiling.ViolationHistory;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.DiscordMemberCount;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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
                                            CloudBase.token = name;
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

            String[] reply = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website)
                            + "?action=get"
                            + "&data=userIdentification"
                            + "&version=" + CloudBase.version
                            + (!CloudBase.identification.isEmpty() ? "&" + CloudBase.identification : ""),
                    RequestUtils.defaultTimeOut / 2);

            if (reply.length > 0) {
                String line = reply[0];

                if (AlgebraUtils.validInteger(line)) {
                    return Integer.parseInt(line);
                }
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "userIdentification:GET");
            return 0;
        }
        return -1;
    }

    static void logServerSpecifications() {
        SpartanBukkit.connectionThread.execute(() -> {
            Runtime runtime = Runtime.getRuntime();
            String motd = StringUtils.getClearColorString(Bukkit.getMotd());

            String specs = MultiVersion.versionString() + CloudBase.separator
                    + Bukkit.getPort() + CloudBase.separator
                    + runtime.availableProcessors() + CloudBase.separator
                    + (runtime.totalMemory() / 1024 / 1024) + CloudBase.separator
                    + Register.manager.getPlugins().length + CloudBase.separator
                    + Base64.getEncoder().encodeToString(motd.getBytes());

            try {
                RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification + "&action=add&data=serverSpecifications"
                        + "&value=" + URLEncoder.encode(specs, "UTF-8") + "&version=" + CloudBase.version);
            } catch (Exception e) {
                CloudBase.throwError(e, "serverSpecifications:ADD");
            }
        });
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
                    + "&value=" + Bukkit.getPort() + CloudBase.separator + SpartanBukkit.getPlayerCount());

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
                    value.append(StringUtils.encodeBase64(uuid + CloudBase.separator + ipAddress)).append(CloudBase.separator);
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
                Runnable runnable = () -> {
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
                                        + StringUtils.getClearColorString(information) + CloudBase.separator
                                        + Config.settings.getBoolean(Settings.showEcosystemOption), "UTF-8"));
                    } catch (Exception e) {
                        CloudBase.throwError(e, "discordWebhooks:ADD");
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

    public static String getAiAssistance(String question) {
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + "?" + CloudBase.identification
                    + "&action=get&data=aiAssistance&version=" + CloudBase.version + "&value="
                    + URLEncoder.encode(question, "UTF-8"));
            return results.length > 0 ? results[0] : null;
        } catch (Exception e) {
            CloudBase.throwError(e, "aiAssistance:GET");
            return null;
        }
    }

    private static String sendCustomerSupport(String contactPlatform, String contactInformation,
                                              String columnType, String columnInformation,
                                              String userInformation) {
        if (contactInformation == null) {
            contactInformation = "None";
        }

        for (Enums.HackType hackType : Enums.HackType.values()) {
            Check check = hackType.getCheck();
            String checkName = check.getName();

            if (check.toString().equals(columnInformation) || checkName.equalsIgnoreCase(columnInformation)) {
                ViolationHistory violationHistory = ResearchEngine.getViolationHistory(hackType, Enums.DataType.UNIVERSAL, ResearchEngine.getLegitimatePlayers());

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
                softwareInformation.append("Average Ignored Violations: ").append(check.getAverageIgnoredViolations(Enums.DataType.UNIVERSAL))
                        .append(newLine);
                softwareInformation.append("Violation Count: ").append(violationHistory.getCount())
                        .append(newLine);

                // Separator

                softwareInformation.append("Server:")
                        .append(newLine);
                softwareInformation.append("Version: ").append(MultiVersion.fork()).append(" ").append(MultiVersion.versionString())
                        .append(newLine);

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

                        if (uniqueInformation.add(playerViolation.identity)) {
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
                        .append("Maximum Violations: ").append(maxViolations);

                try {
                    String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website) + " " + CloudBase.identification
                            + "&action=add&data=customerSupport&version=" + CloudBase.version
                            + "&value=" + URLEncoder.encode(
                            contactPlatform + CloudBase.separator + contactInformation + CloudBase.separator
                                    + columnType + CloudBase.separator + columnInformation + CloudBase.separator
                                    + userInformation + CloudBase.separator + softwareInformation,
                            "UTF-8"), "POST");

                    if (results.length > 0) {
                        String data = results[0];

                        if (data.equals("false")) {
                            return "Failed to complete request, reach support if this continues.";
                        } else if (data.equals("true")) {
                            return "Thanks for reporting" + " " + contactInformation + ", we will get back to you if needed. You can join our Discord server at: §n" + DiscordMemberCount.discordURL;
                        }
                    }
                    return "No results were returned from the server, try updating and reach support if this continues.";
                } catch (Exception e) {
                    CloudBase.throwError(e, "customerSupport:ADD");
                    return "A programming exception has occurred, reach support if this continues.";
                }
            }
        }
        return "Check not found, check your arguments.";
    }

    public static String sendCustomerSupport(String contactInformation, String columnInformation, String userInformation) {
        return sendCustomerSupport(
                "discord", contactInformation,
                "functionality", columnInformation,
                userInformation
        );
    }
}
