package com.vagdedes.spartan.utils.minecraft.server;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.notifications.CrossServerNotifications;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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
import java.util.UUID;

public class ConfigUtils {

    public static String replace(String message, String target, String replacement) {
        return message.replace(target, replacement);
    }

    public static String replaceWithSyntax(String message, HackType hackType) {
        message = replace(message, "{space}", " ");
        message = replace(message, "{online}", String.valueOf(SpartanBukkit.getPlayerCount()));
        message = replace(message, "{staff}", String.valueOf(Permissions.getStaff().size()));
        message = replace(message, "{motd}", Bukkit.getMotd());
        message = replace(message, "{server:name}", CrossServerNotifications.getServerName());
        message = replace(message, "{plugin:version}", API.getVersion());
        message = replace(message, "{server:version}", MultiVersion.serverVersion.toString());
        message = replace(message, "{line}", "\n");

        LocalDateTime now = LocalDateTime.now();
        message = replace(message, "{date:time}", DateTimeFormatter.ofPattern("HH:mm:ss").format(now));
        message = replace(message, "{date:d-m-y}", DateTimeFormatter.ofPattern("dd/MM/yyyy").format(now));
        message = replace(message, "{date:m-d-y}", DateTimeFormatter.ofPattern("MM/dd/yyyy").format(now));
        message = replace(message, "{date:y-m-d}", DateTimeFormatter.ofPattern("yyyy/MM/dd").format(now));

        if (hackType != null) {
            Check check = hackType.getCheck();
            message = replace(message, "{detection}", check.getName());
            message = replace(message, "{detection:real}", hackType.toString());
        }
        return message;
    }

    public static String replaceWithSyntax(SpartanProtocol p, String message, HackType hackType) {
        SpartanLocation loc = p.spartan.movement.getLocation();
        String worldName = p.spartan.getWorld().getName();
        message = replace(message, "{player}", p.bukkit.getName());
        message = replace(message, "{player:type}", p.spartan.dataType.toString().toLowerCase());
        message = replace(message, "{uuid}", p.getUUID().toString());
        message = replace(message, "{ping}", String.valueOf(p.getPing()));
        message = replace(message, "{world}", worldName);
        message = replace(message, "{health}", String.valueOf(p.spartan.getHealth()));
        message = replace(message, "{gamemode}", p.bukkit.getGameMode().toString().toLowerCase());
        message = replace(message, "{x}", String.valueOf(loc.getBlockX()));
        message = replace(message, "{y}", String.valueOf(loc.getBlockY()));
        message = replace(message, "{z}", String.valueOf(loc.getBlockZ()));
        message = replace(message, "{yaw}", String.valueOf(AlgebraUtils.integerRound(loc.getYaw())));
        message = replace(message, "{pitch}", String.valueOf(AlgebraUtils.integerRound(loc.getPitch())));
        message = replace(message, "{cps}", String.valueOf(p.spartan.clicks.getCount()));

        if (hackType != null) {
            message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(p.spartan.dataType, worldName)));
            message = replace(message, "{punish:detection}", String.valueOf(hackType.getCheck().canPunish(p.spartan.dataType)));
        }
        return ChatColor.translateAlternateColorCodes('&', replaceWithSyntax(message, hackType));
    }

    public static String replaceWithSyntax(OfflinePlayer off, String message, HackType hackType) {
        boolean hasHackType = hackType != null;

        if (off.isOnline()) {
            SpartanProtocol p = SpartanBukkit.getProtocol((Player) off);
            return replaceWithSyntax(p, message, hackType);
        } else {
            UUID uuid = off.getUniqueId();
            String name = off.getName();

            if (name != null) {
                message = replace(message, "{player}", name);
            }
            message = replace(message, "{uuid}", uuid.toString());

            if (hasHackType) {
                message = replace(message, "{silent:detection}", String.valueOf(hackType.getCheck().isSilent(null, null)));
                message = replace(message, "{punish:detection}", String.valueOf(hackType.getCheck().canPunish(null)));
            }
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
