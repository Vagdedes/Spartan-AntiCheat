package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.checks.combat.criticals.Criticals;
import com.vagdedes.spartan.checks.combat.criticals.CriticalsUtils;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.gui.spartan.MainMenu;
import com.vagdedes.spartan.objects.features.StatisticalProgress;
import com.vagdedes.spartan.objects.profiling.*;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.statistics.ViolationStatistics;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ResearchEngine {

    public static final int
            logRequirement = Check.maxViolationsPerCycle,
            profileRequirement = logRequirement / 10,
            maxRows = Cache.enoughRAM ? 100_000 : 20_000,
            maxBytes = maxRows * 1024 * 1024;

    private static final double minimumAverageMining = 16.0;

    private static final long cacheTimeLimit = 60_000L * 4; // Despite being 4, realistically it is 5 minutes if extra calculations are accounted for
    private static final int cacheRefreshTicks = 1200;
    private static int schedulerTicks = 0;

    private static boolean
            isFull = false,
            enoughData = false;

    public static final DataType[] usableDataTypes = new DataType[]{DataType.Java, DataType.Bedrock};
    private static StatisticalProgress statisticalProgress = new StatisticalProgress();

    private static final Map<String, PlayerProfile> playerProfiles
            = Collections.synchronizedMap(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final Map<Enums.MiningOre, Double> averageMining = new LinkedHashMap<>(Enums.MiningOre.values().length);
    private static final double[] defaultAverageMining = new double[Enums.MiningOre.values().length];

    static {
        for (Enums.MiningOre ore : Enums.MiningOre.values()) {
            switch (ore) {
                case DIAMOND:
                    defaultAverageMining[ore.ordinal()] = -32.0;
                    break;
                case EMERALD:
                case ANCIENT_DEBRIS:
                    defaultAverageMining[ore.ordinal()] = -minimumAverageMining;
                    break;
                case GOLD:
                    defaultAverageMining[ore.ordinal()] = -64.0;
                    break;
                default:
                    break;
            }
        }

        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (schedulerTicks == 0) {
                    schedulerTicks = cacheRefreshTicks * 10;

                    SpartanBukkit.analysisThread.executeIfFree(() -> {
                        if (isDatabaseMode()) {
                            refresh(false, Register.isPluginEnabled());
                        } else if (schedulerTicks % cacheRefreshTicks == 0) {
                            updateComprehensionCache();
                            MainMenu.refresh();
                        }
                    });
                } else {
                    schedulerTicks -= 1;

                    if (schedulerTicks % cacheRefreshTicks == 0) {
                        SpartanBukkit.analysisThread.executeIfFree(() -> {
                            updateComprehensionCache();
                            MainMenu.refresh();
                        });
                    }
                }
            }, 1L, 1L);
        }
    }

    public static boolean isStorageMode() {
        return Config.settings.getBoolean("Logs.log_file") || isDatabaseMode();
    }

    public static DataType[] getDynamicUsableDataTypes(boolean universal) {
        DataType dataType = SpartanEdition.getMissingDetection();
        return dataType == null ? (universal ? DataType.values() : usableDataTypes) :
                dataType == DataType.Bedrock ? (universal ? new DataType[]{DataType.Universal, DataType.Java} : new DataType[]{DataType.Java}) :
                        (universal ? new DataType[]{DataType.Universal, DataType.Bedrock} : new DataType[]{DataType.Bedrock});
    }

    // Separator

    public static boolean isDatabaseMode() {
        return Config.sql.isEnabled();
    }

    public static boolean enoughData() {
        if (enoughData) {
            return true;
        } else if (playerProfiles.size() >= profileRequirement) {
            int logs = 0, profiles = 0;

            synchronized (playerProfiles) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    profiles++;
                    logs += playerProfile.getUsefulLogs();

                    if (logs >= logRequirement && profiles >= profileRequirement) {
                        return enoughData = true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isFull() {
        return isFull;
    }

    // Separator

    public static void refresh(boolean message, boolean enabledPlugin) {
        Runnable runnable = () -> {
            // Complete Storage
            AntiCheatLogs.refresh();
            Config.sql.refreshDatabase();
            Criticals.clear();

            if (enabledPlugin) {
                CloudFeature.refresh(true);
                String status = recalculateCache();
                CloudFeature.refresh(false);

                if (message && status != null) {
                    AwarenessNotifications.forcefullySend(status);
                }
            } else {
                isFull = false;
                CloudFeature.clear(true);
                CancelViolation.clear();
                ViolationStatistics.clear();
            }
        };

        if (!SpartanBukkit.isSynchronised()) {
            runnable.run();
        } else {
            SpartanBukkit.analysisThread.executeIfFree(runnable);
        }
    }

    // Separator

    public static String getDetectionInformation(String s) {
        if (s.contains(PunishmentHistory.kickMessage)
                || s.contains(PunishmentHistory.warningMessage)
                || s.contains(PunishmentHistory.punishmentMessage)) {
            return null;
        }
        String[] split = s.split("\\), \\(");
        s = split[split.length - 1];
        return s.substring(0, s.length() - 2);
    }

    // Separator

    public static int getDetectionViolationLevel(String s) {
        int index1 = s.indexOf("(VL: ");

        if (index1 > -1) {
            int index2 = s.indexOf(") [");

            if (index2 > -1 && index2 > index1) {
                String number = s.substring(index1 + 5, index2);
                return AlgebraUtils.validInteger(number) ? Integer.parseInt(number) : -1;
            }
        }
        return -1;
    }

    // Separator

    public static StatisticalProgress getProgress() {
        return statisticalProgress;
    }

    // Separator

    public static List<PlayerProfile> getHackers() {
        if (!playerProfiles.isEmpty()) {
            List<PlayerProfile> list = new ArrayList<>(playerProfiles.size());

            synchronized (playerProfiles) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    if (playerProfile.isHacker()) {
                        list.add(playerProfile);
                    }
                }
            }
            return list;
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<PlayerProfile> getSuspectedPlayers() {
        if (!playerProfiles.isEmpty()) {
            List<PlayerProfile> list = new ArrayList<>(playerProfiles.size());

            synchronized (playerProfiles) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    if (playerProfile.isSuspected()) {
                        list.add(playerProfile);
                    }
                }
            }
            return list;
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<PlayerProfile> getLegitimatePlayers() {
        if (!playerProfiles.isEmpty()) {
            List<PlayerProfile> list = new ArrayList<>(playerProfiles.size());

            synchronized (playerProfiles) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    if (playerProfile.isLegitimate()) {
                        list.add(playerProfile);
                    }
                }
            }
            return list;
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<PlayerProfile> getPlayerProfiles() {
        synchronized (playerProfiles) {
            return new ArrayList<>(playerProfiles.values());
        }
    }

    // Separator

    public static PlayerProfile getPlayerProfile(String name) {
        synchronized (playerProfiles) {
            PlayerProfile playerProfile = playerProfiles.get(name);

            if (playerProfile != null) {
                return playerProfile;
            }
            playerProfile = new PlayerProfile(name);
            playerProfiles.put(name, playerProfile);
            return playerProfile;
        }
    }

    public static PlayerProfile getPlayerProfile(SpartanPlayer player) {
        synchronized (playerProfiles) {
            String name = player.name;
            PlayerProfile playerProfile = playerProfiles.get(name);

            if (playerProfile != null) {
                return playerProfile;
            }
            playerProfile = new PlayerProfile(player);
            playerProfiles.put(name, playerProfile);
            return playerProfile;
        }
    }

    public static PlayerProfile getPlayerProfileAdvanced(String name, boolean deep) {
        synchronized (playerProfiles) {
            PlayerProfile profile = playerProfiles.get(name);

            if (profile != null) {
                return profile;
            }
            if (deep && !playerProfiles.isEmpty()) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    if (playerProfile.getName().equalsIgnoreCase(name)) {
                        return playerProfile;
                    }
                }
            }
            return null;
        }
    }

    public static ViolationHistory getViolationHistory(Enums.HackType hackType, DataType dataType, Collection<PlayerProfile> profiles) {
        int size = profiles.size();

        if (size == 0) {
            return null;
        }
        Collection<PlayerViolation> list = new ArrayList<>(size);
        boolean universal = dataType == DataType.Universal;

        // Separator
        for (PlayerProfile playerProfile : profiles) {
            if (universal || ((dataType == DataType.Bedrock) == playerProfile.isBedrockPlayer())) {
                list.addAll(playerProfile.getViolationHistory(hackType).getRawCollection());
            }
        }

        if (list.isEmpty()) {
            return null;
        }
        return new ViolationHistory(list, false);
    }

    // Separator

    public static double getMiningHistoryAverage(Enums.MiningOre ore) {
        Double average = averageMining.get(ore);
        return average == null ? defaultAverageMining[ore.ordinal()] : average;
    }

    // Separator

    public static void resetData(Enums.HackType hackType) {
        SpartanBukkit.storageThread.execute(() -> {
            String hackTypeString = hackType.toString();
            hackType.getCheck().clearMaxCancelledViolations();
            CancelViolation.clear(hackType);

            // Separator

            if (!playerProfiles.isEmpty()) {
                synchronized (playerProfiles) {
                    for (PlayerProfile playerProfile : playerProfiles.values()) {
                        playerProfile.getViolationHistory(hackType).clear();
                        playerProfile.evidence.remove(hackType, true, true, true);
                    }
                }
            }

            // Separator

            if (Config.sql.isEnabled()) {
                Config.sql.update("DELETE FROM " + Config.sql.getTable() + " WHERE functionality = '" + hackTypeString + "';");
            }

            // Separator

            Collection<File> files = getFiles();

            if (!files.isEmpty()) {
                for (File file : files) {
                    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                    for (String key : configuration.getKeys(false)) {
                        String value = configuration.getString(key);

                        if (value != null && value.contains(hackTypeString)) {
                            configuration.set(key, null);
                        }
                    }
                    try {
                        configuration.save(file);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Separator
            MainMenu.refresh();
        });
    }

    public static void resetData(String playerName) {
        // Clear Violations
        SpartanPlayer p = SpartanBukkit.getPlayer(playerName);
        boolean foundPlayer = p != null;

        if (foundPlayer) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                p.getViolations(hackType).reset();
            }
        }

        if (isStorageMode()) {
            // Clear Files/Database
            SpartanBukkit.storageThread.execute(() -> {
                synchronized (playerProfiles) {
                    playerProfiles.remove(playerName);
                }
                if (foundPlayer) {
                    p.setProfile(getPlayerProfile(p));
                }
                if (Config.sql.isEnabled()) {
                    Config.sql.update("DELETE FROM " + Config.sql.getTable() + " WHERE information LIKE '%" + playerName + "%';");
                }
                Collection<File> files = getFiles();

                if (!files.isEmpty()) {
                    for (File file : files) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                        for (String key : configuration.getKeys(false)) {
                            String value = configuration.getString(key);

                            if (value != null && value.contains(playerName)) {
                                configuration.set(key, null);
                            }
                        }
                        try {
                            configuration.save(file);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else {
            synchronized (playerProfiles) {
                playerProfiles.remove(playerName);
            }
            if (foundPlayer) {
                p.setProfile(getPlayerProfile(p));
            }
        }
        enoughData = false;
        MainMenu.refresh();
        SpartanMenu.playerInfo.refresh(playerName);
    }

    // Separator

    public static Collection<File> getFiles() {
        File[] files = new File(Register.plugin.getDataFolder() + "/logs/").listFiles();

        if (files != null && files.length > 0) {
            TreeMap<Integer, File> map = new TreeMap<>();
            String start = "log", end = ".yml";
            int startLength = start.length(), endLength = end.length();

            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName();

                    if (name.startsWith(start) && name.endsWith(end)) {
                        Integer integer = AlgebraUtils.returnValidInteger(name.substring(startLength, name.length() - endLength));

                        if (integer != null) {
                            map.put(integer, file);
                        }
                    }
                }
            }
            List<File> list = new LinkedList<>(map.values());
            Collections.reverse(list);
            return list;
        }
        return new ArrayList<>(0);
    }

    // Separator

    public static Map<String, String> getLogs() {
        long startTime = System.currentTimeMillis();
        Map<String, String> cache = new LinkedHashMap<>();
        int byteSize = 0;
        isFull = false;
        boolean continueWithYAML = false;

        // Separator
        if (Config.settings.getBoolean(Settings.cloudSynchroniseFilesOption)) {
            String crossServerInformationOption = CrossServerInformation.getOptionValue();

            if (CrossServerInformation.isOptionValid(crossServerInformationOption)) {
                String[] incomingInformation = CloudConnections.getCrossServerInformation("log", crossServerInformationOption);

                if (incomingInformation != null && incomingInformation.length > 0) {
                    String key = AntiCheatLogs.syntaxDate(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()), AlgebraUtils.randomInteger(1, Check.maxViolationsPerCycle - 1));

                    for (String information : incomingInformation) {
                        AntiCheatLogs.logInfo(null, information, null, null, null, false, -1);
                        cache.put(key, information);
                        byteSize += key.length() + information.length();

                        if (byteSize >= maxBytes) {
                            isFull = true;
                            break;
                        } else if (System.currentTimeMillis() - startTime >= cacheTimeLimit) {
                            break;
                        }
                    }
                }
            }
        }

        // Separator

        if (Config.sql.isEnabled()) {
            if (!isFull) {
                try {
                    ResultSet rs = Config.sql.query("SELECT creation_date, information FROM " + Config.sql.getTable() + " ORDER BY id DESC LIMIT " + maxRows + ";");

                    if (rs != null) {
                        while (rs.next()) {
                            String data = rs.getString("information");
                            Timestamp t = rs.getTimestamp("creation_date");
                            String date = "(" + AlgebraUtils.randomInteger(1, Check.maxViolationsPerCycle - 1) + ")[" + TimeUtils.getYearMonthDay(t) + " " + TimeUtils.getTime(t) + "]";
                            cache.put(date, data);
                            byteSize += date.length() + data.length();

                            if (byteSize >= maxBytes) {
                                isFull = true;
                                break;
                            } else if (System.currentTimeMillis() - startTime >= cacheTimeLimit) {
                                break;
                            }
                        }
                        rs.close();

                        if (cache.isEmpty()) {
                            continueWithYAML = true;
                        }
                    } else {
                        continueWithYAML = true;
                    }
                } catch (Exception ex) {
                    continueWithYAML = true;
                }
            }
        } else {
            continueWithYAML = true;
        }

        // YAML Process

        if (!isFull && continueWithYAML) {
            Collection<File> files = getFiles();

            if (!files.isEmpty()) {
                //Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

                for (File file : files) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

                    for (String key : c.getKeys(false)) {
                        String data = c.getString(key);

                        if (data != null) {
                            cache.put(key, data);
                            byteSize += key.length() + data.length();

                            if (byteSize >= maxBytes) {
                                isFull = true;
                                break;
                            } else if (System.currentTimeMillis() - startTime >= cacheTimeLimit) {
                                break;
                            }
                        } else if (System.currentTimeMillis() - startTime >= cacheTimeLimit) {
                            break;
                        }
                    }
                    if (isFull || System.currentTimeMillis() - startTime >= cacheTimeLimit) {
                        break;
                    }
                }
            }
        }
        return cache;
    }

    public static int getStorageKey(Enums.HackType hackType, DataType dataType) {
        return (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + dataType.hashCode();
    }

    // Separator

    private static String recalculateCache() {
        if (isStorageMode()) {
            Map<String, String> logs = getLogs();

            // Separator
            String toReturn;
            int size = logs.size();

            if (size > 0) {
                String falsePositiveDisclaimer = StringUtils.getClearColorString(LiveViolation.falsePositiveDisclaimer);

                try {
                    String construct = Config.getConstruct();

                    for (Map.Entry<String, String> entry : logs.entrySet()) {
                        String key = entry.getKey();
                        int dateStart = key.indexOf("[") + 1;

                        if (dateStart > 0) {
                            String fullDate = key.substring(dateStart, key.length() - 1);
                            String partialDate = key.substring(dateStart, dateStart + 10);

                            String data = entry.getValue().replace(construct, "");
                            int dataLength = data.length();
                            int index = data.indexOf("]");

                            if (index > -1 && index != (dataLength - 1)) {
                                data = data.substring(index + 2);
                            }
                            int greatestSplitPosition = 10; // Attention
                            String[] split = data.split(" ", greatestSplitPosition + 1);

                            if (data.contains(CriticalsUtils.criticalHitMessage)) {
                                if (split.length >= greatestSplitPosition) {
                                    Material material = Material.getMaterial(split[6].replace("-", "_").toUpperCase());

                                    if (material != null) {
                                        Double decimal = AlgebraUtils.returnValidDecimal(split[8]);

                                        if (decimal != null) {
                                            CriticalsUtils.cache(split[0], material, decimal);
                                        }
                                    }
                                }
                            } else if (data.contains(PunishmentHistory.warningMessage)) {
                                getPlayerProfile(split[0]).punishmentHistory.increaseWarnings(null, null);
                            } else if (data.contains(PunishmentHistory.kickMessage)) {
                                getPlayerProfile(split[0]).punishmentHistory.increaseKicks(null, null);
                            } else if (data.contains(PunishmentHistory.punishmentMessage)) {
                                getPlayerProfile(split[0]).punishmentHistory.increasePunishments(null, null);
                            } else if (data.contains(" failed ")) {
                                if (split.length >= 3) {
                                    if (!data.startsWith(falsePositiveDisclaimer)) {
                                        String name = split[0];
                                        String hackTypeString = split[2];
                                        PlayerProfile playerProfile = getPlayerProfile(name);

                                        for (Enums.HackType hackType : Enums.HackType.values()) {
                                            if (hackTypeString.equals(hackType.toString())) {
                                                // Separator
                                                String detection = getDetectionInformation(data);

                                                if (detection != null) {
                                                    int violation = getDetectionViolationLevel(data);

                                                    if (violation != -1) {
                                                        SimpleDateFormat sdf = new SimpleDateFormat(
                                                                fullDate.contains(AntiCheatLogs.dateFormatChanger) ?
                                                                        AntiCheatLogs.dateFormat :
                                                                        AntiCheatLogs.usualDateFormat
                                                        );
                                                        playerProfile.getViolationHistory(hackType).store(
                                                                new PlayerViolation(hackType, sdf.parse(fullDate).getTime(), detection, violation)
                                                        );
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else if (data.contains(" found ")) {
                                if (split.length >= 9) {
                                    String name = split[0].toLowerCase();

                                    try {
                                        Enums.MiningOre ore = DetectionNotifications.getMiningOre(
                                                Material.getMaterial(
                                                        split[3].toUpperCase().replace("-", "_")
                                                )
                                        );

                                        if (ore != null) {
                                            MiningHistory miningHistory = getPlayerProfile(name).getMiningHistory(ore);

                                            if (miningHistory != null) {
                                                World.Environment environment = World.Environment.valueOf(
                                                        split[8].toUpperCase().replace("-", "_")
                                                );
                                                miningHistory.increaseMines(environment, 1, partialDate);
                                            }
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                    enoughData = false;
                    updateComprehensionCache();
                    MainMenu.refresh();

                    // Separator
                    toReturn = "Done loading " + size + " rows of data to the Research Engine.";
                } catch (Exception ex) {
                    ex.printStackTrace();
                    toReturn = null;
                }
            } else {
                toReturn = null;
            }
            return toReturn;
        } else {
            isFull = false;
            return "Research Engine will function with memory only due to all log saving options being disabled.";
        }
    }

    private static void updateComprehensionCache() {
        if (!playerProfiles.isEmpty()) {
            Collection<PlayerProfile> playerProfiles;

            synchronized (ResearchEngine.playerProfiles) {
                playerProfiles = new ArrayList<>(ResearchEngine.playerProfiles.values());
            }
            Enums.HackType[] hackTypes = Enums.HackType.values();
            DataType[] dataTypes = getDynamicUsableDataTypes(false);
            List<PlayerProfile> legitimatePlayers = getLegitimatePlayers();
            int mines = 0, logs = 0, kicks = 0, warnings = 0, punishments = 0;
            ViolationStatistics.calculateData(playerProfiles);

            for (PlayerProfile playerProfile : playerProfiles) {
                playerProfile.evidence.judge();
                PunishmentHistory punishmentHistory = playerProfile.punishmentHistory;
                logs += playerProfile.getUsefulLogs();
                mines += playerProfile.getOverallMiningHistory().getMines();
                kicks += punishmentHistory.getKicks();
                warnings += punishmentHistory.getWarnings();
                punishments += punishmentHistory.getPunishments();
            }

            // Separator

            List<SpartanPlayer> staffOnline = SpartanBukkit.getPlayers();

            if (!staffOnline.isEmpty()) {
                staffOnline.removeIf(player -> !Permissions.isStaff(player));
            }
            statisticalProgress = new StatisticalProgress(
                    mines,
                    logs,
                    kicks,
                    warnings,
                    punishments,
                    staffOnline
            );

            // Separator (Find False Positives)

            if (enoughData()) {
                double duration = 1000.0; // 1 second

                for (Enums.HackType hackType : hackTypes) {
                    hackType.getCheck().clearMaxCancelledViolations();

                    for (DataType dataType : dataTypes) {
                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            boolean bedrock = dataType == DataType.Bedrock;
                            Map<Integer, ViolationStatistics.TimePeriod> averages = new LinkedHashMap<>();

                            for (PlayerProfile playerProfile : playerProfiles) {
                                if (bedrock == playerProfile.isBedrockPlayer()
                                        && playerProfile.isLegitimate()) {
                                    Collection<PlayerViolation> list = playerProfile.getViolationHistory(hackType).getCollection();

                                    // Organize the violations into time period pieces
                                    if (!list.isEmpty()) {
                                        for (PlayerViolation playerViolation : list) {
                                            int timeMoment = AlgebraUtils.integerFloor(playerViolation.time / duration);
                                            ViolationStatistics.TimePeriod timePeriod = averages.get(timeMoment);

                                            if (timePeriod == null) {
                                                timePeriod = new ViolationStatistics.TimePeriod();
                                                timePeriod.add(playerProfile, playerViolation);
                                                averages.put(timeMoment, timePeriod);
                                            } else {
                                                timePeriod.add(playerProfile, playerViolation);
                                            }
                                        }
                                    }
                                }
                            }

                            if (!averages.isEmpty()) {
                                Map<Integer, Double> violationAverages = new LinkedHashMap<>();
                                Map<Integer, Integer> violationAveragesDivisor = new LinkedHashMap<>();

                                // Add the average violations for the executed detections based on each time period
                                for (ViolationStatistics.TimePeriod timePeriod : averages.values()) {
                                    for (Map.Entry<Integer, Double> individualViolationAverages : timePeriod.getAverageViolations().entrySet()) {
                                        int hash = individualViolationAverages.getKey();
                                        violationAverages.put(
                                                hash,
                                                violationAverages.getOrDefault(hash, 0.0) + individualViolationAverages.getValue()
                                        );
                                        violationAveragesDivisor.put(
                                                hash,
                                                violationAveragesDivisor.getOrDefault(hash, 0) + 1
                                        );
                                    }
                                }

                                // Calculate the average violations for each executed detection and multiply to get the bigger image
                                duration = Check.violationCycleMilliseconds / duration;

                                for (Map.Entry<Integer, Double> entry : violationAverages.entrySet()) {
                                    entry.setValue(entry.getValue() / violationAveragesDivisor.get(entry.getKey()) * duration);
                                }
                                hackType.getCheck().setMaxCancelledViolations(dataType, violationAverages);
                            } else {
                                hackType.getCheck().clearMaxCancelledViolations();
                            }
                        }
                    }
                }
            } else {
                for (Enums.HackType hackType : hackTypes) {
                    hackType.getCheck().clearMaxCancelledViolations();
                }
            }

            // Separator (Calculate mining statistics)

            if (!legitimatePlayers.isEmpty()) {
                for (Enums.MiningOre ore : Enums.MiningOre.values()) {
                    double average = 0.0, total = 0.0;

                    for (PlayerProfile playerProfile : legitimatePlayers) {
                        MiningHistory miningHistory = playerProfile.getMiningHistory(ore);
                        mines = miningHistory.getMines();

                        if (mines > 0) {
                            average += mines / ((double) miningHistory.getDays());
                            total++;
                        }
                    }

                    if (total > 0) {
                        average = Math.max(average / total, minimumAverageMining);
                        averageMining.put(ore, average);
                    } else {
                        averageMining.remove(ore);
                    }
                }

                // Separator

                CancelViolation.refresh(hackTypes); // Always at the end to include all the recently calculated/cached data
            } else {
                averageMining.clear();
                CancelViolation.clear(); // We clear because there are no hacker-free players
            }
        } else { // We clear because there are no players
            statisticalProgress = new StatisticalProgress();
            averageMining.clear();
            ViolationStatistics.clear();
            CancelViolation.clear();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearMaxCancelledViolations();
            }
        }
    }

    // Separator

    public enum DataType {
        Java, Bedrock, Universal;

        public final String lowerCase;

        DataType() {
            switch (this.ordinal()) {
                case 0:
                    lowerCase = "java";
                    break;
                case 1:
                    lowerCase = "bedrock";
                    break;
                default:
                    lowerCase = "universal";
                    break;
            }
        }
    }
}
