package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.pattern.Pattern;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerEvidence;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
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

    private static final Map<Integer, Integer> requiredPlayers = new ConcurrentHashMap<>();
    private static boolean firstLoad = false;
    public static Map<Enums.HackType, Boolean> violationFired = new ConcurrentHashMap<>();
    private static long schedulerTicks = 0L;
    public static final Check.DataType[] usableDataTypes = new Check.DataType[]{
            Check.DataType.JAVA,
            Check.DataType.BEDROCK
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

    public static PlayerProfile getPlayerProfile(String name, boolean create) {
        name = name.toLowerCase();
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile != null) {
            return playerProfile;
        }
        if (create) {
            playerProfile = new PlayerProfile(name);
            playerProfiles.put(name, playerProfile);
            return playerProfile;
        } else {
            return null;
        }
    }

    public static PlayerProfile getPlayerProfile(SpartanProtocol protocol, boolean force) {
        PlayerProfile playerProfile;

        if (!force) {
            playerProfile = playerProfiles.get(protocol.player.getName().toLowerCase());

            if (playerProfile != null) {
                playerProfile.update(protocol);
                return playerProfile;
            }
        }
        playerProfile = new PlayerProfile(protocol);
        playerProfiles.put(protocol.player.getName().toLowerCase(), playerProfile);
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
                    for (double probability : PlayerEvidence.probabilities) {
                        requiredPlayers.remove(
                                hackType.hashCode() * SpartanBukkit.hashCodeMultiplier
                                        + PlayerEvidence.probabilityToFactors(probability)
                        );
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
                    p.spartanPlayer.getExecutor(hackType).resetLevel();
                }
            } else {
                profile = getPlayerProfile(playerName, true);
            }
            Pattern.deleteFromFile(profile);

            if (isStorageMode()) {
                // Clear Files/Database
                SpartanBukkit.analysisThread.execute(() -> {
                    playerProfiles.remove(playerName);

                    if (foundPlayer) {
                        p.setProfile(getPlayerProfile(p, true));
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
                    p.setProfile(getPlayerProfile(p, true));
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
        String search = "(" + CheckExecutor.violationLevelIdentifier + " ";
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

    private static Check.DataType getDataType(String s) {
        String find = "(" + CheckExecutor.javaPlayerIdentifier + " ";
        int index = s.indexOf(find);

        if (index > -1) {
            s = s.substring(index + find.length());
            index = s.indexOf(")");
            s = s.substring(0, index);

            for (Check.DataType dataType : usableDataTypes) {
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
                buildCache();
                Pattern.reload();
                CloudBase.refresh();
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
                for (Map.Entry<String, String> entry : logs.entrySet()) {
                    try {
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
                                                getPlayerProfile(split[0], true).getViolationHistory(getDataType(data), hackType).store(
                                                        hackType,
                                                        new PlayerViolation(
                                                                sdf.parse(fullDate).getTime(),
                                                                violation[0],
                                                                violation[1],
                                                                detection
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
                                        MiningHistory miningHistory = getPlayerProfile(name, true).getMiningHistory(ore);

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
                    } catch (Exception ignored) {

                    }
                }
                updateCache(true);
                MainMenu.refresh();
            }
        }
    }

    private static void updateCache(boolean force) {
        if ((force || !violationFired.isEmpty()) && !playerProfiles.isEmpty()) {
            Collection<PlayerProfile> profiles = playerProfiles.values();

            if (force) {
                for (PlayerProfile profile : profiles) {
                    profile.evidence.clear();
                }
            }

            for (Check.DataType dataType : usableDataTypes) {
                for (Enums.HackType hackType : (force
                        ? Arrays.asList(Enums.HackType.values())
                        : violationFired.keySet())) {
                    if (!hackType.getCheck().isEnabled(dataType, null)) {
                        continue;
                    }
                    Map<PlayerProfile, Double> wave = new LinkedHashMap<>();

                    for (PlayerProfile profile : profiles) {
                        Double timeDifference = profile.getViolationHistory(
                                dataType,
                                hackType
                        ).getTimeDifference(hackType);

                        if (timeDifference != null) {
                            wave.put(
                                    profile,
                                    timeDifference
                            );
                        }
                    }

                    if (!wave.isEmpty()) {
                        double sum = 0,
                                squareSum = 0,
                                min = Double.MAX_VALUE,
                                max = Double.MIN_VALUE;

                        for (double value : wave.values()) {
                            sum += value;
                            squareSum += value * value;
                        }
                        double divisor = wave.size(),
                                mean = sum / divisor,
                                deviation = Math.sqrt(squareSum / divisor);

                        for (Map.Entry<PlayerProfile, Double> entryChild : wave.entrySet()) {
                            double probability = StatisticsMath.getCumulativeProbability(
                                    (entryChild.getValue() - mean) / deviation
                            );
                            entryChild.setValue(probability);

                            if (probability > max) {
                                max = probability;
                            }
                            if (probability < min) {
                                min = probability;
                            }
                        }
                        max = 1.0 - max;
                        double distributionDifference = Math.abs(max - min);

                        for (Map.Entry<PlayerProfile, Double> entryChild : wave.entrySet()) {
                            entryChild.getKey().evidence.add(
                                    hackType,
                                    PlayerEvidence.modifyProbability(
                                            entryChild.getValue(),
                                            Math.max(min - distributionDifference, 0.0),
                                            Math.max(max - distributionDifference, 0.0)
                                    )
                            );
                        }
                    }
                }
            }
            violationFired.clear();
            Iterator<Integer> iterator = requiredPlayers.values().iterator();

            while (iterator.hasNext()) {
                if (iterator.next() != 0) {
                    iterator.remove();
                }
            }
        }
        int squareSum = 0, count = 0;

        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (hackType.getCheck().isEnabled(null, null)) {
                int result = getRequiredPlayers(hackType, PlayerEvidence.punishmentProbability);
                squareSum += result * result;
                count += result;
            }
        }
        if (squareSum > 0) {
            squareSum = AlgebraUtils.integerCeil(Math.sqrt(squareSum / (double) count));
            String message = AwarenessNotifications.getOptionalNotification(
                    "The plugin needs data from " + squareSum + " more player/s to enable preventions & punishments for all checks."
                            + " This is normal and will be resolved as more players join your server."
            );

            if (message != null) {
                List<SpartanPlayer> players = Permissions.getStaff();

                if (!players.isEmpty()) {
                    for (SpartanPlayer p : players) {
                        if (AwarenessNotifications.canSend(p.protocol.getUUID(), "limited-data", 60 * 60)) {
                            p.getInstance().sendMessage(message);
                        }
                    }
                }
            }
        } else if (SpartanBukkit.packetsEnabled()) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                Check check = hackType.getCheck();

                if (check.isEnabled(null, null)
                        && !check.isSilent(null, null)) {
                    String message = AwarenessNotifications.getOptionalNotification(
                            "Spartan does not support Java player preventions when running on the packet level."
                                    + " This is due to problems risen from trying to implement this functionality with packets."
                    );

                    if (message != null) {
                        List<SpartanPlayer> players = Permissions.getStaff();

                        if (!players.isEmpty()) {
                            for (SpartanPlayer p : players) {
                                if (AwarenessNotifications.canSend(p.protocol.getUUID(), "packet-preventions", 60 * 60)) {
                                    p.getInstance().sendMessage(message);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public static void queueToCache(Enums.HackType hackType) {
        violationFired.put(hackType, true);
    }

    public static int getRequiredPlayers(Enums.HackType hackType, double probability) {
        int requirement = PlayerEvidence.probabilityToFactors(probability),
                hash = hackType.hashCode() * SpartanBukkit.hashCodeMultiplier + requirement,
                count = requiredPlayers.getOrDefault(hash, -1);

        if (count == -1) {
            count = 0;

            if (playerProfiles.size() >= requirement) {
                for (PlayerProfile profile : playerProfiles.values()) {
                    if (profile.hasData(hackType)) {
                        count++;

                        if (count == requirement) {
                            return 0;
                        }
                    }
                }
                count = requirement - count;
            } else {
                count = requirement - playerProfiles.size();
            }
            requiredPlayers.put(hash, count);
        }
        return count;
    }

}
