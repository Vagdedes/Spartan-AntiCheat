package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.checks.combat.criticals.Criticals;
import me.vagdedes.spartan.checks.combat.criticals.CriticalsUtils;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.functionality.moderation.BanManagement;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.gui.SpartanMenu;
import me.vagdedes.spartan.gui.spartan.MainMenu;
import me.vagdedes.spartan.handlers.connection.IDs;
import me.vagdedes.spartan.objects.features.StatisticalProgress;
import me.vagdedes.spartan.objects.profiling.*;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.TimeUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
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

    // Constants
    public static final int
            maxDataLength = 4096,
            logRequirement = Check.maxViolationsPerCycle,
            profileRequirement = logRequirement / 10;
    public static final double downloadedVersion = Double.parseDouble(API.getVersion().substring(6));
    public static final int
            maxSize = Cache.enoughRAM ? 102_400 : 20_480,
            maxSizeInBytes = maxSize * 1024;
    public static final DataType[] usableDataTypes = new DataType[]{DataType.Java, DataType.Bedrock};
    private static final double minimumAverageMining = 16.0;
    // Base
    private static final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>(Config.getMaxPlayers());
    private static final Map<String, ItemStack> skulls = new ConcurrentHashMap<>(logRequirement);
    // Detections
    private static final Map<String, Double> globalVelocityOccurrences = new LinkedHashMap<>();

    // Averages
    private static final Map<Enums.MiningOre, Double>
            averageMining = new LinkedHashMap<>(Enums.MiningOre.values().length),
            defaultAverageMining = new LinkedHashMap<>(Enums.MiningOre.values().length);
    private static final double[]
            reach = new double[3],
            hitTime = new double[3],
            yawRate = new double[3],
            pitchRate = new double[3],
            cps = new double[3],
            winsToLosses = new double[3],
            hitRatio = new double[3],
            hitCombo = new double[3],
            duration = new double[3];

    // Lists
    private static final List<PlayerProfile>
            hackerPlayers = new LinkedList<>(),
            legitimatePlayers = new LinkedList<>(),
            suspectedPlayers = new LinkedList<>(),
            hackerFreePlayerProfiles = new ArrayList<>(Config.getMaxPlayers());
    private static final List<PlayerFight> playerFights = new LinkedList<>();
    private static final int schedulerRefreshTicks = 1200 * 10;
    private static final long cacheTimeLimit = 60_000L * 4; // Despite being 4, realistically it is 5 minutes if extra calculations are accounted for
    private static final Runnable skullsAndFightsRunnable = () -> {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

        if (playerProfiles.size() > 0) {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (players.size() > 0) {
                boolean added = !Config.settings.getBoolean("Important.load_player_head_textures") || skulls.size() == logRequirement;
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
    private static boolean
            isCaching = false,
            isFull = false,
            enoughData = false;
    private static StatisticalProgress statisticalProgress = new StatisticalProgress();
    // Scheduler
    private static int schedulerTicks = 0;
    private static String date = new Timestamp(System.currentTimeMillis()).toString().substring(0, 10);

    static {
        for (int i = 0; i < 3; i++) {
            reach[i] = -1.0;
            hitTime[i] = -1.0;
            yawRate[i] = -1.0;
            pitchRate[i] = -1.0;
            cps[i] = -1.0;
            winsToLosses[i] = -1.0;
            hitCombo[i] = -1.0;
            duration[i] = -1.0;
            hitRatio[i] = -1.0;
        }
        for (Enums.MiningOre ore : Enums.MiningOre.values()) {
            switch (ore) {
                case DIAMOND:
                    defaultAverageMining.put(ore, -32.0);
                    break;
                case EMERALD:
                case ANCIENT_DEBRIS:
                    defaultAverageMining.put(ore, -minimumAverageMining);
                    break;
                case GOLD:
                    defaultAverageMining.put(ore, -64.0);
                    break;
                default:
                    break;
            }
        }

        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (schedulerTicks == 0) {
                    schedulerTicks = schedulerRefreshTicks;

                    SpartanBukkit.analysisThread.executeIfFreeElseHere(() -> {
                        skullsAndFightsRunnable.run();
                        updateCache();

                        if (isDatabaseMode()) {
                            refresh(false, Register.isPluginEnabled());
                        }
                    });
                } else {
                    schedulerTicks -= 1;

                    if (schedulerTicks % 20 == 0) {
                        SpartanBukkit.analysisThread.executeIfFreeElseHere(() -> {
                            skullsAndFightsRunnable.run();

                            if (schedulerTicks % 1200 == 0) {
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

            if (index2 > -1 && index2 > index1 && s.length() > index2) {
                String number = s.substring(index1 + 5, index2);
                return AlgebraUtils.validInteger(number) ? Integer.parseInt(number) : -1;
            }
        }
        return -1;
    }

    public static boolean hasCompetitiveCPS() {
        return cps[1] >= CombatUtils.competitiveCPS;
    }

    // Separator

    public static double[] getCPS() {
        return cps;
    }

    public static double[] getReach() {
        return reach;
    }

    public static double[] getDuration() {
        return duration;
    }

    public static double[] getHitCombo() {
        return hitCombo;
    }

    public static double[] getWinsToLosses() {
        return winsToLosses;
    }

    public static double[] getHitRatio() {
        return hitRatio;
    }

    public static double[] getHitTime() {
        return hitTime;
    }

    public static double[] getYawRate() {
        return yawRate;
    }

    public static double[] getPitchRate() {
        return pitchRate;
    }

    public static double getGlobalVelocityOccurrences(float desiredValue, int desiredCount, boolean vertical) {
        if (hackerFreePlayerProfiles.size() >= profileRequirement) {
            String key = desiredValue + (vertical ? "v" : "h") + desiredCount;
            Double percentage = globalVelocityOccurrences.get(key);

            if (percentage != null) {
                return percentage;
            }
            int valid = 0, total = 0;

            for (PlayerProfile playerProfile : hackerFreePlayerProfiles) { // Include all players including online but not offline players to get an idea of how percentages shift
                PlayerCombat combat = playerProfile.getCombat();

                if (combat.hasEnoughFights()) {
                    double occurrences = combat.getVelocityOccurrences(desiredValue, desiredCount, vertical);

                    if (occurrences != -1.0) {
                        valid += occurrences;
                        total++;
                    }
                }
            }

            if (total >= profileRequirement) {
                percentage = valid / ((double) total);
                globalVelocityOccurrences.put(key, percentage);
                return percentage;
            }
            globalVelocityOccurrences.put(key, -1.0);
        }
        return -1.0;
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

    public static void addHacker(PlayerProfile playerProfile) {
        if (!hackerPlayers.contains(playerProfile)) {
            hackerPlayers.add(playerProfile);
            hackerFreePlayerProfiles.remove(playerProfile);
            legitimatePlayers.remove(playerProfile);
            suspectedPlayers.remove(playerProfile);
        }
    }

    public static List<PlayerProfile> getHackers() {
        return new ArrayList<>(hackerPlayers);
    }

    public static void addSuspected(PlayerProfile playerProfile) {
        if (!suspectedPlayers.contains(playerProfile)) {
            suspectedPlayers.add(playerProfile);
            hackerFreePlayerProfiles.remove(playerProfile);
            legitimatePlayers.remove(playerProfile);
            hackerPlayers.remove(playerProfile);
        }
    }

    public static List<PlayerProfile> getSuspectedPlayers() {
        return new ArrayList<>(suspectedPlayers);
    }

    public static List<PlayerProfile> getLegitimatePlayers() {
        return new ArrayList<>(legitimatePlayers);
    }

    public static void addLegitimate(PlayerProfile playerProfile) {
        if (!legitimatePlayers.contains(playerProfile)) {
            legitimatePlayers.add(playerProfile);

            if (playerProfile.isTrustWorthy(true)) {
                hackerFreePlayerProfiles.add(playerProfile);
            }
            suspectedPlayers.remove(playerProfile);
            hackerPlayers.remove(playerProfile);
        }
    }

    public static List<PlayerProfile> getPlayerProfiles() {
        return new ArrayList<>(playerProfiles.values());
    }

    // Separator

    public static PlayerProfile getPlayerProfile(String name) {
        if (name == null) {
            return new PlayerProfile();
        }
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile != null) {
            return playerProfile;
        }
        playerProfile = new PlayerProfile(name);
        playerProfiles.put(name, playerProfile);
        addLegitimate(playerProfile);
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
        addLegitimate(playerProfile);
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

    public static ViolationHistory getViolationHistory(Enums.HackType hackType, DataType dataType, boolean legitimate) {
        Collection<PlayerProfile> profiles = legitimate ? hackerFreePlayerProfiles : playerProfiles.values();
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

        if (violationsMap.size() == 0) {
            return null;
        }
        return new ViolationHistory(hackType, dataType, violationsMap, violationsList, Math.max(dates.size(), 1));
    }

    // Separator

    public static double getMiningHistoryAverage(Enums.MiningOre ore, double multiplier) {
        Double average = averageMining.get(ore);
        return average == null ? defaultAverageMining.get(ore) : (average * multiplier);
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

            if (playerProfiles.size() > 0) {
                for (PlayerProfile playerProfile : playerProfiles) {
                    playerProfile.getViolationHistory(hackType).clear();
                    playerProfile.getEvidence().remove(hackType);
                }
            }

            // Separator

            if (Config.sql.isEnabled()) {
                Config.sql.update("DELETE FROM " + Config.sql.getTable() + " WHERE functionality = '" + hackTypeString + "';");
            }

            // Separator

            Collection<File> files = getFiles();

            if (files.size() > 0) {
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
            ViolationStatistics.remove(hackType);
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
                        hackerFreePlayerProfiles.remove(playerProfile);
                        hackerPlayers.remove(playerProfile);
                        suspectedPlayers.remove(playerProfile);
                        legitimatePlayers.remove(playerProfile);
                        ViolationStatistics.remove(playerProfile);

                        if (foundPlayer) {
                            PlayerProfile newPlayerProfile = getPlayerProfile(p);
                            hackerFreePlayerProfiles.add(newPlayerProfile);
                            legitimatePlayers.add(newPlayerProfile);
                            p.setProfile(newPlayerProfile);
                        }
                        if (Config.sql.isEnabled()) {
                            Config.sql.update("DELETE FROM " + Config.sql.getTable() + " WHERE information LIKE '%" + playerName + "%';");
                        }
                        Collection<File> files = getFiles();

                        if (files.size() > 0) {
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
                    hackerFreePlayerProfiles.remove(playerProfile);
                    hackerPlayers.remove(playerProfile);
                    suspectedPlayers.remove(playerProfile);
                    legitimatePlayers.remove(playerProfile);
                    ViolationStatistics.remove(playerProfile);
                    SpartanPlayer p = SpartanBukkit.getPlayer(playerName);

                    if (p != null) {
                        PlayerProfile newPlayerProfile = getPlayerProfile(p);
                        hackerFreePlayerProfiles.add(newPlayerProfile);
                        legitimatePlayers.add(newPlayerProfile);
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

                            if (byteSize >= maxSizeInBytes) {
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
                    ResultSet rs = Config.sql.query("SELECT creation_date, information FROM " + Config.sql.getTable() + " ORDER BY id DESC LIMIT " + maxSize + ";");

                    if (rs != null) {
                        while (rs.next()) {
                            String data = rs.getString("information");

                            if (isLogValid(data)) {
                                Timestamp t = rs.getTimestamp("creation_date");
                                String date = "(" + AlgebraUtils.randomInteger(1, Check.maxViolationsPerCycle - 1) + ")[" + TimeUtils.getYearMonthDay(t) + " " + TimeUtils.getTime(t) + "]";
                                cache.put(date, data);
                                byteSize += date.length() + data.length();

                                if (byteSize >= maxSizeInBytes) {
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

                        if (cache.size() == 0) {
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

            if (files.size() > 0) {
                //Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

                for (File file : files) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

                    for (String key : c.getKeys(false)) {
                        String data = c.getString(key);

                        if (data != null && isLogValid(data)) {
                            cache.put(key, data);
                            byteSize += key.length() + data.length();

                            if (byteSize >= maxSizeInBytes) {
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
                                        int winnerHits = 0, loserHits = 0;
                                        int winnerMaxHitCombo = 0, loserMaxHitCombo = 0;
                                        int winnerCPS = 0, loserCPS = 0;
                                        long winnerHitTimeAverage = 0L, loserHitTimeAverage = 0L;
                                        double winnerReachAverage = 0.0, loserReachAverage = 0.0;
                                        float winnerYawRateAverage = 0.0f, loserYawRateAverage = 0.0f;
                                        float winnerPitchRateAverage = 0.0f, loserPitchRateAverage = 0.0f;
                                        Map<Integer, Float[]>
                                                winnerVerticalVelocity = new LinkedHashMap<>(),
                                                loserVerticalVelocity = new LinkedHashMap<>(),
                                                winnerHorizontalVelocity = new LinkedHashMap<>(),
                                                loserHorizontalVelocity = new LinkedHashMap<>();

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
                                                                winnerCPS = Integer.parseInt(argument1);
                                                                loserCPS = Integer.parseInt(argument2);
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.yawRateKey:
                                                        if (subStatisticsLength >= 3) {
                                                            Float decimal1 = AlgebraUtils.returnValidFloat(subStatistics[1]);

                                                            if (decimal1 != null) {
                                                                Float decimal2 = AlgebraUtils.returnValidFloat(subStatistics[2]);

                                                                if (decimal2 != null) {
                                                                    winnerYawRateAverage = decimal1;
                                                                    loserYawRateAverage = decimal2;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.pitchRateKey:
                                                        if (subStatisticsLength >= 3) {
                                                            Float decimal1 = AlgebraUtils.returnValidFloat(subStatistics[1]);

                                                            if (decimal1 != null) {
                                                                Float decimal2 = AlgebraUtils.returnValidFloat(subStatistics[2]);

                                                                if (decimal2 != null) {
                                                                    winnerPitchRateAverage = decimal1;
                                                                    loserPitchRateAverage = decimal2;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    case PlayerFight.oldVerticalVelocityKey: // Attention, added after initial algorithm (2)
                                                    case PlayerFight.verticalVelocityKey: // Attention, added after initial algorithm (3)
                                                    case PlayerFight.horizontalVelocityKey: // Attention, added after initial algorithm (3)
                                                        if (subStatisticsLength >= 3) {
                                                            boolean horizontal = statistic.equals(PlayerFight.horizontalVelocityKey);
                                                            boolean winnerCalculation = true; // Use to judge which data collection to fulfill

                                                            for (String velocityKeys : new String[]{subStatistics[1], subStatistics[2]}) { // Get both winner & loser velocity data to prevent code repetition
                                                                String[] velocityStatistics = velocityKeys.split(PlayerFight.velocityMajorSeparator); // Split the data into their respective collections

                                                                if (velocityStatistics.length > 0) { // Check if the split resulted in valid collections
                                                                    for (String velocityStatistic : velocityStatistics) {
                                                                        String[] velocitySubStatistics = velocityStatistic.split(PlayerFight.velocityMinorSeparator); // Split the collections into their respective variables
                                                                        int velocitySubStatisticsSize = velocitySubStatistics.length;

                                                                        if (velocitySubStatisticsSize > 0) { // Check if the split resulted in valid variables
                                                                            List<Float> floats = new ArrayList<>(velocitySubStatisticsSize);

                                                                            for (String velocitySubStatistic : velocitySubStatistics) {
                                                                                Float validFloat = AlgebraUtils.returnValidFloat(velocitySubStatistic);

                                                                                if (validFloat != null) { // Check if the variable is a valid decimal
                                                                                    floats.add(validFloat);
                                                                                }
                                                                            }

                                                                            if (winnerCalculation) {
                                                                                Map<Integer, Float[]> winnerMap = horizontal ? winnerHorizontalVelocity : winnerVerticalVelocity;
                                                                                winnerMap.put(winnerMap.size(), floats.toArray(new Float[0]));
                                                                            } else {
                                                                                Map<Integer, Float[]> loserMap = horizontal ? loserHorizontalVelocity : loserVerticalVelocity;
                                                                                loserMap.put(loserMap.size(), floats.toArray(new Float[0]));
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                winnerCalculation = false;
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
                                                && (winnerCPS >= 0 && loserCPS >= 0) // Attention, added after initial algorithm (1)
                                                && (winnerYawRateAverage >= 0f && loserYawRateAverage >= 0f) // Attention, added after initial algorithm (3)
                                                && (winnerPitchRateAverage >= 0.0f && loserPitchRateAverage >= 0.0f) // Attention, added after initial algorithm (3)
                                                && (winnerMaxHitCombo >= 0 && loserMaxHitCombo >= 0) // Attention, added after initial algorithm (4)
                                                && (winnerHitTimeAverage > 0L && loserHitTimeAverage > 0L)
                                                && (winnerReachAverage > 0.0 && loserReachAverage > 0.0)) {
                                            PlayerOpponent winnerOpponent = new PlayerOpponent(winner, winnerHits, winnerMaxHitCombo, winnerCPS, duration,
                                                    winnerReachAverage, winnerHitTimeAverage, winnerYawRateAverage, winnerPitchRateAverage,
                                                    new PlayerVelocity(winnerVerticalVelocity, winnerHorizontalVelocity));
                                            PlayerOpponent loserOpponent = new PlayerOpponent(loser, loserHits, loserMaxHitCombo, loserCPS, duration,
                                                    loserReachAverage, loserHitTimeAverage, loserYawRateAverage, loserPitchRateAverage,
                                                    new PlayerVelocity(loserVerticalVelocity, loserHorizontalVelocity));
                                            new PlayerFight(winnerOpponent, loserOpponent, judged);
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

                            if (reports.size() > 0) {
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
        if (!SpartanBukkit.isSynchronised()) {
            updateComprehensionCache(); // Always First
            updateCombatCache();
        }
        String currentDate = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDateTime.now());

        // When day changes
        if (!date.equals(currentDate)) {
            date = currentDate;
            Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();

            if (playerProfiles.size() > 0) {
                for (PlayerProfile playerProfile : playerProfiles) {
                    for (ViolationHistory violationHistory : playerProfile.getViolationHistory()) {
                        violationHistory.clearDays();
                    }
                    for (MiningHistory miningHistory : playerProfile.getMiningHistory()) {
                        miningHistory.increaseDays();
                    }
                }
            }
        }
        MainMenu.refresh();
    }

    private static void updateComprehensionCache() {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.playerProfiles.values();
        int size = playerProfiles.size();

        if (size > 0) {
            Enums.HackType[] hackTypes = Enums.HackType.values();
            DataType[] dataTypes = getDynamicUsableDataTypes(false);
            List<PlayerProfile> playerProfilesCopy = new ArrayList<>(playerProfiles);
            int mines = 0, logs = 0, reports = 0,
                    bans = 0, kicks = 0, warnings = 0;
            ViolationStatistics.calculate(playerProfiles);

            for (PlayerProfile playerProfile : playerProfiles) {
                playerProfile.calculateHistoricalEvidence();

                // Separator

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

            for (Enums.HackType hackType : hackTypes) {
                hackType.getCheck().clearMaxCancelledViolations();

                for (DataType dataType : dataTypes) {
                    if (hackType.getCheck().isEnabled(dataType, null, null)) {
                        boolean bedrock = dataType == DataType.Bedrock;
                        Map<Long, Collection<PlayerViolation>> orderedMap = new TreeMap<>();

                        for (PlayerProfile playerProfile : playerProfiles) {
                            if (bedrock == playerProfile.isBedrockPlayer()
                                    && playerProfile.isTrustWorthy(false)
                                    && playerProfile.getEvidence().getCount() <= (Check.hackerCheckAmount - 1)) {
                                List<PlayerViolation> list = playerProfile.getViolationHistory(hackType).getViolationsList();

                                if (!list.isEmpty()) {
                                    for (PlayerViolation playerViolation : list) {
                                        if (playerViolation.isDetectionEnabled()) {
                                            long time = playerViolation.getTime();
                                            Collection<PlayerViolation> playerViolations = orderedMap.get(time);

                                            if (playerViolations == null) {
                                                playerViolations = new ArrayList<>();
                                                playerViolations.add(playerViolation);
                                                orderedMap.put(time, playerViolations);
                                            } else {
                                                playerViolations.add(playerViolation);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!orderedMap.isEmpty()) {
                            double duration = 1000.0; // 1 second
                            Map<Integer, FalsePositiveDetection.TimePeriod> averages = new HashMap<>();

                            for (Collection<PlayerViolation> playerViolations : orderedMap.values()) {
                                for (PlayerViolation playerViolation : playerViolations) {
                                    int timeMoment = AlgebraUtils.integerFloor(playerViolation.getTime() / duration);
                                    FalsePositiveDetection.TimePeriod timePeriod = averages.get(timeMoment);

                                    if (timePeriod == null) {
                                        timePeriod = new FalsePositiveDetection.TimePeriod(duration);
                                        timePeriod.add(playerViolation);
                                        averages.put(timeMoment, timePeriod);
                                    } else {
                                        timePeriod.add(playerViolation);
                                    }
                                }
                            }

                            // Separator

                            HashMap<Integer, Double> violationAverages = new HashMap<>();
                            HashMap<Integer, Integer> violationAveragesDivisor = new HashMap<>();

                            for (FalsePositiveDetection.TimePeriod timePeriod : averages.values()) {
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

                            // Separator
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

            // Separator (Calculate mining statistics)

            if (!hackerFreePlayerProfiles.isEmpty()) {
                for (Enums.MiningOre ore : Enums.MiningOre.values()) {
                    double average = 0.0, total = 0.0;

                    for (PlayerProfile playerProfile : hackerFreePlayerProfiles) {
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
            legitimatePlayers.clear();
            hackerPlayers.clear();
            playerFights.clear();
            statisticalProgress = new StatisticalProgress();
            hackerFreePlayerProfiles.clear();
            averageMining.clear();
            ViolationStatistics.clear();
            CancelViolation.clear();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearMaxCancelledViolations();
            }
        }
    }

    private static void updateCombatCache() {
        globalVelocityOccurrences.clear();
        double
                averageCPS = 0.0, averageCPSCount = 0.0, minCPS = 0.0, maxCPS = 0.0,

                averageReach = 0.0, averageReachCount = 0.0, minReach = 0.0, maxReach = 0.0,

                averageHitTime = 0.0, averageHitTimeCount = 0.0, minHitTime = 0.0, maxHitTime = 0.0,

                averageYawRate = 0.0, averageYawRateCount = 0.0, minYawRate = 0.0, maxYawRate = 0.0,

                averagePitchRate = 0.0, averagePitchRateCount = 0.0, minPitchRate = 0.0, maxPitchRate = 0.0,

                averageHitCombo = 0.0, averageHitComboCount = 0.0, minHitCombo = 0.0, maxHitCombo = 0.0,

                averageDuration = 0.0, averageDurationCount = 0.0, minDuration = 0.0, maxDuration = 0.0,

                averageHitRatio = 0.0, averageHitRatioCount = 0.0, minHitRatio = 0.0, maxHitRatio = 0.0,

                averageWinsToLosses = 0.0, averageWinsToLossesCount = 0.0, minWinsToLosses = 0.0, maxWinsToLosses = 0.0;

        if (hackerFreePlayerProfiles.size() >= profileRequirement) {
            List<PlayerFight> playerFights = new LinkedList<>();

            for (PlayerProfile playerProfile : hackerFreePlayerProfiles) {
                PlayerCombat combat = playerProfile.getCombat();

                if (combat.hasFights()) {
                    playerFights.addAll(combat.getPastFights());
                    double[] list = combat.getCPSAverages();

                    if (list != null) {
                        averageCPSCount += 1.0;
                        averageCPS += list[0];
                        maxCPS += list[1];
                        minCPS += list[2];
                    }
                    list = combat.getReachAverages();

                    if (list != null) {
                        averageReachCount += 1.0;
                        averageReach += list[0];
                        maxReach += list[1];
                        minReach += list[2];
                    }
                    list = combat.getHitTimeAverages();

                    if (list != null) {
                        averageHitTimeCount += 1.0;
                        averageHitTime += list[0];
                        maxHitTime += list[1];
                        minHitTime += list[2];
                    }
                    list = combat.getYawRateAverages();

                    if (list != null) {
                        averageYawRateCount += 1.0;
                        averageYawRate += list[0];
                        maxYawRate += list[1];
                        minYawRate += list[2];
                    }
                    list = combat.getPitchRateAverages();

                    if (list != null) {
                        averagePitchRateCount += 1.0;
                        averagePitchRate += list[0];
                        maxPitchRate += list[1];
                        minPitchRate += list[2];
                    }
                    list = combat.getHitComboAverages();

                    if (list != null) {
                        averageHitComboCount += 1.0;
                        averageHitCombo += list[0];
                        maxHitCombo += list[1];
                        minHitCombo += list[2];
                    }
                    list = combat.getDurationAverages();

                    if (list != null) {
                        averageDurationCount += 1.0;
                        averageDuration += list[0];
                        maxDuration += list[1];
                        minDuration += list[2];
                    }
                    list = combat.getHitRatioAverages();

                    if (list != null) {
                        averageHitRatioCount += 1.0;
                        averageHitRatio += list[0];
                        maxHitRatio += list[1];
                        minHitRatio += list[2];
                    }

                    // Separator

                    double individual = combat.getAverageWinLossRatio();

                    if (individual != -1.0) {
                        averageWinsToLossesCount += 1.0;
                        averageWinsToLosses += individual;
                        maxWinsToLosses = Math.max(maxWinsToLosses, individual);
                        minWinsToLosses = Math.min(minWinsToLosses, individual);
                    }
                }
            }
            ResearchEngine.playerFights.clear();
            ResearchEngine.playerFights.addAll(playerFights);

            // Separator

            if (averageCPSCount > 0.0) {
                cps[0] = minCPS / averageCPSCount;
                cps[1] = averageCPS / averageCPSCount;
                cps[2] = maxCPS / averageCPSCount;
            } else {
                cps[0] = -1.0;
                cps[1] = -1.0;
                cps[2] = -1.0;
            }

            if (averageReachCount > 0.0) {
                reach[0] = minReach / averageReachCount;
                reach[1] = averageReach / averageReachCount;
                reach[2] = maxReach / averageReachCount;
            } else {
                reach[0] = -1.0;
                reach[1] = -1.0;
                reach[2] = -1.0;
            }

            if (averageHitTimeCount > 0.0) {
                hitTime[0] = minHitTime / averageHitTimeCount;
                hitTime[1] = averageHitTime / averageHitTimeCount;
                hitTime[2] = maxHitTime / averageHitTimeCount;
            } else {
                hitTime[0] = -1.0;
                hitTime[1] = -1.0;
                hitTime[2] = -1.0;
            }

            if (averageYawRateCount > 0.0) {
                yawRate[0] = minYawRate / averageYawRateCount;
                yawRate[1] = averageYawRate / averageYawRateCount;
                yawRate[2] = maxYawRate / averageYawRateCount;
            } else {
                yawRate[0] = -1.0;
                yawRate[1] = -1.0;
                yawRate[2] = -1.0;
            }

            if (averagePitchRateCount > 0.0) {
                pitchRate[0] = minPitchRate / averagePitchRateCount;
                pitchRate[1] = averagePitchRate / averagePitchRateCount;
                pitchRate[2] = maxPitchRate / averagePitchRateCount;
            } else {
                pitchRate[0] = -1.0;
                pitchRate[1] = -1.0;
                pitchRate[2] = -1.0;
            }

            if (averageHitComboCount > 0.0) {
                hitCombo[0] = minHitCombo / averageHitComboCount;
                hitCombo[1] = averageHitCombo / averageHitComboCount;
                hitCombo[2] = maxHitCombo / averageHitComboCount;
            } else {
                hitCombo[0] = -1.0;
                hitCombo[1] = -1.0;
                hitCombo[2] = -1.0;
            }

            if (averageDurationCount > 0.0) {
                duration[0] = minDuration / averageDurationCount;
                duration[1] = averageDuration / averageDurationCount;
                duration[2] = maxDuration / averageDurationCount;
            } else {
                duration[0] = -1.0;
                duration[1] = -1.0;
                duration[2] = -1.0;
            }

            if (averageHitRatioCount > 0.0) {
                hitRatio[0] = minHitRatio / averageHitRatioCount;
                hitRatio[1] = averageHitRatio / averageHitRatioCount;
                hitRatio[2] = maxHitRatio / averageHitRatioCount;
            } else {
                hitRatio[0] = -1.0;
                hitRatio[1] = -1.0;
                hitRatio[2] = -1.0;
            }

            // Separator

            if (averageWinsToLossesCount > 0.0) {
                winsToLosses[0] = minWinsToLosses;
                winsToLosses[1] = averageWinsToLosses / averageWinsToLossesCount;
                winsToLosses[2] = maxWinsToLosses;
            } else {
                winsToLosses[0] = -1.0;
                winsToLosses[1] = -1.0;
                winsToLosses[2] = -1.0;
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
