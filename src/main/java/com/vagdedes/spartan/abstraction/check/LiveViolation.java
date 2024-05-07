package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.InformationAnalysis;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.performance.NotifyViolation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.CheckDelay;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.api.ViolationResetEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class LiveViolation {

    private static final long violationTimeWorth = 2_000L;

    private final SpartanPlayer player;
    private final Enums.HackType hackType;
    private final Map<Integer, Long> level;
    private CancelCause disableCause, silentCause;
    private HackPrevention prevention;

    public LiveViolation(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.level = Collections.synchronizedMap(new LinkedHashMap<>());
        this.prevention = new HackPrevention();
    }

    // Separator

    void run(HackPrevention newPrevention, String information, long time) {
        synchronized (this) {
            if (!CheckDelay.hasCooldown(player)
                    && !CloudBase.isPublicInformationCancelled(hackType, information)
                    && (disableCause == null
                    || !disableCause.pointerMatches(information))) {
                InformationAnalysis analysis = new InformationAnalysis(hackType, information);
                PlayerViolation playerViolation = new PlayerViolation(
                        time,
                        hackType,
                        information,
                        Math.max(
                                this.getLevel(analysis.identity) + 1,
                                AlgebraUtils.integerRound(Math.sqrt(this.getTotalLevel() + 1))
                        ),
                        analysis
                );
                boolean event = Config.settings.getBoolean("Important.enable_developer_api");
                this.player.setLastViolation(this);

                Runnable runnable = () -> {
                    if (event) {
                        PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                                player.getPlayer(),
                                hackType,
                                playerViolation.level,
                                information
                        );
                        Register.manager.callEvent(playerViolationEvent);

                        if (playerViolationEvent.isCancelled()) {
                            return;
                        }
                    }
                    player.getProfile().getViolationHistory(hackType).store(playerViolation);
                    this.increaseLevel(playerViolation.identity);
                    this.performNotification(playerViolation);
                    this.performPunishments(playerViolation);
                    this.prevention = newPrevention;
                    this.prevention.canPrevent = !hackType.getCheck().isSilent(player.getWorld().getName())
                            && (silentCause == null
                            || !silentCause.pointerMatches(information))
                            && (player.getProfile().isSuspectedOrHacker(hackType)
                            || playerViolation.level >= playerViolation.getIgnoredViolations(player));
                };

                if (!event || SpartanBukkit.isSynchronised()) {
                    runnable.run();
                } else {
                    SpartanBukkit.transferTask(player, runnable);
                }
            }
        }
    }

    public boolean prevent() {
        synchronized (this) {
            if (this.prevention.canPrevent
                    && !this.prevention.hasExpired(TPS.getTick(player))) {
                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getPlayer(), hackType);
                    Register.manager.callEvent(checkCancelEvent);

                    if (!checkCancelEvent.isCancelled()) {
                        this.prevention.handle(player);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    this.prevention.handle(player);
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    // Separator

    public int getLevel(int hash) {
        Long time;

        synchronized (this.level) {
            time = this.level.get(hash);
        }
        return time != null ? this.calculateLevel(time) : 0;
    }

    private int calculateLevel(long time) {
        time -= System.currentTimeMillis();
        return time > 0L
                ? AlgebraUtils.integerCeil(time / (double) violationTimeWorth)
                : 0;
    }

    public int getTotalLevel() {
        if (!this.level.isEmpty()) {
            int total = 0;

            for (Long level : this.level.values()) {
                total += this.calculateLevel(level);
            }
            return total;
        } else {
            return 0;
        }
    }

    public boolean hasLevel() {
        if (!this.level.isEmpty()) {
            for (Long level : this.level.values()) {
                if (this.calculateLevel(level) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void increaseLevel(int hash) {
        int multiplier = 1 + player.getProfile().evidence.getKnowledgeList().size();

        synchronized (this.level) {
            Long time = this.level.get(hash);

            if (time == null) {
                this.level.put(hash, System.currentTimeMillis() + (violationTimeWorth * multiplier));
            } else {
                long current = System.currentTimeMillis();

                if (time < current) {
                    this.level.put(hash, current + (violationTimeWorth * multiplier));
                } else {
                    this.level.put(hash, time + (violationTimeWorth * multiplier));
                }
            }
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public void reset() {
        ViolationResetEvent event;

        if (SpartanBukkit.isSynchronised()) {
            Player player = this.player.getPlayer();

            if (player != null && player.isOnline()) {
                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    event = new ViolationResetEvent(player, this.hackType);
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }
            } else {
                event = null;
            }
        } else {
            event = null;
        }

        if (event == null || !event.isCancelled()) {
            synchronized (this.level) {
                this.level.clear();
            }
        }

        // Always last
        player.getProfile().evidence.remove(hackType, true, false, true);
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public CancelCause getDisableCause() {
        return disableCause;
    }

    public CancelCause getSilentCause() {
        return silentCause;
    }

    public void addDisableCause(String reason, String pointer, int ticks) {
        if (disableCause != null) {
            disableCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            disableCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    public void addSilentCause(String reason, String pointer, int ticks) {
        if (silentCause != null) {
            silentCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            silentCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    public void removeDisableCause() {
        this.disableCause = null;
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    public void removeSilentCause() {
        this.silentCause = null;
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    private void performPunishments(PlayerViolation playerViolation) {
        Check check = hackType.getCheck();

        if (check.canPunish) {
            List<String> commands = Config.settings.getPunishmentCommands();

            if (!commands.isEmpty()) {
                Player n = player.getPlayer();

                if (n != null && n.isOnline()) {
                    boolean performed = false, found = false;
                    int index = 0;

                    Collection<Enums.HackType> detectedHacks = player.getProfile().evidence.calculate(player, playerViolation);
                    detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish);

                    if (!detectedHacks.isEmpty()) {
                        boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                        StringBuilder stringBuilder = new StringBuilder();

                        for (Enums.HackType detectedHack : detectedHacks) {
                            stringBuilder.append(detectedHack.getCheck().getName()).append(", ");
                        }
                        String detections = stringBuilder.substring(0, stringBuilder.length() - 2);

                        for (String command : commands) {
                            if (command != null) {
                                found = true;
                                String modifiedCommand = ConfigUtils.replaceWithSyntax(
                                        player,
                                        command.replaceAll("\\{detections}|\\{detection}", detections),
                                        null
                                );
                                commands.set(index, modifiedCommand);

                                if (enabledDeveloperAPI) {
                                    PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, detectedHacks, modifiedCommand);
                                    Register.manager.callEvent(event);

                                    if (event.isCancelled()) {
                                        continue;
                                    }
                                }

                                performed = true;
                                SpartanBukkit.runDelayedTask(player, () -> {
                                    if (player != null) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                    }
                                }, 1);
                            }
                            index++;
                        }
                    }

                    if (performed) {
                        player.getProfile().punishmentHistory.increasePunishments(player, StringUtils.toString(commands, "\n"));
                    } else if (found && AwarenessNotifications.canSend(AwarenessNotifications.uuid, hackType + "-cancelled-punishment-event")) {
                        String notification = "Just a reminder that the punishments of the '" + hackType + "' check were just cancelled via code by a third-party plugin."
                                + " Please do not reach support for this as it relates only to your server.";
                        List<Plugin> dependentPlugins = PluginUtils.getDependentPlugins(Register.plugin.getName());

                        if (!dependentPlugins.isEmpty()) {
                            StringBuilder dependentPluginNames = new StringBuilder();

                            for (Plugin plugin : dependentPlugins) {
                                dependentPluginNames.append(dependentPluginNames.length() == 0 ? "" : ", ").append(plugin.getName());
                            }
                            notification += " Here are possible plugins that could be doing this:\n" + dependentPluginNames;
                        }
                        AwarenessNotifications.forcefullySend(notification);
                    }
                }
            } else if (AwarenessNotifications.canSend(AwarenessNotifications.uuid, hackType + "-no-punishment-commands")) {
                AwarenessNotifications.forcefullySend("Just a reminder that you have set no punishment commands for the '" + hackType + "' check.");
            }
        }
    }

    private void performNotification(PlayerViolation playerViolation) {
        boolean canPrevent = this.prevention != null,
                individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
        int ignoredViolations = playerViolation.getIgnoredViolations(player);
        String message = ConfigUtils.replaceWithSyntax(
                player,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", playerViolation.information),
                hackType
        );

        if (canPrevent
                || !hackType.getCheck().supportsLiveEvidence) {
            CrossServerInformation.queueNotification(message, true);
        }
        SpartanLocation location = player.movement.getLocation();
        String information = Config.getConstruct() + player.name + " failed " + hackType + " (VL: " + playerViolation
                + ") " + "[(Version: " + MultiVersion.fork() + " " + MultiVersion.versionString()
                + "), (I-V: " + (canPrevent ? "-" : "") + ignoredViolations + ") (Silent: "
                + hackType.getCheck().isSilent(player.getWorld().getName()) + "), "
                + "(Ping: " + player.getPing() + "ms), (TPS: "
                + AlgebraUtils.cut(TPS.get(player, false), 3) + "), " +
                "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " "
                + location.getBlockZ() + "), (" + playerViolation.information + ")]";
        AntiCheatLogs.logInfo(player, information, information, null, hackType, true, playerViolation.level);

        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", player.name);

        if (individualOnlyNotifications) {
            Integer divisor = DetectionNotifications.getDivisor(player, false);

            if (DetectionNotifications.canAcceptMessages(player, divisor, false)
                    && isDivisorValid(playerViolation.level, divisor)) { // Attention
                ClickableMessage.sendCommand(player, message, "Command: " + command, command);
            }
        } else {
            List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

            if (!notificationPlayers.isEmpty()) {
                for (SpartanPlayer staff : notificationPlayers) {
                    if (isDivisorValid(
                            playerViolation.level,
                            NotifyViolation.get(staff, player, ignoredViolations)
                    )) {
                        ClickableMessage.sendCommand(
                                staff,
                                message,
                                "Command: " + command,
                                command
                        );
                    }
                }
            }
        }
    }

    private boolean isDivisorValid(int level, int divisor) {
        return divisor == 1 || level % divisor == 0;
    }
}
