package com.vagdedes.spartan.utils.minecraft.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PunishmentHistory;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigUtils {

    private static final Map<String, Map<String, Boolean>> map
            = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void clear() {
        synchronized (map) {
            map.clear();
        }
    }

    public static boolean contains(String message, String index) {
        synchronized (map) {
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
                childMap = new LinkedHashMap<>();
                boolean contains = message.contains(index);
                childMap.put(index, contains);
                map.put(message, childMap);
                return contains;
            }
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
            message = replace(message, "{punish:detection}", String.valueOf(check.canPunish));
        }
        return message;
    }

    public static String replaceWithSyntax(SpartanPlayer p, String message, HackType hackType) {
        UUID uuid = p.uuid;
        SpartanLocation loc = p.movement.getLocation();
        String worldName = p.getWorld().getName();
        message = replace(message, "{tps}", String.valueOf(AlgebraUtils.cut(TPS.get(p, false), 2)));
        message = replace(message, "{player}", p.name);
        message = replace(message, "{player:type}", p.dataType.lowerCase);
        message = replace(message, "{uuid}", uuid.toString());
        message = replace(message, "{ping}", String.valueOf(p.getPing()));
        message = replace(message, "{world}", worldName);
        message = replace(message, "{health}", String.valueOf(p.getHealth()));
        message = replace(message, "{gamemode}", p.getGameMode().toString().toLowerCase());
        message = replace(message, "{x}", String.valueOf(loc.getBlockX()));
        message = replace(message, "{y}", String.valueOf(loc.getBlockY()));
        message = replace(message, "{z}", String.valueOf(loc.getBlockZ()));
        message = replace(message, "{yaw}", String.valueOf(AlgebraUtils.integerRound(loc.getYaw())));
        message = replace(message, "{pitch}", String.valueOf(AlgebraUtils.integerRound(loc.getPitch())));
        message = replace(message, "{cps}", String.valueOf(p.clicks.getCount()));

        PunishmentHistory punishmentHistory = p.getProfile().punishmentHistory;
        message = replace(message, "{kicks}", String.valueOf(punishmentHistory.getKicks()));
        message = replace(message, "{warnings}", String.valueOf(punishmentHistory.getWarnings()));

        if (hackType != null) {
            message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(worldName)));
            message = replace(message, "{vls:detection}", String.valueOf(p.getViolations(hackType).getTotalLevel()));
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

        if (off.isOnline()) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) off);

            if (p != null) {
                SpartanLocation loc = p.movement.getLocation();
                String worldName = p.getWorld().getName();
                message = replace(message, "{player:type}", p.dataType.lowerCase);
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
                message = replace(message, "{cps}", String.valueOf(p.clicks.getCount()));

                PunishmentHistory punishmentHistory = p.getProfile().punishmentHistory;
                message = replace(message, "{kicks}", String.valueOf(punishmentHistory.getKicks()));
                message = replace(message, "{warnings}", String.valueOf(punishmentHistory.getWarnings()));

                if (hasHackType) {
                    message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(worldName)));
                    message = replace(message, "{vls:detection}", String.valueOf(p.getViolations(hackType).getTotalLevel()));
                }
            } else if (hasHackType) {
                PlayerProfile profile = ResearchEngine.getPlayerProfileAdvanced(name, false);

                if (profile != null) {
                    message = replace(message, "{player:type}", profile.getDataType().lowerCase);
                }
                message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(null)));
            }
        } else if (hasHackType) {
            message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(null)));
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

    public static boolean has(File file, String path) {
        return YamlConfiguration.loadConfiguration(file).contains(path);
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
}
