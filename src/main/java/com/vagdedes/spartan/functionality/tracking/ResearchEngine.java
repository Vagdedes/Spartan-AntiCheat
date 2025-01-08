package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.concurrent.Threads;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResearchEngine {

    private static boolean firstLoad = false;
    public static Map<Enums.HackType, Collection<Check.DataType>> violationFired = new ConcurrentHashMap<>();
    private static long schedulerTicks = 0L;
    private static final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();
    private static final Threads.ThreadPool statisticsThread = new Threads.ThreadPool(1L);

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            if (firstLoad) {
                if (schedulerTicks == 0) {
                    schedulerTicks = 1200L;

                    if (Config.sql.isEnabled()) {
                        refresh(Register.isPluginEnabled());
                    } else {
                        statisticsThread.executeIfFree(() -> {
                            updateCache(false);
                            MainMenu.refresh();
                        });
                    }
                } else {
                    schedulerTicks -= 1;

                    statisticsThread.executeIfFree(() -> {
                        updateCache(false);

                        if (schedulerTicks % TPS.maximum == 0) {
                            MainMenu.refresh();
                        }
                    });
                }
            }
        }, 1L, 1L);
    }

    // Separator

    public static Collection<PlayerProfile> getPlayerProfiles() {
        return playerProfiles.values();
    }

    public static PlayerProfile getPlayerProfile(String name) {
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile == null) {
            playerProfile = new PlayerProfile(name);
            playerProfiles.put(name, playerProfile);
        }
        return playerProfile;
    }

    public static PlayerProfile getAnyCasePlayerProfile(String name) {
        if (!playerProfiles.isEmpty()) {
            for (Map.Entry<String, PlayerProfile> entry : playerProfiles.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public static PlayerProfile getPlayerProfile(SpartanProtocol protocol) {
        PlayerProfile playerProfile = playerProfiles.get(protocol.bukkit().getName());

        if (playerProfile == null) {
            playerProfile = new PlayerProfile(protocol);
            playerProfiles.put(protocol.bukkit().getName(), playerProfile);
        }
        return playerProfile;
    }

    private static void createPlayerProfile(SpartanProtocol protocol) {
        PlayerProfile profile = new PlayerProfile(protocol);
        profile.update(protocol);
        playerProfiles.put(protocol.bukkit().getName(), new PlayerProfile(protocol));
    }

    // Separator

    public static void resetData(Enums.HackType hackType) {
        if (firstLoad) {
            statisticsThread.execute(() -> {
                String hackTypeString = hackType.toString();

                // Separator

                if (!playerProfiles.isEmpty()) {
                    for (PlayerProfile playerProfile : playerProfiles.values()) {
                        for (CheckDetection detectionExecutor : playerProfile.getRunner(hackType).getDetections()) {
                            for (Check.DataType dataType : Check.DataType.values()) {
                                detectionExecutor.clearProbability(dataType);
                                detectionExecutor.clearData(dataType);
                            }
                        }
                        playerProfile.getContinuity().clear();
                    }
                    updateCache(true);
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
    }

    public static void resetData(String playerName) {
        if (firstLoad) {
            // Clear Violations
            SpartanProtocol player = SpartanBukkit.getProtocol(playerName);

            if (isStorageMode()) {
                // Clear Files/Database
                statisticsThread.execute(() -> {
                    if (player == null) {
                        playerProfiles.remove(playerName);
                    } else {
                        createPlayerProfile(player);
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
            } else if (player == null) {
                playerProfiles.remove(playerName);
            } else {
                createPlayerProfile(player);
            }
            MainMenu.refresh();
            InteractiveInventory.playerInfo.refresh(playerName);
        }
    }

    // Separator

    public static String findInformation(String s, String pattern) {
        String find = "(" + pattern + " ";
        int index = s.indexOf(find);

        if (index > -1) {
            s = s.substring(index + find.length());
            index = s.indexOf(")");
            return s.substring(0, index);
        }
        return null;
    }

    private static Check.DataType findDataType(String s) {
        s = findInformation(s, CheckDetection.javaPlayerIdentifier);

        if (s != null) {
            for (Check.DataType dataType : Check.DataType.values()) {
                if (s.equals(dataType.toString())) {
                    return dataType;
                }
            }
        }
        return Check.DataType.JAVA;
    }

    private static boolean isStorageMode() {
        return Config.settings.getBoolean("Logs.log_file") || Config.sql.isEnabled();
    }

    private static Collection<File> getFiles() {
        File[] files = new File(AntiCheatLogs.folderPath).listFiles();

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

    private static Map<String, String> getLogs() {
        Map<String, String> cache = new LinkedHashMap<>();
        int byteSize = 0;
        boolean isFull = false,
                continueWithYAML = false;

        // Separator

        if (Config.sql.isEnabled()) {
            try {
                ResultSet rs = Config.sql.query("SELECT creation_date, information FROM " + Config.sql.getTable() + " ORDER BY id DESC LIMIT " + SpartanBukkit.maxSQLRows + ";");

                if (rs != null) {
                    while (rs.next()) {
                        String data = rs.getString("information"),
                                date = rs.getString("creation_date");
                        cache.put(date, data);
                        byteSize += date.length() + data.length();

                        if (byteSize >= SpartanBukkit.maxBytes) {
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
        } else {
            continueWithYAML = true;
        }

        // YAML Process

        if (!isFull && continueWithYAML) {
            Collection<File> files = getFiles();

            if (!files.isEmpty()) {
                for (File file : files) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

                    for (String key : c.getKeys(false)) {
                        String data = c.getString(key);

                        if (data != null) {
                            cache.put(key, data);
                            byteSize += key.length() + data.length();

                            if (byteSize >= SpartanBukkit.maxBytes) {
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
            Config.sql.refreshDatabase();

            if (enabledPlugin) {
                buildCache();
                CloudBase.refresh();
            } else {
                CloudBase.clear();
            }
        };

        if (firstLoad) {
            statisticsThread.executeIfFree(runnable);
        } else {
            statisticsThread.execute(runnable);
            firstLoad = true;
        }
    }

    private static void buildCache() {
        if (isStorageMode()) {
            Map<String, String> logs = getLogs();

            if (!logs.isEmpty()) {
                for (Map.Entry<String, String> entry : logs.entrySet()) {
                    String fullDate = entry.getKey(),
                            data = entry.getValue();
                    String detection = findInformation(
                            data,
                            CheckDetection.detectionIdentifier
                    );

                    if (detection != null) {
                        String hackTypeString = findInformation(
                                data,
                                CheckDetection.checkIdentifier
                        );

                        if (hackTypeString != null) {
                            String player = findInformation(
                                    data,
                                    AntiCheatLogs.playerIdentifier
                            );

                            if (player != null) {
                                for (Enums.HackType hackType : Enums.HackType.values()) {
                                    if (hackTypeString.equals(hackType.toString())) {
                                        CheckDetection detectionExecutor = getPlayerProfile(
                                                player
                                        ).getRunner(
                                                hackType
                                        ).getDetection(
                                                detection
                                        );

                                        if (detectionExecutor != null) {
                                            SimpleDateFormat sdf = new SimpleDateFormat(AntiCheatLogs.dateFormat);

                                            try {
                                                detectionExecutor.store(
                                                        findDataType(data),
                                                        sdf.parse(fullDate).getTime()
                                                );
                                            } catch (Exception ignored) {
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        String oreString = findInformation(
                                data,
                                MiningHistory.oreIdentifier
                        );

                        if (oreString != null) {
                            String amount = findInformation(
                                    data,
                                    MiningHistory.amountIdentifier
                            );

                            if (amount != null
                                    && AlgebraUtils.validInteger(amount)) {
                                String player = findInformation(
                                        data,
                                        AntiCheatLogs.playerIdentifier
                                );

                                if (player != null) {
                                    String environmentString = findInformation(
                                            data,
                                            MiningHistory.environmentIdentifier
                                    );

                                    if (environmentString != null) {
                                        Material material = MaterialUtils.findMaterial(
                                                oreString.toUpperCase().replace("-", "_")
                                        );

                                        if (material != null) {
                                            MiningHistory.MiningOre ore = MiningHistory.getMiningOre(material);

                                            if (ore != null) {
                                                World.Environment environment = MaterialUtils.findEnvironment(
                                                        environmentString.toUpperCase().replace("-", "_")
                                                );

                                                if (environment != null) {
                                                    getPlayerProfile(
                                                            player
                                                    ).getMiningHistory(
                                                            ore
                                                    ).increaseMines(
                                                            environment,
                                                            Integer.parseInt(amount)
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            int index = data.indexOf(PlayerProfile.activeFor);

                            if (index != -1) {
                                SimpleDateFormat sdf = new SimpleDateFormat(AntiCheatLogs.dateFormat);

                                try {
                                    ResearchEngine.getPlayerProfile(
                                            data.split(" ", 2)[0]
                                    ).getContinuity().setActiveTime(
                                            sdf.parse(fullDate).getTime(),
                                            Long.parseLong(data.substring(index + PlayerProfile.activeFor.length())),
                                            false
                                    );
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }
        for (PlayerProfile profile : playerProfiles.values()) {
            for (CheckRunner runner : profile.getRunners()) {
                for (CheckDetection detection : runner.getDetections()) {
                    detection.sort();
                }
            }
        }
        updateCache(true);
        MainMenu.refresh();
    }

    private static void updateCache(boolean force) {
        if ((force || !violationFired.isEmpty()) && !playerProfiles.isEmpty()) {
            Collection<PlayerProfile> profiles = playerProfiles.values();

            if (force) {
                for (PlayerProfile profile : profiles) {
                    for (CheckRunner executor : profile.getRunners()) {
                        for (CheckDetection detectionExecutor : executor.getDetections()) {
                            for (Check.DataType dataType : Check.DataType.values()) {
                                detectionExecutor.clearProbability(dataType);
                            }
                        }
                    }
                }
            }

            for (Enums.HackType hackType : (force
                    ? Arrays.asList(Enums.HackType.values())
                    : violationFired.keySet())) {
                Collection<Check.DataType> dataTypes = violationFired.get(hackType);

                for (Check.DataType dataType : (force
                        ? Arrays.asList(Check.DataType.values())
                        : (dataTypes == null || dataTypes.isEmpty()
                        ? Arrays.asList(Check.DataType.values()) :
                        dataTypes))) {
                    Map<String, Map<CheckDetection, Double>> wave = new LinkedHashMap<>();

                    if (hackType.getCheck().isEnabled(dataType, null)) {
                        for (PlayerProfile profile : profiles) {
                            for (CheckDetection detectionExecutor : profile.getRunner(hackType).getDetections()) {
                                wave.computeIfAbsent(
                                        detectionExecutor.name,
                                        k -> new LinkedHashMap<>()
                                ).put(
                                        detectionExecutor,
                                        detectionExecutor.getData(profile, dataType)
                                );
                            }
                        }
                    }

                    if (wave.isEmpty()) {
                        for (PlayerProfile profile : profiles) {
                            for (CheckDetection detectionExecutor : profile.getRunner(hackType).getDetections()) {
                                detectionExecutor.clearProbability(dataType);
                            }
                        }
                    } else {
                        for (Map<CheckDetection, Double> map : wave.values()) {
                            double sum = 0,
                                    squareSum = 0,
                                    min = Double.MAX_VALUE,
                                    max = Double.MIN_VALUE;

                            for (double value : map.values()) {
                                sum += value;
                                squareSum += value * value;
                            }
                            double divisor = map.size(),
                                    mean = sum / divisor,
                                    deviation = Math.sqrt(squareSum / divisor);

                            for (Map.Entry<CheckDetection, Double> entry : map.entrySet()) {
                                double probability = StatisticsMath.getCumulativeProbability(
                                        (entry.getValue() - mean) / deviation
                                );
                                entry.setValue(probability);

                                if (probability > max) {
                                    max = probability;
                                }
                                if (probability < min) {
                                    min = probability;
                                }
                            }
                            max = 1.0 - max;
                            double distributionDifference = Math.abs(max - min);

                            for (Map.Entry<CheckDetection, Double> entry : map.entrySet()) {
                                entry.getKey().setProbability(
                                        dataType,
                                        PlayerEvidence.modifyProbability(
                                                entry.getValue(),
                                                Math.max(min - distributionDifference, 0.0),
                                                Math.max(max - distributionDifference, 0.0)
                                        )
                                );
                            }
                        }
                    }
                }
            }
            violationFired.clear();
        }
    }

    public static void queueToCache(Enums.HackType hackType, Check.DataType dataType) {
        violationFired.computeIfAbsent(
                hackType,
                k -> new CopyOnWriteArrayList<>()
        ).add(dataType);
    }

}
