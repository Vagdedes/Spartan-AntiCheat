package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.api.PlayerFoundOreEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;

public class DetectionNotifications {

    private static final Map<UUID, Integer> notifications = new LinkedHashMap<>(Config.getMaxPlayers());

    // Base

    public static void clear() {
        notifications.clear();
    }

    // General

    public static List<SpartanPlayer> getPlayers(boolean all) {
        int size = notifications.size();

        if (size > 0) {
            List<SpartanPlayer> list = new ArrayList<>(size);

            for (Map.Entry<UUID, Integer> entry : notifications.entrySet()) {
                SpartanPlayer p = SpartanBukkit.getPlayer(entry.getKey());

                if (p != null && canAcceptMessages(p, entry.getValue(), all)) {
                    list.add(p);
                }
            }
            return list;
        }
        return new ArrayList<>(0);
    }

    // Feedback

    public static boolean isEnabled(SpartanPlayer p) {
        return notifications.containsKey(p.uuid);
    }

    public static boolean hasPermission(SpartanPlayer p) {
        return Permissions.has(p, Enums.Permission.NOTIFICATIONS);
    }

    public static Integer getDivisor(SpartanPlayer p, boolean absolute) {
        Integer divisor = notifications.get(p.uuid);
        return divisor != null ? (absolute ? Math.abs(divisor) : divisor) : null;
    }

    public static boolean canAcceptMessages(SpartanPlayer p, Integer divisor, boolean all) {
        return divisor != null
                && (divisor < 0

                || (all
                || !ResearchEngine.enoughData()
                || divisor >= 1
                || Config.settings.getBoolean("Notifications.individual_only_notifications"))
                && hasPermission(p));
    }

    // Executors

    public static void toggle(SpartanPlayer p, int i) {
        if (!isEnabled(p)) {
            change(p, i, false);
        } else {
            notifications.remove(p.uuid);
            p.sendMessage(Config.messages.getColorfulString("notifications_disable").replace("{type}", ""));
        }
    }

    public static void set(SpartanPlayer p, boolean value, int i) {
        if (value) {
            if (!isEnabled(p)) {
                change(p, i, false);
            }
        } else if (notifications.remove(p.uuid) != null) {
            p.sendMessage(Config.messages.getColorfulString("notifications_disable").replace("{type}", "Verbose"));
        }
    }

    public static void change(SpartanPlayer p, int i, boolean change) {
        notifications.put(p.uuid, i);
        p.sendMessage(Config.messages.getColorfulString(change ? "notifications_modified" : "notifications_enable").replace("{type}", ""));
    }

    // Handlers

    public static Enums.MiningOre getMiningOre(Material material) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
            if (material == Material.ANCIENT_DEBRIS) {
                return Enums.MiningOre.ANCIENT_DEBRIS;
            }
            if (material == Material.GILDED_BLACKSTONE || material == Material.NETHER_GOLD_ORE) {
                return Enums.MiningOre.GOLD;
            }
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                if (material == Material.DEEPSLATE_DIAMOND_ORE) {
                    return Enums.MiningOre.DIAMOND;
                }
                if (material == Material.DEEPSLATE_EMERALD_ORE) {
                    return Enums.MiningOre.EMERALD;
                }
                if (material == Material.DEEPSLATE_GOLD_ORE) {
                    return Enums.MiningOre.GOLD;
                }
            }
        }
        switch (material) {
            case DIAMOND_ORE:
                return Enums.MiningOre.DIAMOND;
            case EMERALD_ORE:
                return Enums.MiningOre.EMERALD;
            case GOLD_ORE:
                return Enums.MiningOre.GOLD;
        }
        return null;
    }

    public static void runMining(SpartanPlayer player, SpartanBlock block, boolean cancelled) {
        if (player.getGameMode() == GameMode.SURVIVAL && PlayerUtils.isPickaxeItem(player.getItemInHand().getType())) {
            Enums.MiningOre ore = getMiningOre(block.material);

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
                    event = new PlayerFoundOreEvent(player.getInstance(), log, location.getLimitedBukkitLocation(), block.material);
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }

                if (event == null || !event.isCancelled()) {
                    AntiCheatLogs.logInfo(
                            player,
                            log,
                            null,
                            block.material,
                            Enums.HackType.XRay,
                            true,
                            -1
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
