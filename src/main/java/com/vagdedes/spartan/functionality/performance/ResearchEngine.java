package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.pattern.Pattern;
import com.vagdedes.spartan.abstraction.profiling.*;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResearchEngine {

    public static final double requiredProfiles = 10;
    public static final int violationsMeasurementDuration = 60_000;
    private static final double minimumAverageMining = 16.0;

    private static final int cacheRefreshTicks = 1200;
    private static int schedulerTicks = 0;

    private static boolean enoughData = false;

    public static final Enums.DataType[] usableDataTypes = new Enums.DataType[]{Enums.DataType.JAVA, Enums.DataType.BEDROCK};
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
                        if (Config.sql.isEnabled()) {
                            refresh(Register.isPluginEnabled());
                        } else if (schedulerTicks % cacheRefreshTicks == 0) {
                            updateCache();
                            MainMenu.refresh();
                        }
                    });
                } else {
                    schedulerTicks -= 1;

                    if (schedulerTicks % cacheRefreshTicks == 0) {
                        SpartanBukkit.analysisThread.executeIfFree(() -> {
                            updateCache();
                            MainMenu.refresh();
                        });
                    }
                }
            }, 1L, 1L);
        }
    }

    public static boolean isStorageMode() {
        return Config.settings.getBoolean("Logs.log_file") || Config.sql.isEnabled();
    }

    public static Enums.DataType[] getDynamicUsableDataTypes(boolean universal) {
        Enums.DataType dataType = SpartanEdition.getMissingDetection();
        return dataType == null ? (universal ? Enums.DataType.values() : usableDataTypes) :
                dataType == Enums.DataType.BEDROCK ? (universal ? new Enums.DataType[]{Enums.DataType.UNIVERSAL, Enums.DataType.JAVA} : new Enums.DataType[]{Enums.DataType.JAVA}) :
                        (universal ? new Enums.DataType[]{Enums.DataType.UNIVERSAL, Enums.DataType.BEDROCK} : new Enums.DataType[]{Enums.DataType.BEDROCK});
    }

    // Separator

    public static boolean enoughData() {
        if (enoughData) {
            return true;
        } else if (playerProfiles.size() >= requiredProfiles) {
            int profiles = 0;

            synchronized (playerProfiles) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    if (playerProfile.isLegitimate()) {
                        profiles++;

                        if (profiles >= requiredProfiles) {
                            return enoughData = true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Separator

    public static String getDetectionInformation(String s) {
        String[] split = s.split("\\), \\(");
        s = split[split.length - 1];
        return s.length() < 2 ? null : s.substring(0, s.length() - 2);
    }

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
        PlayerProfile playerProfile;

        synchronized (playerProfiles) {
            playerProfile = new PlayerProfile(player);
            playerProfiles.put(player.name, playerProfile);
        }
        return playerProfile;
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

    // Separator

    public static ViolationHistory getViolationHistory(Enums.HackType hackType, Enums.DataType dataType, Collection<PlayerProfile> profiles) {
        int size = profiles.size();

        if (size == 0) {
            return null;
        }
        Collection<PlayerViolation> list = new ArrayList<>(size);
        boolean universal = dataType == Enums.DataType.UNIVERSAL;

        // Separator
        for (PlayerProfile playerProfile : profiles) {
            if (universal || ((dataType == Enums.DataType.BEDROCK) == playerProfile.isBedrockPlayer())) {
                list.addAll(playerProfile.getViolationHistory(hackType).getRawCollection());
            }
        }

        if (list.isEmpty()) {
            return null;
        }
        return new ViolationHistory(list, false);
    }

    public static double getMiningHistoryAverage(Enums.MiningOre ore) {
        Double average = averageMining.get(ore);
        return average == null ? defaultAverageMining[ore.ordinal()] : average;
    }

    // Separator

    public static void resetData(Enums.HackType hackType) {
        SpartanBukkit.analysisThread.executeWithPriority(() -> {
            String hackTypeString = hackType.toString();
            hackType.getCheck().clearIgnoredViolations();

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
        PlayerProfile profile;

        if (foundPlayer) {
            profile = p.getProfile();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                p.getViolations(hackType).reset();
            }
        } else {
            profile = getPlayerProfile(playerName);
        }
        Pattern.deleteFromFile(profile);

        if (isStorageMode()) {
            // Clear Files/Database
            SpartanBukkit.analysisThread.executeWithPriority(() -> {
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
        InteractiveInventory.playerInfo.refresh(playerName);
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

    public static Map<String, String> getLogs() {
        Map<String, String> cache = new LinkedHashMap<>();
        int byteSize = 0;
        boolean isFull = false,
                continueWithYAML = false;

        // Separator

        if (Config.sql.isEnabled()) {
            if (!isFull) {
                try {
                    ResultSet rs = Config.sql.query("SELECT creation_date, information FROM " + Config.sql.getTable() + " ORDER BY id DESC LIMIT " + Cache.maxRows + ";");

                    if (rs != null) {
                        while (rs.next()) {
                            String data = rs.getString("information");
                            String[] all = TimeUtils.getAll(rs.getTimestamp("creation_date"));
                            String date = "(" + new Random().nextInt() + ")[" + all[0] + " " + all[1] + "]";
                            cache.put(date, data);
                            byteSize += date.length() + data.length();

                            if (byteSize >= Cache.maxBytes) {
                                isFull = true;
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

                            if (byteSize >= Cache.maxBytes) {
                                isFull = true;
                                break;
                            }
                        }
                    }
                    if (isFull) {
                        break;
                    }
                }
            }
        }
        return cache;
    }

    // Separator

    public static void refresh(boolean enabledPlugin) {
        Runnable runnable = () -> {
            // Complete Storage
            AntiCheatLogs.refresh();
            Config.sql.refreshDatabase();

            if (enabledPlugin) {
                CloudBase.refresh(true);
                buildCache();
                Pattern.reload();
                CloudBase.refresh(false);
            } else {
                CloudBase.clear(true);
                Pattern.clear();
                ViolationAnalysis.clear();
            }
        };

        if (SpartanBukkit.isSynchronised()) {
            SpartanBukkit.analysisThread.executeIfFree(runnable);
        } else {
            runnable.run();
        }
    }

    private static void buildCache() {
        if (isStorageMode()) {
            Map<String, String> logs = getLogs();

            // Separator
            int size = logs.size();

            if (size > 0) {
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

                            if (data.contains(PunishmentHistory.warningMessage)) {
                                getPlayerProfile(split[0]).punishmentHistory.increaseWarnings(null, null);
                            } else if (data.contains(PunishmentHistory.kickMessage)) {
                                getPlayerProfile(split[0]).punishmentHistory.increaseKicks(null, null);
                            } else if (data.contains(PunishmentHistory.punishmentMessage)) {
                                getPlayerProfile(split[0]).punishmentHistory.increasePunishments(null, null);
                            } else if (data.contains(" failed ")) {
                                if (split.length >= 3) {
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
                                                            new PlayerViolation(sdf.parse(fullDate).getTime(), hackType, detection, violation)
                                                    );
                                                }
                                            }
                                            break;
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
                    updateCache();
                    MainMenu.refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void updateCache() {
        if (!playerProfiles.isEmpty()) {
            Collection<PlayerProfile> playerProfiles;

            synchronized (ResearchEngine.playerProfiles) {
                playerProfiles = new ArrayList<>(ResearchEngine.playerProfiles.values());
            }
            Enums.HackType[] hackTypes = Enums.HackType.values();
            Enums.DataType[] dataTypes = getDynamicUsableDataTypes(false);
            List<PlayerProfile> legitimatePlayers = getLegitimatePlayers();
            int mines = 0, kicks = 0, warnings = 0, punishments = 0;
            ViolationAnalysis.calculateData(playerProfiles);

            for (PlayerProfile playerProfile : playerProfiles) {
                playerProfile.evidence.judge();
                PunishmentHistory punishmentHistory = playerProfile.punishmentHistory;
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
                    kicks,
                    warnings,
                    punishments,
                    staffOnline
            );

            // Separator

            if (enoughData()) {
                double analysisDuration = 1_000.0; // 1 second

                for (Enums.HackType hackType : hackTypes) {
                    hackType.getCheck().clearIgnoredViolations();

                    for (Enums.DataType dataType : dataTypes) {
                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            boolean bedrock = dataType == Enums.DataType.BEDROCK;
                            Map<Integer, ViolationAnalysis.TimePeriod> averages = new LinkedHashMap<>();

                            for (PlayerProfile playerProfile : playerProfiles) {
                                if (bedrock == playerProfile.isBedrockPlayer()
                                        && playerProfile.isLegitimate()) {
                                    Collection<PlayerViolation> list = playerProfile.getViolationHistory(hackType).getCollection();

                                    // Organize the violations into time period pieces
                                    if (!list.isEmpty()) {
                                        for (PlayerViolation playerViolation : list) {
                                            int timeMoment = AlgebraUtils.integerFloor(playerViolation.time / analysisDuration);
                                            ViolationAnalysis.TimePeriod timePeriod = averages.get(timeMoment);

                                            if (timePeriod == null) {
                                                timePeriod = new ViolationAnalysis.TimePeriod();
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
                                for (ViolationAnalysis.TimePeriod timePeriod : averages.values()) {
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
                                analysisDuration = violationsMeasurementDuration / analysisDuration;

                                for (Map.Entry<Integer, Double> entry : violationAverages.entrySet()) {
                                    entry.setValue(entry.getValue() / violationAveragesDivisor.get(entry.getKey()) * analysisDuration);
                                }
                                hackType.getCheck().setIgnoredViolations(dataType, violationAverages);
                            } else {
                                hackType.getCheck().clearIgnoredViolations();
                            }
                        }
                    }
                }
            } else {
                for (Enums.HackType hackType : hackTypes) {
                    hackType.getCheck().clearIgnoredViolations();
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
            } else {
                averageMining.clear();
            }
        } else { // We clear because there are no players
            statisticalProgress = new StatisticalProgress();
            averageMining.clear();
            ViolationAnalysis.clear();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearIgnoredViolations();
            }
        }
    }
}
