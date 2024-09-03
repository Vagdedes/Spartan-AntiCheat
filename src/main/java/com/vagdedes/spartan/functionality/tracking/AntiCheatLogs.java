package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
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

    private static void storeInFile(String information) {
        if (fileConfiguration != null) {
            SpartanBukkit.dataThread.executeIfSyncElseHere(() -> {
                fileConfiguration.set(
                        DateTimeFormatter.ofPattern(dateFormat).format(LocalDateTime.now()),
                        information
                );
                save();
            });
        }
    }

    // Separator

    public static void logInfo(SpartanPlayer p,
                               String notification,
                               String information,
                               boolean console,
                               Material material,
                               Enums.HackType hackType,
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
                fileConfiguration = YamlConfiguration.loadConfiguration(savedFile);
            } else {
                storeInFile(information);
            }
        }
        Config.sql.logInfo(p, notification, information, material, hackType, playerViolation);
    }

    public static void logMining(SpartanPlayer player, SpartanBlock block, boolean cancelled) {
        if (player.getInstance().getGameMode() == GameMode.SURVIVAL
                && PlayerUtils.isPickaxeItem(player.getItemInHand().getType())) {
            MiningHistory.MiningOre ore = MiningHistory.getMiningOre(block.material);

            if (ore != null) {
                SpartanLocation location = player.movement.getLocation();
                World.Environment environment = location.world.getEnvironment();
                int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ(), amount = 1;
                String key = ore.toString(),
                        log = player.getInstance().getName() + " found " + amount + " " + key
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
                            null,
                            log,
                            false,
                            block.material,
                            null,
                            null
                    );
                    MiningHistory miningHistory = player.protocol.getProfile().getMiningHistory(ore);

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
