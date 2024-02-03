package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.checks.combat.criticals.Criticals;
import com.vagdedes.spartan.checks.combat.criticals.CriticalsUtils;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.moderation.BanManagement;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.gui.spartan.MainMenu;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.objects.features.StatisticalProgress;
import com.vagdedes.spartan.objects.profiling.*;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResearchEngine {

    public static final int
            maxDataLength = 4096,
            logRequirement = Check.maxViolationsPerCycle,
            profileRequirement = logRequirement / 10,
            maxRows = Cache.enoughRAM ? 100_000 : 20_000,
            maxBytes = maxRows * maxDataLength;

    public static final double downloadedVersion = Double.parseDouble(API.getVersion().substring(6));
    private static final double minimumAverageMining = 16.0;
    private static double
            averageReach = -1.0,
            averageCPS = -1.0;

    private static final long cacheTimeLimit = 60_000L * 4; // Despite being 4, realistically it is 5 minutes if extra calculations are accounted for
    public static final int
            cacheRefreshTicks = 1200,
            databaseRefreshTicks = cacheRefreshTicks * 10;
    private static int schedulerTicks = 0;

    private static boolean
            isCaching = false,
            isFull = false,
            enoughData = false;

    public static final DataType[] usableDataTypes = new DataType[]{DataType.Java, DataType.Bedrock};
    private static StatisticalProgress statisticalProgress = new StatisticalProgress();
    private static String date = new Timestamp(System.currentTimeMillis()).toString().substring(0, 10);

    private static final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>(Config.getMaxPlayers());
    private static final Map<String, ItemStack> skulls = new ConcurrentHashMap<>(logRequirement);
    private static final Map<Enums.MiningOre, Double> averageMining = new LinkedHashMap<>(Enums.MiningOre.values().length);
    private static final double[] defaultAverageMining = new double[Enums.MiningOre.values().length];
    private static final List<PlayerFight> playerFights = new LinkedList<>();

    private static final Runnable skullsAndFightsRunnable = () -> {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

        if (!playerProfiles.isEmpty()) {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                boolean added = !Config.settings.getBoolean("Important.load_player_head_textures")
                        || skulls.size() == logRequirement;
                Inventory inv = Bukkit.createInventory(players.get(0).getPlayer(), 9, "");

                for (PlayerProfile playerProfile : playerProfiles) {
                    playerProfile.getCombat().runFights();

                    if (!added && !playerProfile.isBedrockPlayer()) {
                        String name = playerProfile.getName();

                        if (!skulls.containsKey(name)) {
                            ItemStack skull = playerProfile.getSkull();
                            inv.addItem(skull);
                            skulls.put(name, skull);
                            added = true;
                        }
                    }
                }
            }
        }
    };

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
                    schedulerTicks = databaseRefreshTicks;

                    SpartanBukkit.analysisThread.executeIfFreeElseHere(() -> {
                        skullsAndFightsRunnable.run();

                        if (isDatabaseMode()) {
                            refresh(false, Register.isPluginEnabled());
                        }
                    });
                } else {
                    schedulerTicks -= 1;

                    if (schedulerTicks % 20 == 0) {
                        SpartanBukkit.analysisThread.executeIfFreeElseHere(() -> {
                            skullsAndFightsRunnable.run();

                            if (schedulerTicks % cacheRefreshTicks == 0) {
                                updateCache();
                            }
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
            return !TestServer.isIdentified();
        }
        Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

        if (playerProfiles.size() >= profileRequirement) {
            int logs = 0, profiles = 0;

            for (PlayerProfile playerProfile : playerProfiles) {
                if (!playerProfile.wasStaff()) {
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

    public static boolean isCaching() {
        return isCaching;
    }

    public static ItemStack getSkull(String name) {
        return skulls.getOrDefault(name, InventoryUtils.getHead());
    }

    // Separator

    public static void refresh(boolean message, boolean enabledPlugin) {
        if (!enabledPlugin || !isCaching()) {
            // Complete Storage
            AntiCheatLogs.refresh();
            Config.sql.refreshDatabase();
            Criticals.clear();

            if (enabledPlugin) {
                isCaching = true;

                Runnable runnable = () -> {
                    CloudFeature.refresh(true);
                    String status = recalculateCache();
                    CloudFeature.refresh(false);

                    if (message && status != null) {
                        AwarenessNotifications.forcefullySend(status);
                    }
                    isCaching = false;
                };

                if (!SpartanBukkit.isSynchronised()) { // Other Threads
                    runnable.run();
                } else { // Main Thread
                    SpartanBukkit.analysisThread.execute(runnable);
                }
            } else {
                isCaching = false;
                isFull = false;
                skulls.clear();
                CloudFeature.clear(true);
                CancelViolation.clear();
                ViolationStatistics.clear();
            }
        }
    }

    // Separator

    public static String getDetectionInformation(String s) {
        if (s.contains(Moderation.reportMessage)
                || s.contains(Moderation.kickMessage)
                || s.contains(Moderation.warningMessage)) {
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

    public static boolean hasCompetitiveCPS() {
        return averageCPS >= CombatUtils.competitiveCPS;
    }

    // Separator

    public static double getAverageCPS() {
        return averageCPS;
    }

    public static double getAverageReach() {
        return averageReach;
    }

    public static List<PlayerReport> getReports(Enums.HackType hackType, int days, boolean checkIfDismissed) {
        List<PlayerReport> reports = new LinkedList<>();

        boolean hasDays = days > 0,
                hasHackType = hackType != null;

        for (PlayerProfile playerProfile : ResearchEngine.playerProfiles.values()) {
            List<PlayerReport> playerReports = hasDays || hasHackType ?
                    playerProfile.getPunishmentHistory().getReports(hackType, days) :
                    playerProfile.getPunishmentHistory().getReports();

            if (checkIfDismissed) {
                for (PlayerReport playerReport : playerReports) {
                    if (!playerReport.isDismissed()) {
                        reports.add(playerReport);
                    }
                }
            } else {
                reports.addAll(playerReports);
            }
        }
        return reports;
    }

    public static StatisticalProgress getProgress() {
        return statisticalProgress;
    }

    public static List<PlayerFight> getFights() {
        return new ArrayList<>(playerFights);
    }

    // Separator

    public static List<PlayerProfile> getPunishedProfiles(boolean reports) {
        int size = playerProfiles.size();

        if (size > 0) {
            ArrayList<PlayerProfile> profiles = new ArrayList<>(size);

            for (PlayerProfile playerProfile : playerProfiles.values()) {
                if (playerProfile.getPunishmentHistory().getOverall(reports) > 0) {
                    profiles.add(playerProfile);
                }
            }
            return profiles;
        }
        return new ArrayList<>(0);
    }

    public static void addFight(PlayerFight fight, boolean refreshMenu) {
        playerFights.add(fight);

        if (refreshMenu) {
            MainMenu.refresh();
        }
    }

    public static List<PlayerProfile> getHackers() {
        if (!playerProfiles.isEmpty()) {
            List<PlayerProfile> list = new ArrayList<>(playerProfiles.size());

            for (PlayerProfile playerProfile : playerProfiles.values()) {
                if (playerProfile.isHacker()) {
                    list.add(playerProfile);
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

            for (PlayerProfile playerProfile : playerProfiles.values()) {
                if (playerProfile.isSuspected()) {
                    list.add(playerProfile);
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

            for (PlayerProfile playerProfile : playerProfiles.values()) {
                if (playerProfile.isLegitimate()) {
                    list.add(playerProfile);
                }
            }
            return list;
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<PlayerProfile> getPlayerProfiles() {
        return new ArrayList<>(playerProfiles.values());
    }

    // Separator

    public static PlayerProfile getPlayerProfile(String name) {
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile != null) {
            return playerProfile;
        }
        playerProfile = new PlayerProfile(name);
        playerProfiles.put(name, playerProfile);
        return playerProfile;
    }

    public static PlayerProfile getPlayerProfile(SpartanPlayer player) {
        String name = player.getName();
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile != null) {
            return playerProfile;
        }
        playerProfile = new PlayerProfile(player);
        playerProfiles.put(name, playerProfile);
        return playerProfile;
    }

    public static PlayerProfile getPlayerProfileAdvanced(String name, boolean deep) {
        PlayerProfile profile = playerProfiles.get(name);

        if (profile != null) {
            return profile;
        }
        if (deep) {
            Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

            if (!playerProfiles.isEmpty()) {
                for (PlayerProfile playerProfile : playerProfiles) {
                    if (playerProfile.getName().equalsIgnoreCase(name)) {
                        return playerProfile;
                    }
                }
            }
        }
        return null;
    }

    public static ViolationHistory getViolationHistory(Enums.HackType hackType, DataType dataType, Collection<PlayerProfile> profiles) {
        int size = profiles.size();

        if (size == 0) {
            return null;
        }
        List<PlayerViolation> violationsList = new ArrayList<>(size);
        Map<Integer, List<PlayerViolation>> violationsMap = new LinkedHashMap<>(size);
        Set<String> dates = new HashSet<>(size);
        boolean universal = dataType == DataType.Universal;

        // Separator
        for (PlayerProfile playerProfile : profiles) {
            if (universal || ((dataType == DataType.Bedrock) == playerProfile.isBedrockPlayer())) {
                ViolationHistory violationHistory = playerProfile.getViolationHistory(hackType);
                violationsMap.putAll(violationHistory.getViolationsMap());
                violationsList.addAll(violationHistory.getViolationsList());
                dates.addAll(violationHistory.getDates());
            }
        }

        if (violationsMap.isEmpty()) {
            return null;
        }
        return new ViolationHistory(hackType, dataType, violationsMap, violationsList, Math.max(dates.size(), 1));
    }

    // Separator

    public static double getMiningHistoryAverage(Enums.MiningOre ore, double multiplier) {
        Double average = averageMining.get(ore);
        return average == null ? defaultAverageMining[ore.ordinal()] : (average * multiplier);
    }

    // Separator

    public static void resetData(Enums.HackType hackType) {
        SpartanBukkit.storageThread.execute(() -> {
            while (isCaching()) {
                // Wait here until the caching is finished
            }
            String hackTypeString = hackType.toString();
            hackType.getCheck().clearMaxCancelledViolations();
            CancelViolation.clear(hackType);

            // Separator

            Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

            if (!playerProfiles.isEmpty()) {
                for (PlayerProfile playerProfile : playerProfiles) {
                    playerProfile.getViolationHistory(hackType).clear();
                    playerProfile.removeFromAllEvidence(hackType);
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
        if (playerName != null) {
            PlayerProfile playerProfile = playerProfiles.get(playerName);

            if (playerProfile != null) {
                if (isStorageMode()) {
                    // Clear Violations
                    SpartanPlayer p = SpartanBukkit.getPlayer(playerName);
                    boolean foundPlayer = p != null;

                    if (foundPlayer) {
                        for (Enums.HackType hackType : Enums.HackType.values()) {
                            p.getViolations(hackType).reset();
                        }
                    }

                    // Clear Files/Database
                    SpartanBukkit.storageThread.execute(() -> {
                        while (isCaching()) {
                            // Wait here until the caching is finished
                        }
                        playerProfiles.remove(playerName);

                        if (foundPlayer) {
                            PlayerProfile newPlayerProfile = getPlayerProfile(p);
                            p.setProfile(newPlayerProfile);
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
                    playerProfiles.remove(playerName);
                    SpartanPlayer p = SpartanBukkit.getPlayer(playerName);

                    if (p != null) {
                        PlayerProfile newPlayerProfile = getPlayerProfile(p);
                        p.setProfile(newPlayerProfile);
                    }
                }
                enoughData = false;
                MainMenu.refresh();
                SpartanMenu.playerInfo.refresh(playerName);
            }
        }
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
        if (IDs.isValid() && CloudFeature.canSynchroniseFiles(true)) {
            String crossServerInformationOption = CrossServerInformation.getOptionValue();

            if (CrossServerInformation.isOptionValid(crossServerInformationOption)) {
                String[] incomingInformation = CloudConnections.getCrossServerInformation("log", crossServerInformationOption);

                if (incomingInformation != null && incomingInformation.length > 0) {
                    String key = AntiCheatLogs.syntaxDate(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()), AlgebraUtils.randomInteger(1, Check.maxViolationsPerCycle - 1));

                    for (String information : incomingInformation) {
                        if (isLogValid(information)) {
                            AntiCheatLogs.logInfo(null, information, null, null, null, false, false, -1, -1);
                            cache.put(key, information);
                            byteSize += key.length() + information.length();

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

                            if (isLogValid(data)) {
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

                        if (data != null && isLogValid(data)) {
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

    public static int getOnlinePlayers(String data) {
        int startString = data.indexOf("(Online: ");

        if (startString > -1) {
            data = data.substring(startString + 9);
            int endString = data.indexOf(")");

            try {
                return Integer.parseInt(data.substring(0, endString));
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    public static boolean isLowTPS(String data) {
        int tpsString = data.indexOf("(TPS: ");

        if (tpsString > -1) {
            try {
                return Double.parseDouble(data.substring(tpsString + 6, tpsString + 11)) < TPS.minimum;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static boolean isLogValid(String log) {
        if (log.length() <= maxDataLength) {
            String start = "[Spartan Phase ";
            int index = log.indexOf(start);

            if (index > -1) {
                index += start.length();
                String number = log.substring(index, index + 3);

                if (AlgebraUtils.validNumber(number)) {
                    return (downloadedVersion - Double.parseDouble(number)) <= 25.0;
                }
            }
            return true;
        }
        return false;
    }

    // Separator

    private static String recalculateCache() {
        if (isStorageMode()) {
            Map<String, String> logs = getLogs();

            // Separator
            String toReturn;
            int size = logs.size();

            if (size > 0) {
                String falsePositiveDisclaimer = StringUtils.getClearColorString(Moderation.falsePositiveDisclaimer);

                try {
                    String construct = Config.getConstruct();
                    Map<String, Set<String>> miningDays = new LinkedHashMap<>(size);
                    Map<String, List<String>> dismissedReports = new LinkedHashMap<>(size);

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

                            if (data.contains(Moderation.criticalHitMessage)) {
                                if (split.length >= greatestSplitPosition) {
                                    Material material = Material.getMaterial(split[6].replace("-", "_").toUpperCase());

                                    if (material != null) {
                                        Double decimal = AlgebraUtils.returnValidDecimal(split[8]);

                                        if (decimal != null) {
                                            CriticalsUtils.cache(split[0], material, decimal);
                                        }
                                    }
                                }
                            } else if (data.contains(PlayerFight.finderKey)) {
                                int dataIndex = data.indexOf(PlayerFight.introductorySeparator);

                                if (dataIndex > -1) {
                                    dataIndex += 2;
                                    String[] statistics = data.substring(dataIndex).split(PlayerFight.majorSeparator);

                                    if (statistics.length > 0) {
                                        String[] information = data.split(" ", 7);
                                        String winner = information[0];
                                        String loser = information[5].replace(PlayerFight.introductorySeparator, ""); // Introductory separator is connected to the player's name

                                        boolean judged = false;
                                        long duration = 0L;
                                        long winnerHitTimeAverage = 0L, loserHitTimeAverage = 0L;
                                        int winnerHits = 0, loserHits = 0;
                                        int winnerMaxHitCombo = 0, loserMaxHitCombo = 0;
                                        int winnerMaxCPS = 0, loserMaxCPS = 0;
                                        double winnerCPS = 0.0, loserCPS = 0.0;
                                        double winnerReachAverage = 0.0, loserReachAverage = 0.0;
                                        double winnerYawRateAverage = 0.0f, loserYawRateAverage = 0.0f;
                                        double winnerPitchRateAverage = 0.0f, loserPitchRateAverage = 0.0f;

                                        for (String statistic : statistics) {
                                            String[] subStatistics = statistic.split(PlayerFight.intermediateSeparator[1]);
                                            int subStatisticsLength = subStatistics.length;

                                            if (subStatisticsLength >= 2) {
                                                statistic = subStatistics[0];
                                                String argument1, argument2;

                                                switch (statistic) {
                                                    case PlayerFight.outcomeKey:
                                                        if (subStatistics[1].equals(PlayerFight.outcome[0])) {
                                                            judged = true;
                                                        }
                                                        break;
                                                    case PlayerFight.durationKey:
                                                        argument1 = subStatistics[1];

                                                        if (AlgebraUtils.validInteger(argument1)) {
                                                            duration = Long.parseLong(argument1);
                                                        }
                                                        break;
                                                    case PlayerFight.hitsKey:
                                                        if (subStatisticsLength >= 3) {
                                                            argument1 = subStatistics[1];
                                                            argument2 = subStatistics[2];

                                                            if (AlgebraUtils.validInteger(argument1) && AlgebraUtils.validInteger(argument2)) {
                                                                winnerHits = Integer.parseInt(argument1);
                                                                loserHits = Integer.parseInt(argument2);
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.hitTimeAverageKey:
                                                        if (subStatisticsLength >= 3) {
                                                            argument1 = subStatistics[1];
                                                            argument2 = subStatistics[2];

                                                            if (AlgebraUtils.validInteger(argument1) && AlgebraUtils.validInteger(argument2)) {
                                                                winnerHitTimeAverage = Long.parseLong(argument1);
                                                                loserHitTimeAverage = Long.parseLong(argument2);
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.reachAverageKey:
                                                        if (subStatisticsLength >= 3) {
                                                            Double decimal1 = AlgebraUtils.returnValidDecimal(subStatistics[1]);

                                                            if (decimal1 != null) {
                                                                Double decimal2 = AlgebraUtils.returnValidDecimal(subStatistics[2]);

                                                                if (decimal2 != null) {
                                                                    winnerReachAverage = decimal1;
                                                                    loserReachAverage = decimal2;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.maxCpsKey:
                                                        if (subStatisticsLength >= 3) {
                                                            argument1 = subStatistics[1];
                                                            argument2 = subStatistics[2];

                                                            if (AlgebraUtils.validInteger(argument1) && AlgebraUtils.validInteger(argument2)) {
                                                                winnerMaxCPS = Integer.parseInt(argument1);
                                                                loserMaxCPS = Integer.parseInt(argument2);
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.cpsKey:
                                                        if (subStatisticsLength >= 3) {
                                                            Double decimal1 = AlgebraUtils.returnValidDecimal(subStatistics[1]);

                                                            if (decimal1 != null) {
                                                                Double decimal2 = AlgebraUtils.returnValidDecimal(subStatistics[2]);

                                                                if (decimal2 != null) {
                                                                    winnerCPS = decimal1;
                                                                    loserCPS = decimal2;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.yawRateKey:
                                                        if (subStatisticsLength >= 3) {
                                                            Double decimal1 = AlgebraUtils.returnValidDecimal(subStatistics[1]);

                                                            if (decimal1 != null) {
                                                                Double decimal2 = AlgebraUtils.returnValidDecimal(subStatistics[2]);

                                                                if (decimal2 != null) {
                                                                    winnerYawRateAverage = decimal1;
                                                                    loserYawRateAverage = decimal2;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.pitchRateKey:
                                                        if (subStatisticsLength >= 3) {
                                                            Double decimal1 = AlgebraUtils.returnValidDecimal(subStatistics[1]);

                                                            if (decimal1 != null) {
                                                                Double decimal2 = AlgebraUtils.returnValidDecimal(subStatistics[2]);

                                                                if (decimal2 != null) {
                                                                    winnerPitchRateAverage = decimal1;
                                                                    loserPitchRateAverage = decimal2;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.maxHitComboKey:
                                                        if (subStatisticsLength >= 3) {
                                                            argument1 = subStatistics[1];
                                                            argument2 = subStatistics[2];

                                                            if (AlgebraUtils.validInteger(argument1) && AlgebraUtils.validInteger(argument2)) {
                                                                winnerMaxHitCombo = Integer.parseInt(argument1);
                                                                loserMaxHitCombo = Integer.parseInt(argument2);
                                                            }
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        }

                                        if (duration > 0L
                                                && (winnerHits > 0 && loserHits > 0 && (winnerHits + loserHits) >= PlayerFight.requiredHits)
                                                & (winnerCPS >= 0 && loserCPS >= 0)
                                                && (winnerMaxCPS > 0 && loserMaxCPS > 0)
                                                && (winnerYawRateAverage > 0f && loserYawRateAverage > 0f)
                                                && (winnerPitchRateAverage > 0.0f && loserPitchRateAverage > 0.0f)
                                                && (winnerMaxHitCombo > 0 && loserMaxHitCombo > 0)
                                                && (winnerHitTimeAverage > 0L && loserHitTimeAverage > 0L)
                                                && (winnerReachAverage > 0.0 && loserReachAverage > 0.0)) {
                                            new PlayerFight(
                                                    new PlayerOpponent(winner, winnerHits, winnerMaxHitCombo, winnerMaxCPS, winnerCPS, duration,
                                                            winnerReachAverage, winnerHitTimeAverage, winnerYawRateAverage, winnerPitchRateAverage),
                                                    new PlayerOpponent(loser, loserHits, loserMaxHitCombo, loserMaxCPS, loserCPS, duration,
                                                            loserReachAverage, loserHitTimeAverage, loserYawRateAverage, loserPitchRateAverage),
                                                    judged
                                            );
                                        }
                                    }
                                }
                            } else if (data.contains(Moderation.warningMessage)) {
                                getPlayerProfile(split[0]).getPunishmentHistory().increaseWarnings();
                            } else if (data.contains(Moderation.kickMessage)) {
                                getPlayerProfile(split[0]).getPunishmentHistory().increaseKicks();
                            } else if (data.contains(BanManagement.message)) {
                                getPlayerProfile(split[0]).getPunishmentHistory().increaseBans();
                            } else if (data.contains(Moderation.reportMessage)) {
                                String name = split[0];
                                int wordIndex = data.indexOf("for");

                                if (wordIndex > -1) {
                                    getPlayerProfile(name).getPunishmentHistory().increaseReports(null, name, data.substring(wordIndex + 4), Timestamp.valueOf(fullDate.replace("/", "-")), false);
                                }
                            } else if (data.contains(Moderation.dismissedReportMessage)) {
                                if (split.length >= 4) {
                                    String name = split[3];
                                    int reasonIndex = data.indexOf("reason:");

                                    if (reasonIndex > -1) {
                                        String reason = data.substring(reasonIndex + 8);
                                        List<String> reports = dismissedReports.get(name);

                                        if (reports == null) {
                                            reports = new ArrayList<>();
                                            reports.add(reason);
                                            dismissedReports.put(name, reports);
                                        } else {
                                            reports.add(reason);
                                        }
                                    }
                                }
                            } else if (data.contains(" failed ")) {
                                if (split.length >= 3 && !data.contains("(VL: 0)")) {
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
                                                        playerProfile.getViolationHistory(hackType).increaseViolations(
                                                                new PlayerViolation(name, hackType, sdf.parse(fullDate).getTime(), detection, violation, isLowTPS(data))
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
                                                miningHistory.increaseMines(environment, 1);

                                                // Separator
                                                Set<String> days = miningDays.get(name);

                                                if (days == null) {
                                                    days = new HashSet<>();
                                                    days.add(partialDate);
                                                    miningDays.put(name, days);
                                                    miningHistory.increaseDays();
                                                } else if (days.add(partialDate)) {
                                                    miningHistory.increaseDays();
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }

                    // Cache Preporation
                    for (Map.Entry<String, List<String>> entry : dismissedReports.entrySet()) {
                        String name = entry.getKey();
                        PlayerProfile playerProfile = getPlayerProfile(name);

                        for (String reason : entry.getValue()) {
                            List<PlayerReport> reports = playerProfile.getPunishmentHistory().getReports();

                            if (!reports.isEmpty()) {
                                for (PlayerReport playerReport : reports) {
                                    if (playerReport.getReason().equals(reason)
                                            && playerReport.dismiss(name, null, false)) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    enoughData = false;
                    updateCache();

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

    private static void updateCache() {
        String currentDate = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDateTime.now());

        // When day changes
        if (!date.equals(currentDate)) {
            date = currentDate;
            Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

            if (!playerProfiles.isEmpty()) {
                for (PlayerProfile playerProfile : playerProfiles) {
                    for (MiningHistory miningHistory : playerProfile.getMiningHistory()) {
                        miningHistory.increaseDays();
                    }
                }
            }
        }
        if (!SpartanBukkit.isSynchronised()) {
            updateComprehensionCache(); // Always First
            updateCombatCache();
        }
        MainMenu.refresh();
    }

    private static void updateComprehensionCache() {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();
        int size = playerProfiles.size();

        if (size > 0) {
            Enums.HackType[] hackTypes = Enums.HackType.values();
            DataType[] dataTypes = getDynamicUsableDataTypes(false);
            List<PlayerProfile>
                    playerProfilesCopy = new ArrayList<>(playerProfiles),
                    legitimatePlayers = getLegitimatePlayers();
            int mines = 0, logs = 0, reports = 0,
                    bans = 0, kicks = 0, warnings = 0;
            ViolationStatistics.calculateData(playerProfiles);

            for (PlayerProfile playerProfile : playerProfiles) {
                playerProfile.getEvidence().judge();
                PunishmentHistory punishmentHistory = playerProfile.getPunishmentHistory();

                for (ViolationHistory violationHistory : playerProfile.getViolationHistory()) {
                    logs += playerProfile.getUsefulLogs(violationHistory);
                }
                mines += playerProfile.getOverallMiningHistory().getMines();
                reports += punishmentHistory.getReports().size();
                bans += punishmentHistory.getBans();
                kicks += punishmentHistory.getKicks();
                warnings += punishmentHistory.getWarnings();
            }

            // Separator

            int staffOffline = 0;
            List<SpartanPlayer> staffOnline = SpartanBukkit.getPlayers();

            if (!staffOnline.isEmpty()) {
                Iterator<SpartanPlayer> iterator = staffOnline.iterator();

                while (iterator.hasNext()) {
                    SpartanPlayer player = iterator.next();

                    if (Permissions.isStaff(player)) {
                        playerProfilesCopy.remove(player.getProfile());
                    } else {
                        iterator.remove();
                    }
                }
            }
            if (!playerProfilesCopy.isEmpty()) {
                for (PlayerProfile playerProfile : playerProfilesCopy) {
                    if (playerProfile.wasStaff()) {
                        staffOffline++;
                    }
                }
            }
            statisticalProgress = new StatisticalProgress(mines, logs, reports, bans, kicks, warnings, staffOffline, staffOnline.toArray(new SpartanPlayer[0]));

            // Separator (Find False Positives)

            if (enoughData()) {
                double duration = 1000.0; // 1 tick

                for (Enums.HackType hackType : hackTypes) {
                    hackType.getCheck().clearMaxCancelledViolations();

                    for (DataType dataType : dataTypes) {
                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            boolean bedrock = dataType == DataType.Bedrock;
                            Map<Integer, ViolationStatistics.TimePeriod> averages = new HashMap<>();

                            for (PlayerProfile playerProfile : playerProfiles) {
                                if (bedrock == playerProfile.isBedrockPlayer()
                                        && !playerProfile.wasStaff()
                                        && !playerProfile.wasTesting()
                                        && playerProfile.getEvidence().getCount() <= (Check.hackerCheckAmount - 1)) {
                                    List<PlayerViolation> list = playerProfile.getViolationHistory(hackType).getViolationsList();

                                    // Organize the violations into time period pieces
                                    if (!list.isEmpty()) {
                                        for (PlayerViolation playerViolation : list) {
                                            if (playerViolation.isDetectionEnabled()) {
                                                int timeMoment = AlgebraUtils.integerFloor(playerViolation.getTime() / duration);
                                                ViolationStatistics.TimePeriod timePeriod = averages.get(timeMoment);

                                                if (timePeriod == null) {
                                                    timePeriod = new ViolationStatistics.TimePeriod();
                                                    timePeriod.add(playerViolation);
                                                    averages.put(timeMoment, timePeriod);
                                                } else {
                                                    timePeriod.add(playerViolation);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!averages.isEmpty()) {
                                HashMap<Integer, Double> violationAverages = new HashMap<>();
                                HashMap<Integer, Integer> violationAveragesDivisor = new HashMap<>();

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
                                duration = Check.violationCycleSeconds / duration;

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
            playerFights.clear();
            statisticalProgress = new StatisticalProgress();
            averageMining.clear();
            ViolationStatistics.clear();
            CancelViolation.clear();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearMaxCancelledViolations();
            }
        }
    }

    private static void updateCombatCache() {
        List<PlayerProfile> legitimatePlayers = getLegitimatePlayers();
        double averageCPS = 0.0, averageCPSCount = 0.0,
                averageReach = 0.0, averageReachCount = 0.0;

        if (legitimatePlayers.size() >= profileRequirement) {
            List<PlayerFight> playerFights = new LinkedList<>();

            for (PlayerProfile playerProfile : legitimatePlayers) {
                PlayerCombat combat = playerProfile.getCombat();

                if (combat.hasFights()) {
                    playerFights.addAll(combat.getPastFights());
                    double[] list = combat.getMaxCPSAverages();

                    if (list != null) {
                        averageCPSCount += 1.0;
                        averageCPS += list[0];
                    }
                    list = combat.getReachAverages();

                    if (list != null) {
                        averageReachCount += 1.0;
                        averageReach += list[0];
                    }
                }
            }
            ResearchEngine.playerFights.clear();

            if (playerFights.size() >= profileRequirement) {
                ResearchEngine.playerFights.addAll(playerFights);

                if (averageCPSCount > 0.0 && averageCPS <= CombatUtils.maxLegitimateCPS) {
                    ResearchEngine.averageCPS = averageCPS / averageCPSCount;
                } else {
                    ResearchEngine.averageCPS = -1.0;
                }

                if (averageReachCount > 0.0 && averageCPS <= CombatUtils.maxHitDistance) {
                    ResearchEngine.averageReach = averageReach / averageReachCount;
                } else {
                    ResearchEngine.averageReach = -1.0;
                }
            } else {
                ResearchEngine.averageCPS = -1.0;
                ResearchEngine.averageReach = -1.0;
            }
        } else {
            ResearchEngine.playerFights.clear();
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
