package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AntiCheatLogs {

    private static final String dateOnlyFormat = "yyyyMMdd";
    public static final String
            dateFormat = "yyyy/MM/dd HH:mm:ss:SSSXXX",
            folderPath = Register.plugin.getDataFolder() + "/logs",
            playerIdentifier = "Player:";

    private static File todayFile = getFile();
    private static YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(todayFile);

    private static final Object synchronizer = new Object();
    private static boolean saving = false;

    public static String getDate(String pattern, long time) {
        return DateTimeFormatter.ofPattern(pattern).format(
                Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        );
    }

    private static String getFileName() {
        return "log"
                + ZonedDateTime.now().format(DateTimeFormatter.ofPattern(dateOnlyFormat))
                + ".yml";
    }

    private static File getFile() {
        return new File(
                folderPath + "/" + getFileName()
        );
    }

    private static void rawSave() {
        try {
            fileConfiguration.save(todayFile);
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        if (!saving) {
            synchronized (synchronizer) {
                saving = true;
                rawSave();
                saving = false;
            }
        }
    }

    // Separator

    public static void rawLogInfo(long time, String information, boolean console, boolean store, boolean sql) {
        if (console && Config.settings.getBoolean("Logs.log_console")) {
            Bukkit.getConsoleSender().sendMessage(information);
        }
        if (Config.settings.getBoolean("Logs.log_file")) {
            if (store && fileConfiguration != null) {
                String fileName = getFileName();

                if (!fileName.equals(todayFile.getName())) {
                    rawSave();
                    todayFile = getFile();
                    fileConfiguration = YamlConfiguration.loadConfiguration(todayFile);
                }
                PluginBase.dataThread.executeIfUnknownThreadElseHere(() -> {
                    fileConfiguration.set(
                            AntiCheatLogs.getDate(AntiCheatLogs.dateFormat, time),
                            information
                    );
                    AntiCheatLogs.save();
                });
            }
        }
        if (sql) {
            Config.sql.logInfo(null, null, information, null, null, System.currentTimeMillis());
        }
    }

    public static void logInfo(PlayerProtocol p,
                               String notification,
                               String information,
                               boolean console,
                               Material material,
                               Enums.HackType hackType,
                               long time) {
        rawLogInfo(time, information, console, true, false);
        Config.sql.logInfo(p, notification, information, material, hackType, time);
    }

}
