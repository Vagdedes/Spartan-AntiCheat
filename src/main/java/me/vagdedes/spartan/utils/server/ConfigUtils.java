package me.vagdedes.spartan.utils.server;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.profiling.PunishmentHistory;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.TimeUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigUtils {

    private static final Map<String, Map<String, Boolean>> map = new ConcurrentHashMap<>();

    public static void clear() {
        map.clear();
    }

    public static boolean contains(String message, String index) {
        Map<String, Boolean> childMap = map.get(message);

        if (childMap != null) {
            Boolean contains = childMap.get(index);

            if (contains != null) {
                return contains;
            }
            contains = message.contains(index);
            childMap.put(index, contains);
            return contains;
        } else {
            childMap = new ConcurrentHashMap<>();
            boolean contains = message.contains(index);
            childMap.put(index, contains);
            map.put(message, childMap);
            return contains;
        }
    }

    public static String replace(String message, String target, String replacement) {
        return contains(message, target) ? message.replace(target, replacement) : message;
    }

    public static String replaceWithSyntax(String message, HackType hackType) {
        message = replace(message, "%%", " ");
        message = replace(message, "{space}", " ");
        message = replace(message, "{tps}", String.valueOf(AlgebraUtils.cut(TPS.get(null, false), 2)));
        message = replace(message, "{online}", String.valueOf(SpartanBukkit.getPlayerCount()));
        message = replace(message, "{staff}", String.valueOf(Permissions.getStaff().size()));
        message = replace(message, "{motd}", Bukkit.getMotd());
        message = replace(message, "{server:name}", CrossServerInformation.getOptionValue());
        message = replace(message, "{plugin:version}", API.getVersion());
        message = replace(message, "{server:version}", MultiVersion.versionString());
        message = replace(message, "{server:type}", MultiVersion.fork());
        message = replace(message, "{line}", "\n");

        if (contains(message, "{date:")) {
            LocalDateTime now = LocalDateTime.now();
            message = replace(message, "{date:time}", DateTimeFormatter.ofPattern("HH:mm:ss").format(now));
            message = replace(message, "{date:d-m-y}", DateTimeFormatter.ofPattern("dd/MM/yyyy").format(now));
            message = replace(message, "{date:m-d-y}", DateTimeFormatter.ofPattern("MM/dd/yyyy").format(now));
            message = replace(message, "{date:y-m-d}", DateTimeFormatter.ofPattern("yyyy/MM/dd").format(now));
        }

        if (hackType != null) {
            Check check = hackType.getCheck();

            if (contains(message, "{detection")) {
                message = replace(message, "{detection}", check.getName());
                message = replace(message, "{detection:real}", hackType.toString());
            }
            message = replace(message, "{punish:detection}", String.valueOf(check.canPunish()));
        }
        return message;
    }

    public static String replaceWithSyntax(SpartanPlayer p, String message, HackType hackType) {
        UUID uuid = p.getUniqueId();
        SpartanLocation loc = p.getLocation();
        String worldName = p.getWorld().getName();
        message = replace(message, "{tps}", String.valueOf(AlgebraUtils.cut(TPS.get(p, false), 2)));
        message = replace(message, "{player}", p.getName());
        message = replace(message, "{player:type}", p.getDataType().lowerCase);
        message = replace(message, "{uuid}", uuid.toString());
        message = replace(message, "{ping}", String.valueOf(p.getPing()));
        message = replace(message, "{vls}", String.valueOf(Check.getViolationCount(uuid)));
        message = replace(message, "{world}", worldName);
        message = replace(message, "{health}", String.valueOf(p.getHealth()));
        message = replace(message, "{gamemode}", p.getGameMode().toString().toLowerCase());
        message = replace(message, "{x}", String.valueOf(loc.getBlockX()));
        message = replace(message, "{y}", String.valueOf(loc.getBlockY()));
        message = replace(message, "{z}", String.valueOf(loc.getBlockZ()));
        message = replace(message, "{yaw}", String.valueOf(AlgebraUtils.integerRound(loc.getYaw())));
        message = replace(message, "{pitch}", String.valueOf(AlgebraUtils.integerRound(loc.getPitch())));
        message = replace(message, "{cps}", String.valueOf(p.getClickData().getCount()));
        message = replace(message, "{time-online}", String.valueOf(TimeUtils.getDifference(new Timestamp(p.getLastPlayed()), 1000)));
        message = replace(message, "{moving}", String.valueOf(p.isMoving(true)));

        PunishmentHistory punishmentHistory = p.getProfile().getPunishmentHistory();
        message = replace(message, "{kicks}", String.valueOf(punishmentHistory.getKicks()));
        message = replace(message, "{bans}", String.valueOf(punishmentHistory.getBans()));
        message = replace(message, "{warnings}", String.valueOf(punishmentHistory.getWarnings()));
        message = replace(message, "{reports}", String.valueOf(punishmentHistory.getReports().size()));

        if (hackType != null) {
            LiveViolation liveViolation = hackType.getCheck().getViolations(p);
            int lastLevel = liveViolation.getLastCancelledLevel();
            boolean cancelled = lastLevel != 0;
            int violations = cancelled ? liveViolation.getCancelledLevel(lastLevel) : liveViolation.getLevel();
            String category = cancelled ? Enums.PunishmentCategory.MINIMUM.getString() : Check.getCategoryFromViolations(violations, hackType, p.getDataType(), p.getProfile().isSuspectedOrHacker(hackType)).getString();
            message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(worldName, uuid)));
            message = replace(message, "{vls:detection}", String.valueOf(violations));
            message = replace(message, "{category:detection}", category);
            message = replace(message, "{detection:category:adverb}", (category.endsWith("ly") ? category : category + "ly"));
        }
        return ChatColor.translateAlternateColorCodes('&', replaceWithSyntax(message, hackType));
    }

    public static String replaceWithSyntax(OfflinePlayer off, String message, HackType hackType) {
        boolean hasHackType = hackType != null;
        UUID uuid = off.getUniqueId();
        String name = off.getName();

        if (name != null) {
            message = replace(message, "{player}", name);
        }
        message = replace(message, "{uuid}", uuid.toString());
        message = replace(message, "{vls}", String.valueOf(Check.getViolationCount(uuid)));

        if (off.isOnline()) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) off);

            if (p != null) {
                SpartanLocation loc = p.getLocation();
                String worldName = p.getWorld().getName();
                message = replace(message, "{player:type}", p.getDataType().lowerCase);
                message = replace(message, "{tps}", String.valueOf(AlgebraUtils.cut(TPS.get(p, false), 2)));
                message = replace(message, "{ping}", String.valueOf(p.getPing()));
                message = replace(message, "{world}", worldName);
                message = replace(message, "{health}", String.valueOf(p.getHealth()));
                message = replace(message, "{gamemode}", p.getGameMode().toString().toLowerCase());
                message = replace(message, "{x}", String.valueOf(loc.getBlockX()));
                message = replace(message, "{y}", String.valueOf(loc.getBlockY()));
                message = replace(message, "{z}", String.valueOf(loc.getBlockZ()));
                message = replace(message, "{yaw}", String.valueOf(AlgebraUtils.integerRound(loc.getYaw())));
                message = replace(message, "{pitch}", String.valueOf(AlgebraUtils.integerRound(loc.getPitch())));
                message = replace(message, "{cps}", String.valueOf(p.getClickData().getCount()));
                message = replace(message, "{time-online}", String.valueOf(TimeUtils.getDifference(new Timestamp(p.getLastPlayed()), 1000)));
                message = replace(message, "{moving}", String.valueOf(p.getCustomDistance() > 0.0));

                PunishmentHistory punishmentHistory = ResearchEngine.getPlayerProfile(name).getPunishmentHistory();
                message = replace(message, "{kicks}", String.valueOf(punishmentHistory.getKicks()));
                message = replace(message, "{bans}", String.valueOf(punishmentHistory.getBans()));
                message = replace(message, "{warnings}", String.valueOf(punishmentHistory.getWarnings()));
                message = replace(message, "{reports}", String.valueOf(punishmentHistory.getReports().size()));

                if (hasHackType) {
                    LiveViolation liveViolation = hackType.getCheck().getViolations(uuid, p.getDataType());
                    int lastLevel = liveViolation.getLastCancelledLevel();
                    boolean cancelled = lastLevel != 0;
                    int violations = cancelled ? liveViolation.getCancelledLevel(lastLevel) : liveViolation.getLevel();
                    String category = cancelled ? Enums.PunishmentCategory.MINIMUM.getString() : Check.getCategoryFromViolations(violations, hackType, p.getDataType(), p.getProfile().isSuspectedOrHacker(hackType)).getString();
                    message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(worldName, uuid)));
                    message = replace(message, "{vls:detection}", String.valueOf(violations));
                    message = replace(message, "{category:detection}", category);
                    message = replace(message, "{detection:category:adverb}", (category.endsWith("ly") ? category : category + "ly"));
                }
            } else if (hasHackType) {
                PlayerProfile profile = ResearchEngine.getPlayerProfileAdvanced(name, false);

                if (profile != null) {
                    message = replace(message, "{player:type}", profile.getDataType().lowerCase);
                }
                message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(null, null)));
            }
        } else if (hasHackType) {
            message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(null, null)));
        }
        return ChatColor.translateAlternateColorCodes('&', replaceWithSyntax(message, hackType));
    }

    public static void add(File file, String path, Object value) {
        YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception ignored) {
            }
        }
        if (!filea.contains(path)) {
            set(file, path, value);
        }
    }

    public static void set(File file, String path, Object value) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception ignored) {
            }
        }
        final YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);
        filea.set(path, value);

        try {
            filea.save(file);
        } catch (Exception ignored) {
        }
    }

    public static void add(String path, Object value) {
        if (!Register.plugin.getConfig().contains(path)) {
            set(path, value);
        }
    }

    public static void set(String path, Object value) {
        Register.plugin.getConfig().set(path, value);
        Register.plugin.saveConfig();
        Register.plugin.reloadConfig();
    }

    public static int getDirectorySize(String directory) {
        File[] files = new File(directory).listFiles();
        int i = 0;

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    i++;
                }
            }
        }
        return i;
    }
}
