package me.vagdedes.spartan.api;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.moderation.BanManagement;
import com.vagdedes.spartan.functionality.moderation.Wave;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.connection.Latency;
import com.vagdedes.spartan.handlers.stability.*;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BackgroundAPI {

    static String licenseID() {
        return IDs.user();
    }

    static String getVersion() {
        return Register.plugin != null ? Register.plugin.getDescription().getVersion() : "Unknown";
    }

    static String getMessage(String path) {
        return Config.messages.getColorfulString(path);
    }

    static boolean getSetting(String path) {
        return Config.settings.getBoolean(path);
    }

    static String getCategory(Player p, HackType hackType) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);

        if (player != null) {
            return Check.getCategoryFromViolations(
                    player.getViolations(hackType).getLevel(),
                    hackType,
                    player.getProfile().isSuspectedOrHacker(hackType)
            ).toString();
        }
        return Enums.PunishmentCategory.UNLIKE.toString();
    }

    @Deprecated
    static boolean hasVerboseEnabled(Player p) {
        return hasNotificationsEnabled(p);
    }

    static boolean hasNotificationsEnabled(Player p) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player != null && DetectionNotifications.isEnabled(player);
    }

    static int getViolationResetTime() {
        return (int) (Check.violationCycleSeconds / 1_000L);
    }

    @Deprecated
    static void setVerbose(Player p, boolean value) {
        setNotifications(p, value);
    }

    static void setNotifications(Player p, boolean value) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanPlayer player = SpartanBukkit.getPlayer(p);

            if (player != null) {
                DetectionNotifications.set(player, value, -1);
            }
        }
    }

    @Deprecated
    static void setVerbose(Player p, boolean value, int frequency) {
        setNotifications(p, frequency);
    }

    static void setNotifications(Player p, int frequency) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanPlayer player = SpartanBukkit.getPlayer(p);

            if (player != null) {
                DetectionNotifications.set(player, true, -Math.max(1, Math.abs(frequency)));
            }
        }
    }

    static int getPing(Player p) {
        return Latency.ping(p);
    }

    static double getTPS() {
        return TPS.get(null, false);
    }

    static boolean hasPermission(Player p, Permission Permission) {
        return Permissions.has(p, Permission);
    }

    static boolean isEnabled(HackType HackType) {
        return HackType.getCheck().isEnabled(null, null, null);
    }

    static boolean isSilent(HackType HackType) {
        return HackType.getCheck().isSilent(null, null);
    }

    static int getVL(Player p, HackType hackType) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);

        if (player != null) {
            return player.getViolations(hackType).getLevel();
        } else {
            return 0;
        }
    }

    @Deprecated
    static double getDecimalVL(Player p, HackType HackType) {
        AwarenessNotifications.forcefullySend("The API method 'getDecimalVL' has been removed.");
        return 0.0;
    }

    static int getVL(Player p) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);

        if (player != null) {
            return player.getViolationCount();
        } else {
            return 0;
        }
    }

    @Deprecated
    static void setVL(Player p, HackType HackType, int amount) {
        AwarenessNotifications.forcefullySend("The API method 'setVL' has been removed.");
    }

    @Deprecated
    static int getCancelViolation(HackType hackType, String worldName) {
        return getCancelViolation(hackType);
    }

    static int getCancelViolation(HackType hackType) {
        return CancelViolation.get(hackType, ResearchEngine.DataType.Universal);
    }

    static int getViolationDivisor(Player p, HackType hackType) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player == null ? 0 :
                NotifyViolation.get(player, null, SpartanBukkit.getPlayerCount(),
                        CancelViolation.get(
                                hackType,
                                player.getDataType()
                        ),
                        TestServer.isIdentified());
    }

    static void reloadConfig() {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Config.reload(null);
        }
    }

    static void reloadPermissions() {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Permissions.clear();
        }
    }

    static void reloadPermissions(Player p) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Permissions.remove(p.getUniqueId());
        }
    }

    static void enableCheck(HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().setEnabled(null, true);
        }
    }

    static void disableCheck(HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().setEnabled(null, false);
        }
    }

    static void enableSilentChecking(Player p, HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().addSilentUser(p.getUniqueId(), "Developer-API", 0);
        }
    }

    static void disableSilentChecking(Player p, HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().removeSilentUser(p.getUniqueId());
        }
    }

    static void enableSilentChecking(HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().setSilent("true");
        }
    }

    static void disableSilentChecking(HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().setSilent("false");
        }
    }

    static void cancelCheck(Player p, HackType HackType, int ticks) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().addDisabledUser(p.getUniqueId(), "Developer-API", ticks);
        }
    }

    static void cancelCheckPerVerbose(Player p, String string, int ticks) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) { // Keep the null pointer protection to prevent the method from acting differently
            UUID uuid = p.getUniqueId();

            for (HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().addDisabledUser(uuid, "Developer-API", string, ticks);
            }
        }
    }

    static void startCheck(Player p, HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().addDisabledUser(p.getUniqueId(), "Developer-API", 0);
        }
    }

    static void stopCheck(Player p, HackType HackType) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType.getCheck().removeDisabledUser(p.getUniqueId());
        }
    }

    static void resetVL() {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            HackType[] hackTypes = Enums.HackType.values();

            for (SpartanPlayer player : SpartanBukkit.getPlayers()) {
                for (HackType hackType : hackTypes) {
                    player.getViolations(hackType).reset();
                }
            }
        }
    }

    static void resetVL(Player p) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanPlayer player = SpartanBukkit.getPlayer(p);

            if (player != null) {
                for (HackType hackType : Enums.HackType.values()) {
                    player.getViolations(hackType).reset();
                }
            }
        }
    }

    static boolean isBypassing(Player p) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player != null && Permissions.isBypassing(player, null);
    }

    static boolean isBypassing(Player p, HackType HackType) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player != null && Permissions.isBypassing(player, HackType);
    }

    static void banPlayer(UUID uuid, String reason) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            BanManagement.ban(uuid, Bukkit.getConsoleSender(), reason, 0L);
        }
    }

    static boolean isBanned(UUID uuid) {
        return BanManagement.isBanned(uuid);
    }

    static void unbanPlayer(UUID uuid) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            BanManagement.unban(uuid, false);
        }
    }

    static String getBanReason(UUID uuid) {
        return BanManagement.get(uuid, "reason");
    }

    static String getBanPunisher(UUID uuid) {
        return BanManagement.get(uuid, "punisher");
    }

    static boolean isHacker(Player p) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player != null && player.getProfile().isHacker();
    }

    static boolean isLegitimate(Player p) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player != null && player.getProfile().isLegitimate();
    }

    @Deprecated
    static boolean hasMiningNotificationsEnabled(Player p) {
        return hasNotificationsEnabled(p);
    }

    @Deprecated
    static void setMiningNotifications(Player p, boolean value) {
        setNotifications(p, value);
    }

    static int getCPS(Player p) {
        SpartanPlayer player = SpartanBukkit.getPlayer(p);
        return player == null ? 0 : player.getClicks().getCount();
    }

    static UUID[] getBanList() {
        return BanManagement.getBanList().toArray(new UUID[0]);
    }

    static boolean addToWave(UUID uuid, String command) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Wave.add(uuid, command);
            return true;
        }
        return false;
    }

    static void removeFromWave(UUID uuid) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Wave.remove(uuid);
        }
    }

    static void clearWave() {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Wave.clear();
        }
    }

    static void runWave() {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Wave.start();
        }
    }

    static UUID[] getWaveList() {
        return Wave.getWaveList();
    }

    static int getWaveSize() {
        return Wave.getWaveList().length;
    }

    static boolean isAddedToTheWave(UUID uuid) {
        return Wave.getCommand(uuid) != null;
    }

    static void warnPlayer(Player p, String reason) {
        AwarenessNotifications.forcefullySend("The API method 'warnPlayer' has been removed.");
    }

    static void addPermission(Player p, Permission permission) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            Permissions.add(p, permission);
        }
    }

    @Deprecated
    static void sendClientSidedBlock(Player p, Location loc, Material m, byte b) {
        AwarenessNotifications.forcefullySend("The API method 'sendClientSidedBlock' has been removed.");
    }

    @Deprecated
    static void destroyClientSidedBlock(Player p, Location loc) {
        AwarenessNotifications.forcefullySend("The API method 'destroyClientSidedBlock' has been removed.");
    }

    @Deprecated
    static void removeClientSidedBlocks(Player p) {
        AwarenessNotifications.forcefullySend("The API method 'removeClientSidedBlocks' has been removed.");
    }

    @Deprecated
    static boolean containsClientSidedBlock(Player p, Location loc) {
        AwarenessNotifications.forcefullySend("The API method 'containsClientSidedBlock' has been removed.");
        return false;
    }

    @Deprecated
    static Material getClientSidedBlockMaterial(Player p, Location loc) {
        AwarenessNotifications.forcefullySend("The API method 'getClientSidedBlockMaterial' has been removed.");
        return null;
    }

    @Deprecated
    static byte getClientSidedBlockData(Player p, Location loc) {
        AwarenessNotifications.forcefullySend("The API method 'getClientSidedBlockData' has been removed.");
        return (byte) 0;
    }

    static String getConfiguredCheckName(HackType hackType) {
        return hackType.getCheck().getName();
    }

    static void setConfiguredCheckName(HackType hackType, String name) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            hackType.getCheck().setName(name);
        }
    }

    static void disableVelocityProtection(Player p, int ticks) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanPlayer player = SpartanBukkit.getPlayer(p);

            if (player != null) {
                Handlers handlers = player.getHandlers();
                handlers.disable(Handlers.HandlerType.Velocity, ticks);
                handlers.remove(Handlers.HandlerType.Velocity);
            }
        }
    }

    static void setOnGround(Player p, int ticks) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanPlayer player = SpartanBukkit.getPlayer(p);

            if (player != null) {
                GroundUtils.setOnGround(player, ticks);
            }
        }
    }

    @Deprecated
    static int getMaxPunishmentViolation(HackType hackType) {
        AwarenessNotifications.forcefullySend("The API method 'getMaxPunishmentViolation' has been removed.");
        return 0;
    }

    @Deprecated
    static int getMinPunishmentViolation(HackType hackType) {
        AwarenessNotifications.forcefullySend("The API method 'getMinPunishmentViolation' has been removed.");
        return 0;
    }

    @Deprecated
    static boolean mayPunishPlayer(Player p, HackType hackType) {
        AwarenessNotifications.forcefullySend("The API method 'mayPunishPlayer' has been removed.");
        return false;
    }
}
