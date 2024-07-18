package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerEvidence;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LiveViolation {

    public static final String
            violationLevelIdentifier = "Violations:",
            javaPlayerIdentifier = "Java:";

    private final SpartanPlayer player;
    private final Enums.HackType hackType;
    private long level;
    private CancelCause disableCause, silentCause;
    private HackPrevention prevention;
    private final Cooldowns notifications;

    public LiveViolation(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.level = 0L;
        this.prevention = new HackPrevention();
        this.notifications = new Cooldowns(new ConcurrentHashMap<>());
    }

    // Separator

    void run(HackPrevention newPrevention, String information, double increase, long time) {
        synchronized (this) {
            if (PlayerDetectionSlots.isChecked(player.uuid)
                    && !CloudBase.isInformationCancelled(hackType, information)
                    && (disableCause == null
                    || disableCause.hasExpired()
                    || !disableCause.pointerMatches(information))) {
                int increaseInt = Math.max(AlgebraUtils.integerRound(increase), 1);
                PlayerViolation playerViolation = new PlayerViolation(
                        time,
                        hackType,
                        this.getLevel(),
                        increaseInt
                );
                boolean event = Config.settings.getBoolean("Important.enable_developer_api");

                Runnable runnable = () -> {
                    if (event) {
                        PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                                player.getInstance(),
                                hackType,
                                playerViolation.sum(),
                                information
                        );
                        Register.manager.callEvent(playerViolationEvent);

                        if (playerViolationEvent.isCancelled()) {
                            return;
                        }
                    }
                    player.protocol.getProfile().getViolationHistory(player.dataType, hackType).store(playerViolation);
                    this.increaseLevel(increaseInt);
                    this.performNotification(playerViolation, information);
                    this.performPunishments();
                    this.prevention = newPrevention;
                    this.prevention.canPrevent =
                            !hackType.getCheck().isSilent(player.dataType, player.getWorld().getName())
                                    && (silentCause == null
                                    || silentCause.hasExpired()
                                    || !silentCause.pointerMatches(information))
                                    && player.protocol.getProfile().evidence.get(this.hackType) >= PlayerEvidence.prevention;
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
                    && !this.prevention.hasExpired()) {
                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    if (SpartanBukkit.isSynchronised()) {
                        CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
                        Register.manager.callEvent(checkCancelEvent);

                        if (!checkCancelEvent.isCancelled()) {
                            this.prevention.handle(player);
                            return true;
                        } else {
                            return false;
                        }
                    } else if (SpartanBukkit.packetsEnabled()) {
                        this.prevention.handle(player);
                        return true;
                    } else {
                        Thread thread = Thread.currentThread();
                        Boolean[] cancelled = new Boolean[1];

                        SpartanBukkit.transferTask(this.player, () -> {
                            CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
                            Register.manager.callEvent(checkCancelEvent);
                            cancelled[0] = checkCancelEvent.isCancelled();

                            synchronized (thread) {
                                thread.notifyAll();
                            }
                        });
                        synchronized (thread) {
                            if (cancelled[0] == null) {
                                try {
                                    thread.wait();
                                } catch (Exception ex) {
                                }
                            }
                        }
                        if (!cancelled[0]) {
                            this.prevention.handle(player);
                            return true;
                        } else {
                            return false;
                        }
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

    public int getLevel() {
        long level = this.level;
        level -= System.currentTimeMillis();
        return level > 0L
                ? AlgebraUtils.integerCeil(level / (double) hackType.violationTimeWorth)
                : 0;
    }

    public boolean hasLevel() {
        return this.level - System.currentTimeMillis() > 0L;
    }

    private void increaseLevel(int amount) {
        long current = System.currentTimeMillis();

        if (this.level < current) {
            this.level = current + (hackType.violationTimeWorth * amount);
        } else {
            this.level += (hackType.violationTimeWorth * amount);
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public void reset() {
        this.level = 0L;
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public CancelCause getDisableCause() {
        return disableCause != null && disableCause.hasExpired() ? null : disableCause;
    }

    public CancelCause getSilentCause() {
        return silentCause != null && silentCause.hasExpired() ? null : silentCause;
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

    private void performPunishments() {
        Check check = hackType.getCheck();

        if (check.canPunish(player.dataType)) {
            List<String> commands = Config.settings.getPunishmentCommands();

            if (!commands.isEmpty()) {
                Collection<Enums.HackType> detectedHacks = player.protocol.getProfile().evidence.getKnowledgeList(PlayerEvidence.punishment);
                detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish(player.dataType));

                if (!detectedHacks.isEmpty()) {
                    int index = 0;
                    boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                    StringBuilder stringBuilder = new StringBuilder();

                    for (Enums.HackType detectedHack : detectedHacks) {
                        stringBuilder.append(detectedHack.getCheck().getName()).append(", ");
                    }
                    String detections = stringBuilder.substring(0, stringBuilder.length() - 2);

                    for (String command : commands) {
                        String modifiedCommand = ConfigUtils.replaceWithSyntax(
                                player,
                                command.replaceAll("\\{detections}|\\{detection}", detections),
                                null
                        );
                        commands.set(index++, modifiedCommand);

                        if (enabledDeveloperAPI) {
                            Runnable runnable = () -> {
                                PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(
                                        player.getInstance(),
                                        hackType,
                                        detectedHacks,
                                        modifiedCommand
                                );
                                Register.manager.callEvent(event);

                                if (!event.isCancelled()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                }
                            };

                            if (SpartanBukkit.isSynchronised()) {
                                runnable.run();
                            } else {
                                SpartanBukkit.transferTask(player, runnable);
                            }
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                        }
                    }
                    SpartanLocation location = player.movement.getLocation();
                    CloudConnections.executeDiscordWebhook(
                            "punishments",
                            player.uuid,
                            player.name,
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ(),
                            "Punishment",
                            StringUtils.toString(commands, "\n")
                    );
                }
            }
        }
    }

    private void performNotification(PlayerViolation playerViolation, String information) {
        String notification = ConfigUtils.replaceWithSyntax(
                player,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{vls:detection}", Integer.toString(playerViolation.sum())),
                hackType
        );

        SpartanLocation location = player.movement.getLocation();
        information = player.name + " failed " + hackType
                + " (" + violationLevelIdentifier + " " + playerViolation.level + "+" + playerViolation.increase + "), "
                + "(Server-Version: " + MultiVersion.versionString() + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(player.dataType, player.getWorld().getName()) + "), "
                + "(" + javaPlayerIdentifier + " " + (!player.bedrockPlayer) + ")" + ", "
                + "(Packets: " + SpartanBukkit.packetsEnabled() + "), "
                + "(Ping: " + player.getPing() + "ms), "
                + "(W-XYZ: " + location.world.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "[Information: " + information + "]";
        AntiCheatLogs.logInfo(player, notification, information, true, null, playerViolation);

        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", player.name);

        if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
            if (DetectionNotifications.isEnabled(player)) { // Attention
                ClickableMessage.sendCommand(player.getInstance(), notification, command, command);
            }
        } else {
            List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers();

            if (!notificationPlayers.isEmpty()) {
                for (SpartanPlayer staff : notificationPlayers) {
                    if (staff.getViolations(this.hackType).canSendNotification(this.player)) {
                        ClickableMessage.sendCommand(
                                staff.getInstance(),
                                notification,
                                command,
                                command
                        );
                    }
                }
            }
        }
    }

    // Separator

    private int getNotificationTicksCooldown(SpartanPlayer detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.player);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.uuid.equals(this.player.uuid)
                || detected.getWorld().equals(this.player.getWorld())
                && detected.movement.getLocation().distance(this.player.movement.getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return (int) TPS.maximum;
        }
    }

    public boolean canSendNotification(Object detected) {
        boolean player = detected instanceof SpartanPlayer;

        if (this.notifications.canDo("")
                && (player
                ? ((SpartanPlayer) detected).protocol.getProfile().evidence.get(this.hackType)
                : ResearchEngine.getPlayerProfile(detected.toString()).evidence.get(this.hackType))
                >= PlayerEvidence.notification) {
            int ticks = this.getNotificationTicksCooldown(
                    player ? (SpartanPlayer) detected : null
            );

            if (ticks > 0) {
                this.notifications.add("", ticks);
            }
            return true;
        }
        return false;
    }

}
