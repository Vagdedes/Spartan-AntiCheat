package me.vagdedes.spartan.features.notifications;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.PlayerFoundOreEvent;
import me.vagdedes.spartan.checks.world.XRay;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.configuration.AntiCheatLogs;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.profiling.MiningHistory;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
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

    public static int getPlayersRawSize() {
        return notifications.size();
    }

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
        return notifications.containsKey(p.getUniqueId());
    }

    public static boolean hasPermission(SpartanPlayer p) {
        return Permissions.has(p, Enums.Permission.NOTIFICATIONS);
    }

    public static Integer getDivisor(SpartanPlayer p, boolean absolute) {
        Integer divisor = notifications.get(p.getUniqueId());
        return divisor != null ? (absolute ? Math.abs(divisor) : divisor) : null;
    }

    public static boolean canAcceptMessages(SpartanPlayer p, Integer divisor, boolean all) {
        return divisor != null
                && (divisor < 0

                || (all
                || TestServer.isIdentified()
                || !SpartanBukkit.isProductionServer()
                || !ResearchEngine.enoughData()
                || divisor >= 1
                || Settings.getBoolean("Notifications.individual_only_notifications"))
                && hasPermission(p));
    }

    // Executors

    public static void toggle(SpartanPlayer p, int i) {
        if (!isEnabled(p)) {
            change(p, i, false);
        } else {
            notifications.remove(p.getUniqueId());
            p.sendMessage(Messages.get("notifications_disable").replace("{type}", ""));
        }
    }

    public static void set(SpartanPlayer p, boolean value, int i) {
        if (value) {
            if (!isEnabled(p)) {
                change(p, i, false);
            }
        } else if (notifications.remove(p.getUniqueId()) != null) {
            p.sendMessage(Messages.get("notifications_disable").replace("{type}", "Verbose"));
        }
    }

    public static void change(SpartanPlayer p, int i, boolean change) {
        notifications.put(p.getUniqueId(), i);
        p.sendMessage(Messages.get(change ? "notifications_modified" : "notifications_enable").replace("{type}", ""));
    }

    // Handlers

    public static Enums.MiningOre getMiningOre(Material material) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
            if (material == Material.ANCIENT_DEBRIS) {
                return Enums.MiningOre.ANCIENT_DEBRIS;
            }
            if (material == Material.GILDED_BLACKSTONE || material == Material.NETHER_GOLD_ORE) {
                return Enums.MiningOre.Gold;
            }
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                if (material == Material.DEEPSLATE_DIAMOND_ORE) {
                    return Enums.MiningOre.DIAMOND;
                }
                if (material == Material.DEEPSLATE_EMERALD_ORE) {
                    return Enums.MiningOre.EMERALD;
                }
                if (material == Material.DEEPSLATE_GOLD_ORE) {
                    return Enums.MiningOre.Gold;
                }
            }
        }
        switch (material) {
            case DIAMOND_ORE:
                return Enums.MiningOre.DIAMOND;
            case EMERALD_ORE:
                return Enums.MiningOre.EMERALD;
            case GOLD_ORE:
                return Enums.MiningOre.Gold;
        }
        return null;
    }

    public static void runMining(SpartanPlayer player, SpartanBlock block) {
        if (player.getGameMode() == GameMode.SURVIVAL && PlayerData.isPickaxeItem(player.getItemInHand().getType())) {
            Material material = block.getType();
            Enums.MiningOre ore = getMiningOre(material);

            if (ore != null) {
                SpartanLocation location = player.getLocation();
                World.Environment environment = location.getWorld().getEnvironment();
                int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ(), amount = 1;
                String key = ore.getString(),
                        log = player.getName() + " found " + amount + " " + key
                                + " on " + x + ", " + y + ", " + z + ", " + BlockUtils.environmentToString(environment);

                // API Event
                PlayerFoundOreEvent event;

                if (Settings.getBoolean("Important.enable_developer_api")) {
                    event = new PlayerFoundOreEvent(player.getPlayer(), log, location.getLimitedBukkitLocation(), material);
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }

                if (event == null || !event.isCancelled()) {
                    AntiCheatLogs.logInfo(
                            player,
                            log,
                            null,
                            material,
                            XRay.check,
                            false,
                            true,
                            XRay.check.getCheck().getViolations(player).getLevel(),
                            CancelViolation.get(XRay.check, player.getDataType()));


                    if (!ResearchEngine.isCaching()) {
                        MiningHistory miningHistory = player.getProfile().getMiningHistory(ore);

                        if (miningHistory != null) {
                            String pluralKey = key.endsWith("s") ? (key + "es") : (key + "s");
                            miningHistory.increaseMines(environment, amount);
                            XRay.run(player,
                                    environment,
                                    miningHistory,
                                    ore, pluralKey);
                        }
                    }
                }
            }
        }
    }
}
