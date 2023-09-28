package me.vagdedes.spartan.configuration;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.api.SpartanReloadEvent;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.functionality.moderation.BanManagement;
import me.vagdedes.spartan.functionality.moderation.Wave;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.functionality.synchronicity.DiscordWebhooks;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.gui.SpartanMenu;
import me.vagdedes.spartan.handlers.connection.IDs;
import me.vagdedes.spartan.handlers.connection.Piracy;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.spartan.handlers.stability.Chunks;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.interfaces.listeners.EventsHandler7;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Config {

    public static final String
            legacyFileName = "config.yml",
            fileName = "checks.yml",
            defaultConstruct = "[Spartan " + API.getVersion() + "] ";
    private static YamlConfiguration configuration = null;
    private static String construct = defaultConstruct;
    private static boolean legacyFile = false;
    private static int maxPlayers = 20;
    public static Settings settings = new Settings();
    public static SQLFeature sql = new SQLFeature();
    public static Messages messages = new Messages();
    public static Compatibility compatibility = new Compatibility();

    static {
        refreshVariables(false);
    }

    // Separator

    public static int getMaxPlayers() {
        return maxPlayers;
    }

    public static boolean isLegacy() {
        return legacyFile;
    }

    public static File getFile() {
        return new File(Register.plugin.getDataFolder() + "/" + (isLegacy() ? legacyFileName : fileName));
    }

    public static YamlConfiguration getConfiguration() { // Synchronise it in all uses
        if (configuration == null) {
            File file = getFile();

            if (file.exists()) {
                configuration = YamlConfiguration.loadConfiguration(file);
            } else {
                try {
                    if (file.createNewFile()) {
                        configuration = YamlConfiguration.loadConfiguration(file);
                    } else {
                        configuration = null;
                    }
                } catch (Exception ignored) {
                    configuration = null;
                }
            }
        }
        return configuration;
    }

    public static File getAlternativeFile() {
        return new File(Register.plugin.getDataFolder() + "/" + (!isLegacy() ? legacyFileName : fileName));
    }

    public static String getConstruct() {
        return construct;
    }

    // Separator

    public static int getMaxPunishmentViolation(HackType hackType, ResearchEngine.DataType dataType) {
        if (!Config.isLegacy()) {
            return Check.getCategoryPunishment(hackType, dataType, Enums.PunishmentCategory.ABSOLUTE);
        }
        Check check = hackType.getCheck();

        for (int i = 1; i <= Check.maxViolationsPerCycle; i++) {
            for (String s : check.getLegacyCommands(i)) {
                if (s != null) {
                    return i;
                }
            }
        }
        return 0;
    }

    public static int getMinPunishmentViolation(HackType hackType, ResearchEngine.DataType dataType) {
        if (!Config.isLegacy()) {
            return Check.getCategoryPunishment(hackType, dataType, Enums.PunishmentCategory.UNLIKE);
        }
        Check check = hackType.getCheck();

        for (int i = 1; i <= Check.maxViolationsPerCycle; i++) {
            for (String s : check.getLegacyCommands(i)) {
                if (s != null) {
                    return i;
                }
            }
        }
        return 0;
    }

    // Separator

    public static Check getCheckByName(String s) {
        for (HackType hackType : Enums.HackType.values()) {
            Check check = hackType.getCheck();
            String checkName = check.getName();

            if (checkName != null && checkName.equals(s)) {
                return check;
            }
        }
        return null;
    }

    // Separator

    public static void refreshVariables(boolean clearChecksCache) {
        // Config Utilities Cache
        ConfigUtils.clear();

        // Memory Allowance
        int max = Math.max(Bukkit.getMaxPlayers(), maxPlayers); // static, dynamic & past/minimum
        maxPlayers = Math.min(max, 500);

        // Legacy Configuration
        String path = Register.plugin.getDataFolder() + "/";
        File file = new File(path + legacyFileName);

        if (file.exists()) {
            legacyFile = true;
            configuration = YamlConfiguration.loadConfiguration(file);
            Register.plugin.reloadConfig();
        } else {
            legacyFile = false;
            file = new File(path + fileName);

            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        configuration = YamlConfiguration.loadConfiguration(file);
                    } else {
                        configuration = null;
                    }
                } catch (Exception ignored) {
                    configuration = null;
                }
            } else {
                configuration = YamlConfiguration.loadConfiguration(file);
            }
        }

        // Identification & Labelling
        if (Piracy.enabled) {
            construct = "[Spartan " + API.getVersion() + "/" + IDs.hide(IDs.user()) + "/" + IDs.hide(IDs.nonce()) + "] ";
        } else if (IDs.isValid()) {
            construct = "[Spartan " + API.getVersion() + "/" + IDs.hide(IDs.user()) + "] ";
        } else {
            construct = defaultConstruct;
        }
        CloudFeature.clear(false);
        CrossServerInformation.refresh();

        // Check Cache
        if (clearChecksCache) {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                for (HackType hackType : Enums.HackType.values()) {
                    hackType.resetCheck();

                    for (SpartanPlayer p : players) {
                        p.getViolations(hackType).reset();
                    }
                }
            } else {
                for (HackType hackType : Enums.HackType.values()) {
                    hackType.resetCheck();
                }
            }
        }
    }

    public static void createConfigurations(boolean local) {
        if (Config.isLegacy()) {
            Register.plugin.reloadConfig();
        }
        if (!local) { // Always first
            Config.getAlternativeFile().delete();
            File file = getFile();

            if (file.exists()) {
                CrossServerInformation.sendConfiguration(file);
            }
        }
        settings.create(local); // Always Second (Research Engine File Logs)
        sql.create(local); // Always Third (Research Engine SQL Logs)
        messages.create(local);
        Compatibility.create(local);
        BanManagement.create(local);
        Wave.create(local);
    }

    // Separator

    public static void create() {
        boolean enabledPlugin = Register.isPluginEnabled();

        // Utilities (Always Before Configuration)
        refreshVariables(true);

        // Configuration (Always First)
        if (enabledPlugin) {
            createConfigurations(false); // Always First
        } else {
            settings.clear();
            sql.refreshConfiguration();
            messages.clear();
            compatibility.clear();
            BanManagement.clear();
            Wave.clearCache();
        }

        // Utilities
        PluginUtils.clear();

        // System
        Cache.clearStorage(!enabledPlugin);
        Chunks.reload(enabledPlugin);
        Permissions.clear();

        // Features
        SpartanMenu.manageConfiguration.clear();
        AwarenessNotifications.refresh();

        // Objects
        SpartanPlayer.clear();

        // Handlers
        ResearchEngine.refresh(true, enabledPlugin);

        // Listeners
        EventsHandler7.refresh();

        // Notifications (Always Last)
        if (enabledPlugin && AwarenessNotifications.areEnabled()) {
            boolean legacy = isLegacy(),
                    discord = DiscordWebhooks.isUsing() && !ResearchEngine.enoughData();

            if (legacy || discord) {
                String legacyConfigurationMessage = AwarenessNotifications.getNotification(
                        "You are using the '" + legacyFileName + "' file, which is a legacy configuration that can cause detection instabilities. " +
                                "Unless you know what you are doing, it's recommended to revert to the new one by deleting '" + legacyFileName + "' and running the command '/spartan reload'.",
                        true
                ), discordWebhooksMessage = AwarenessNotifications.getOptionalNotification(
                        "The Discord Webhooks feature is enabled but needs significant data to function. " +
                                "Please be patient while this data is collected by the anti-cheat."
                );
                List<SpartanPlayer> players = Permissions.getStaff();

                if (players.size() > 0) {
                    for (SpartanPlayer p : players) {
                        UUID uuid = p.getUniqueId();

                        if (legacy && AwarenessNotifications.canSend(uuid, "config")) {
                            p.sendMessage(legacyConfigurationMessage);
                        }
                        if (discord && AwarenessNotifications.canSend(uuid, "discord")) {
                            p.sendMessage(discordWebhooksMessage);
                        }
                    }
                }
            }
        }
    }

    public static void reload(Object sender) {
        if (Config.settings.getBoolean("Important.enable_developer_api")) {
            SpartanReloadEvent event = new SpartanReloadEvent();
            Register.manager.callEvent(event);

            if (event.isCancelled()) {
                return;
            }
        }
        if (sender != null) {
            String message = Config.messages.getColorfulString("config_reload");

            if (sender instanceof CommandSender) {
                ((CommandSender) sender).sendMessage(message);
            } else if (sender instanceof SpartanPlayer) {
                ((SpartanPlayer) sender).sendMessage(message);
            }
        }
        create();
    }

    // Separator

    public static boolean hasCancelAfterViolationOption() {
        boolean cancelAfterViolation = false;

        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (hackType.getCheck().hasCancelViolation()) {
                cancelAfterViolation = true;
                break;
            }
        }
        return cancelAfterViolation;
    }

    // Separator

    public static Collection<HackType> getPunishableHackModules(SpartanPlayer p, HackType hackType, int violation, ResearchEngine.DataType dataType) {
        if (violation % hackType.getCheck().getDefaultCancelViolation() == 0) {
            PlayerProfile profile = p.getProfile();

            if (profile.calculateLiveEvidence(p, hackType, dataType)) {
                Collection<HackType> evidence = profile.getEvidence().getKnowledgeList();
                Iterator<HackType> iterator = evidence.iterator();

                while (iterator.hasNext()) { // Check for punishment allowance
                    HackType loopHackType = iterator.next();

                    if (!loopHackType.getCheck().canPunish()) {
                        iterator.remove();
                    }
                }
                return evidence;
            }
        }
        return new ArrayList<>(0);
    }

    // Separator

    public static void enableChecks() {
        CheckProtection.cancel(20, 0);

        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setEnabled(null, true);
        }
    }

    public static void disableChecks() {
        CheckProtection.cancel(20, 0);

        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setEnabled(null, false);
        }
    }

    // Separator

    public static void enableSilentChecking() {
        CheckProtection.cancel(20, 0);

        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setSilent("true");
        }
    }

    public static void disableSilentChecking() {
        CheckProtection.cancel(20, 0);

        for (HackType hackType : Enums.HackType.values()) {
            hackType.getCheck().setSilent("false");
        }
    }
}
