package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.api.PlayerFoundOreEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class AntiCheatLogs {

    public static final String
            dateFormatChanger = ".",
            usualDateFormat = "yyyy/MM/dd HH:mm:ss",
            dateFormat = "yyyy/MM/dd HH:mm:ss" + dateFormatChanger + "SSSSSSSSS",
            folderPath = Register.plugin.getDataFolder() + "/logs";

    private static Timestamp time = new Timestamp(System.currentTimeMillis());
    private static File savedFile = createFile(time);
    private static YamlConfiguration fileConfiguration = null;

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

    private static void storeInFile(String information) {
        if (fileConfiguration != null) {
            SpartanBukkit.dataThread.executeIfSyncElseHere(() -> {
                fileConfiguration.set(
                        "(" + new Random().nextInt() + ")"
                                + "[" + DateTimeFormatter.ofPattern(dateFormat).format(LocalDateTime.now()) + "]",
                        information
                );
                save();
            });
        }
    }

    // Separator

    public static void logInfo(SpartanPlayer p, String info, boolean console) {
        logInfo(p, info, console, null, null);
    }

    public static void logInfo(SpartanPlayer p,
                               String information,
                               boolean console,
                               Material material,
                               PlayerViolation playerViolation) {
        if (console && Config.settings.getBoolean("Logs.log_console")) {
            Bukkit.getConsoleSender().sendMessage(information);
        }
        if (Config.settings.getBoolean("Logs.log_file")) {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            if (time.getDay() != now.getDay()) {
                if (fileConfiguration != null) {
                    save();
                }
                time = now;
                savedFile = createFile(time);

                try {
                    if (savedFile.createNewFile()) {
                        fileConfiguration = YamlConfiguration.loadConfiguration(savedFile);
                        storeInFile(information);
                    } else {
                        fileConfiguration = null;
                        savedFile = null;
                    }
                } catch (Exception ignored) {
                    fileConfiguration = null;
                    savedFile = null;
                }
            } else {
                storeInFile(information);
            }
        }
        Config.sql.logInfo(p, information, material, playerViolation);
    }

    public static void logMining(SpartanPlayer player, SpartanBlock block, boolean cancelled) {
        if (player.getGameMode() == GameMode.SURVIVAL
                && PlayerUtils.isPickaxeItem(player.getItemInHand().getType())) {
            MiningHistory.MiningOre ore = MiningHistory.getMiningOre(block.material);

            if (ore != null) {
                SpartanLocation location = player.movement.getLocation();
                World.Environment environment = location.world.getEnvironment();
                int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ(), amount = 1;
                String key = ore.toString(),
                        log = player.name + " found " + amount + " " + key
                                + " on " + x + ", " + y + ", " + z + ", " + BlockUtils.environmentToString(environment);

                // API Event
                PlayerFoundOreEvent event;

                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    event = new PlayerFoundOreEvent(player.getInstance(), log, location.getBukkitLocation(), block.material);
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }

                if (event == null || !event.isCancelled()) {
                    AntiCheatLogs.logInfo(
                            player,
                            log,
                            false,
                            block.material,
                            null
                    );
                    MiningHistory miningHistory = player.getProfile().getMiningHistory(ore);

                    if (miningHistory != null) {
                        String pluralKey = key.endsWith("s") ? (key + "es") : (key + "s");
                        miningHistory.increaseMines(environment, amount);
                        player.getExecutor(Enums.HackType.XRay).handle(
                                cancelled,
                                new Object[]{environment, miningHistory, ore, pluralKey});
                    }
                }
            }
        }
    }

}
