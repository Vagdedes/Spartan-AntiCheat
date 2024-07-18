package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.LiveViolation;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.pattern.Pattern;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.profiling.ViolationHistory;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResearchEngine {

    private static boolean firstLoad = false;
    public static Map<Enums.HackType, Boolean> violationFired = new ConcurrentHashMap<>();
    private static long schedulerTicks = 0L;
    public static final Enums.DataType[] usableDataTypes = new Enums.DataType[]{
            Enums.DataType.JAVA,
            Enums.DataType.BEDROCK
    };
    private static final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            if (firstLoad) {
                if (schedulerTicks == 0) {
                    schedulerTicks = 1200L;

                    if (Config.sql.isEnabled()) {
                        refresh(Register.isPluginEnabled());
                    } else {
                        SpartanBukkit.analysisThread.executeIfFree(() -> {
                            updateCache(false);
                            MainMenu.refresh();
                        });
                    }
                } else {
                    schedulerTicks -= 1;

                    SpartanBukkit.analysisThread.executeIfFree(() -> {
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

    public static List<PlayerProfile> getPlayerProfiles() {
        return new ArrayList<>(playerProfiles.values());
    }

    public static PlayerProfile getPlayerProfile(String name) {
        name = name.toLowerCase();
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile != null) {
            return playerProfile;
        }
        playerProfile = new PlayerProfile(name);
        playerProfiles.put(name, playerProfile);
        return playerProfile;
    }

    public static PlayerProfile getPlayerProfile(SpartanPlayer player, boolean force) {
        PlayerProfile playerProfile;

        if (!force) {
            playerProfile = playerProfiles.get(player.name.toLowerCase());

            if (playerProfile != null) {
                playerProfile.update(player);
                return playerProfile;
            }
        }
        playerProfile = new PlayerProfile(player);
        playerProfiles.put(player.name.toLowerCase(), playerProfile);
        return playerProfile;
    }

    // Separator

    public static void resetData(Enums.HackType hackType) {
        if (firstLoad) {
            SpartanBukkit.analysisThread.execute(() -> {
                String hackTypeString = hackType.toString();

                // Separator

                if (!playerProfiles.isEmpty()) {
                    for (PlayerProfile playerProfile : playerProfiles.values()) {
                        playerProfile.evidence.remove(hackType);
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
            SpartanProtocol p = SpartanBukkit.getProtocol(playerName);
            boolean foundPlayer = p != null;
            PlayerProfile profile;

            if (foundPlayer) {
                profile = p.getProfile();

                for (Enums.HackType hackType : Enums.HackType.values()) {
                    p.spartanPlayer.getViolations(hackType).reset();
                }
            } else {
                profile = getPlayerProfile(playerName);
            }
            Pattern.deleteFromFile(profile);

            if (isStorageMode()) {
                // Clear Files/Database
                SpartanBukkit.analysisThread.execute(() -> {
                    playerProfiles.remove(playerName);

                    if (foundPlayer) {
                        p.setProfile(getPlayerProfile(p.spartanPlayer, true));
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

                if (foundPlayer) {
                    p.setProfile(getPlayerProfile(p.spartanPlayer, true));
                }
            }
            MainMenu.refresh();
            InteractiveInventory.playerInfo.refresh(playerName);
        }
    }

    // Separator

    private static String getDetectionInformation(String s) {
        int index = s.indexOf("[");
        return index > -1
                ? s.substring(index + 1, s.length() - 1)
                : null;
    }

    private static int[] getDetectionViolationLevel(String s) {
        String search = "(" + LiveViolation.violationLevelIdentifier + " ";
        int index1 = s.indexOf(search);

        if (index1 > -1) {
            s = s.substring(index1 + search.length());
            int index2 = s.indexOf(")");

            if (index2 != -1) {
                String number = s.substring(0, index2);
                String[] split = number.split("\\+", 3);

                if (split.length == 2) {
                    if (AlgebraUtils.validInteger(split[0])
                            && AlgebraUtils.validInteger(split[1])) {
                        return new int[]{
                                Integer.parseInt(split[0]),
                                Integer.parseInt(split[1])
                        };
                    }
                } else if (AlgebraUtils.validInteger(number)) {
                    return new int[]{
                            Integer.parseInt(number),
                            1
                    };
                }
            }
        }
        return null;
    }

    private static Enums.DataType getDataType(String s) {
        String find = "(" + LiveViolation.javaPlayerIdentifier + " ";
        int index = s.indexOf(find);

        if (index > -1) {
            s = s.substring(index + find.length());
            index = s.indexOf(")");
            s = s.substring(0, index);

            for (Enums.DataType dataType : usableDataTypes) {
                if (s.equals(dataType.toString())) {
                    return dataType;
                }
            }
        }
        return Enums.DataType.JAVA;
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
                        String data = rs.getString("information");
                        String[] all = TimeUtils.getAll(rs.getTimestamp("creation_date"));
                        String date = all[0] + " " + all[1];
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
                CloudBase.refresh(true);
                buildCache();
                Pattern.reload();
                CloudBase.refresh(false);
            } else {
                CloudBase.clear(true);
                Pattern.clear();
            }
        };

        if (firstLoad) {
            SpartanBukkit.analysisThread.executeIfFree(runnable);
        } else {
            SpartanBukkit.analysisThread.execute(runnable);
            firstLoad = true;
        }
    }

    private static void buildCache() {
        if (isStorageMode()) {
            Map<String, String> logs = getLogs();

            if (!logs.isEmpty()) {
                try {
                    for (Map.Entry<String, String> entry : logs.entrySet()) {
                        String fullDate = entry.getKey(),
                                partialDate = fullDate.substring(0, 10),
                                data = entry.getValue();

                        if (data.contains(" failed ")) {
                            String[] split = data.split(" ");

                            if (split.length >= 3) {
                                String hackTypeString = split[2];

                                for (Enums.HackType hackType : Enums.HackType.values()) {
                                    if (hackTypeString.equals(hackType.toString())) {
                                        String detection = getDetectionInformation(data);

                                        if (detection != null) {
                                            int[] violation = getDetectionViolationLevel(data);

                                            if (violation != null) {
                                                SimpleDateFormat sdf = new SimpleDateFormat(AntiCheatLogs.dateFormat);
                                                getPlayerProfile(split[0]).getViolationHistory(getDataType(data), hackType).store(
                                                        new PlayerViolation(
                                                                sdf.parse(fullDate).getTime(),
                                                                hackType,
                                                                violation[0],
                                                                violation[1]
                                                        )
                                                );
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        } else if (data.contains(" found ")) {
                            String[] split = data.split(" ");

                            if (split.length >= 9) {
                                String name = split[0].toLowerCase();

                                try {
                                    MiningHistory.MiningOre ore = MiningHistory.getMiningOre(
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
                    updateCache(true);
                    MainMenu.refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void updateCache(boolean force) {
        if ((force || !violationFired.isEmpty()) && !playerProfiles.isEmpty()) {
            Collection<PlayerProfile> profiles = playerProfiles.values();

            if (force) {
                violationFired.clear();

                for (PlayerProfile profile : profiles) {
                    profile.evidence.clear();
                }
            }

            for (Enums.DataType dataType : usableDataTypes) {
                for (Enums.HackType hackType : (force
                        ? Arrays.asList(Enums.HackType.values())
                        : violationFired.keySet())) {
                    Map<PlayerProfile, int[]> wave = new LinkedHashMap<>();

                    for (PlayerProfile profile : profiles) {
                        ViolationHistory violationHistory = profile.getViolationHistory(
                                dataType,
                                hackType
                        );

                        if (!violationHistory.isEmpty()) {
                            wave.put(
                                    profile,
                                    new int[]{
                                            violationHistory.getIncreaseSum(),
                                            violationHistory.getTimeDifferenceSum()
                                    }
                            );
                        }
                    }

                    if (!wave.isEmpty()) {
                        int sum = 0, squareSum = 0;

                        for (int[] value : wave.values()) {
                            sum += value[0];
                            squareSum += value[0] * value[0];
                        }
                        double divisor = wave.size(),
                                mean = sum / divisor,
                                deviation = Math.sqrt(squareSum / divisor);

                        for (Map.Entry<PlayerProfile, int[]> entryChild : wave.entrySet()) {
                            PlayerProfile profile = entryChild.getKey();
                            double probability = StatisticsMath.getCumulativeProbability(
                                    (entryChild.getValue()[0] - mean) / deviation
                            );

                            if (probability > profile.evidence.get(hackType)) {
                                profile.evidence.add(
                                        hackType,
                                        probability
                                );
                            }
                        }

                        // Separator

                        divisor--; // Because of the previous violation required for comparison

                        if (divisor > 0) {
                            sum = 0;
                            squareSum = 0;

                            for (int[] value : wave.values()) {
                                sum += value[1];
                                squareSum += value[1] * value[1];
                            }
                            mean = sum / divisor;
                            deviation = Math.sqrt(squareSum / divisor);

                            for (Map.Entry<PlayerProfile, int[]> entryChild : wave.entrySet()) {
                                PlayerProfile profile = entryChild.getKey();
                                double probability = StatisticsMath.getCumulativeProbability(
                                        (entryChild.getValue()[1] - mean) / deviation
                                );

                                if (probability < 0.0) {
                                    probability = 0.0 - probability;

                                    if (probability > profile.evidence.get(hackType)) {
                                        profile.evidence.add(
                                                hackType,
                                                probability
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
                violationFired.clear();
            }
        }
    }

    public static void queueToCache(PlayerViolation violation) {
        violationFired.put(violation.hackType, true);
    }

}
