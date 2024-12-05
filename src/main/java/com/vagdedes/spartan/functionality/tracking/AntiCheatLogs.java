package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.api.PlayerFoundOreEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AntiCheatLogs {

    public static final String
            dateFormat = "yyyy/MM/dd HH:mm:ss:SSSSSSSSS",
            folderPath = Register.plugin.getDataFolder() + "/logs";

    private static Timestamp time = new Timestamp(System.currentTimeMillis());
    private static File savedFile = createFile(time);
    private static YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(savedFile);

    private static File createFile(Timestamp timestamp) {
        return new File(
                folderPath + "/log"
                        + TimeUtils.getYearMonthDay(timestamp)
                        .replace(TimeUtils.dateSeparator, "")
                        + ".yml"
        );
    }

    private static void save() {
        try {
            fileConfiguration.save(savedFile);
        } catch (Exception ignored) {
        }
    }

    // Separator

    public static void rawLogInfo(long time, String information, boolean console, boolean store, boolean sql) {
        if (console && Config.settings.getBoolean("Logs.log_console")) {
            Bukkit.getConsoleSender().sendMessage(information);
        }
        if (Config.settings.getBoolean("Logs.log_file")) {
            Timestamp now = new Timestamp(time);

            if (AntiCheatLogs.time.getDay() != now.getDay()) {
                if (fileConfiguration != null) {
                    save();
                }
                AntiCheatLogs.time = now;
                savedFile = createFile(AntiCheatLogs.time);
                fileConfiguration = YamlConfiguration.loadConfiguration(savedFile);
            } else if (store && fileConfiguration != null) {
                SpartanBukkit.dataThread.executeIfSyncElseHere(() -> {
                    fileConfiguration.set(
                            DateTimeFormatter.ofPattern(dateFormat).format(LocalDateTime.now()),
                            information
                    );
                    save();
                });
            }
        }
        if (sql) {
            Config.sql.logInfo(null, null, information, null, null, System.currentTimeMillis());
        }
    }

    public static void logInfo(SpartanProtocol p,
                               String notification,
                               String information,
                               boolean console,
                               Material material,
                               Enums.HackType hackType,
                               long time,
                               boolean unlikely) {
        rawLogInfo(time, information, console, !unlikely, false);
        Config.sql.logInfo(p, notification, information, material, hackType, time);
    }

    public static void logMining(SpartanProtocol protocol, Block block, boolean cancelled) {
        if (protocol.bukkit.getGameMode() == GameMode.SURVIVAL
                && PlayerUtils.isPickaxeItem(protocol.spartan.getItemInHand().getType())) {
            MiningHistory.MiningOre ore = MiningHistory.getMiningOre(block.getType());

            if (ore != null) {
                World.Environment environment = block.getWorld().getEnvironment();
                int x = block.getX(), y = block.getY(), z = block.getZ(), amount = 1;
                String key = ore.toString(),
                        log = protocol.bukkit.getName() + MiningHistory.found + amount + " " + key
                                + " on " + x + ", " + y + ", " + z + ", " + BlockUtils.environmentToString(environment);

                // API Event
                PlayerFoundOreEvent event;

                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    event = new PlayerFoundOreEvent(protocol.bukkit, log, block.getLocation(), block.getType());
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }

                if ((event == null || !event.isCancelled())
                        && Enums.HackType.XRay.getCheck().isEnabled(protocol.spartan.dataType, protocol.spartan.getWorld().getName())) {
                    AntiCheatLogs.logInfo(
                            protocol,
                            null,
                            log,
                            false,
                            block.getType(),
                            null,
                            System.currentTimeMillis(),
                            false
                    );
                    MiningHistory miningHistory = protocol.getProfile().getMiningHistory(ore);

                    if (miningHistory != null) {
                        String pluralKey = key.endsWith("s") ? (key + "es") : (key + "s");
                        miningHistory.increaseMines(environment, amount);
                        protocol.spartan.getRunner(Enums.HackType.XRay).handle(
                                cancelled,
                                new Object[]{environment, miningHistory, ore, pluralKey});
                    }
                }
            }
        }
    }

}
