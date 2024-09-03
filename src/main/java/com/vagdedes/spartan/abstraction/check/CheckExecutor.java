package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.data.Trackers;
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
import com.vagdedes.spartan.functionality.server.*;
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

public abstract class CheckExecutor extends DetectionExecutor {

    public static final String
            violationLevelIdentifier = "Violations:",
            javaPlayerIdentifier = "Java:";

    protected final DetectionExecutor[] detections;
    private final boolean scheduledPrevention;
    private final long[] function;
    private long level, notifications;
    private CancelCause disableCause, silentCause;
    private HackPrevention prevention;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player, int detections,
                         boolean scheduledPrevention) {
        super(null, hackType, player);
        this.detections = new DetectionExecutor[detections];
        this.scheduledPrevention = scheduledPrevention;
        this.prevention = new HackPrevention();
        this.function = new long[2];
    }

    protected final void addDetections(DetectionExecutor[] detections) {
        for (int i = 0; i < detections.length; i++) {
            this.detections[i] = detections[i];
        }
    }

    // Run

    public final void run(boolean cancelled) {
        if (canFunctionOrJustImplemented() && (!cancelled || hackType.getCheck().handleCancelledEvents)) {
            runInternal(cancelled);
        } else {
            cannotRun(cancelled);
        }
    }

    protected void cannotRun(boolean cancelled) {

    }

    protected void runInternal(boolean cancelled) {

    }

    // Handle

    public final void handle(boolean cancelled, Object object) {
        if (canFunctionOrJustImplemented() && (!cancelled || hackType.getCheck().handleCancelledEvents)) {
            handleInternal(cancelled, object);
        } else {
            cannotHandle(cancelled, object);
        }
    }

    protected void cannotHandle(boolean cancelled, Object object) {

    }

    protected void handleInternal(boolean cancelled, Object object) {

    }

    // Separator

    protected boolean canRun() {
        return true;
    }

    // Separator

    private void loadFunction() {
        long tick = TPS.tick();

        if (function[0] != tick) {
            function[0] = tick;
            function[1] = !player.trackers.has(Trackers.TrackerType.SPECTATOR)
                    && !player.protocol.isLoading()
                    && player.getCancellableCompatibility() == null
                    && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName())
                    && this.getDisableCause() == null
                    && !Permissions.isBypassing(player, hackType)
                    && canRun() ? 1L : 0L;
        }
    }

    private boolean canFunctionOrJustImplemented() {
        if (player.protocol.timePassed() <= TPS.maximum * TPS.tickTime) {
            return true;
        } else {
            loadFunction();
            return function[1] == 1L;
        }
    }

    final boolean canFunction() {
        loadFunction();
        return function[1] == 1L;
    }

    // Level

    public final int getLevel() {
        long level = this.level - System.currentTimeMillis();
        return level > 0L
                ? AlgebraUtils.integerCeil(level / (double) hackType.violationTimeWorth)
                : 0;
    }

    public final boolean hasLevel() {
        return this.level - System.currentTimeMillis() > 0L;
    }

    private void increaseLevel(int amount) {
        long current = System.currentTimeMillis();

        if (this.level < current) {
            this.level = current + (hackType.violationTimeWorth * amount);
        } else {
            this.level += (hackType.violationTimeWorth * amount);
        }
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void resetLevel() {
        this.level = 0L;
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    // Causes

    public final CancelCause getDisableCause() {
        return disableCause != null && disableCause.hasExpired() ? null : disableCause;
    }

    public final CancelCause getSilentCause() {
        return silentCause != null && silentCause.hasExpired() ? null : silentCause;
    }

    public final void addDisableCause(String reason, String pointer, int ticks) {
        if (disableCause != null) {
            disableCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            disableCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        if (silentCause != null) {
            silentCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            silentCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void removeDisableCause() {
        this.disableCause = null;
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void removeSilentCause() {
        this.silentCause = null;
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    // Violation

    final void violate(HackPrevention newPrevention, String information, double increase, long time) {
        if (executor.scheduledPrevention) {
            executor.prevent();
        }
        if (PlayerDetectionSlots.isChecked(player)
                && !CloudBase.isInformationCancelled(hackType, information)
                && (disableCause == null
                || disableCause.hasExpired()
                || !disableCause.pointerMatches(information))) {
            int increaseInt = Math.max(AlgebraUtils.integerRound(increase), 1);
            PlayerViolation playerViolation = new PlayerViolation(
                    time,
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
                player.protocol.getProfile().getViolationHistory(player.dataType, hackType).store(
                        hackType,
                        playerViolation
                );
                this.increaseLevel(increaseInt);
                this.notify(playerViolation, information);
                this.punish();
                this.prevention = newPrevention;
                this.prevention.canPrevent =
                        !hackType.getCheck().isSilent(player.dataType, player.getWorld().getName())
                                && (silentCause == null
                                || silentCause.hasExpired()
                                || !silentCause.pointerMatches(information))
                                && PlayerEvidence.getRequiredPlayers(hackType, PlayerEvidence.preventionProbability) == 0
                                && player.protocol.getProfile().evidence.surpassed(this.hackType, PlayerEvidence.preventionProbability);
            };

            if (!event || SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.transferTask(player, runnable);
            }
        }
    }

    // Prevention

    public final boolean prevent() {
        if (this.prevention.complete()) {
            if (SpartanBukkit.isSynchronised()) {
                if (SpartanBukkit.packetsEnabled()) {
                    return false;
                } else {
                    CheckCancelEvent checkCancelEvent;

                    if (Config.settings.getBoolean("Important.enable_developer_api")) {
                        checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
                        Register.manager.callEvent(checkCancelEvent);
                    } else {
                        checkCancelEvent = null;
                    }

                    if (checkCancelEvent == null
                            || !checkCancelEvent.isCancelled()) {
                        this.prevention.handle(player);
                        return true;
                    } else {
                        return false;
                    }
                }
            } else if (!SpartanBukkit.packetsEnabled()) {
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
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Punishment

    private void punish() {
        Check check = hackType.getCheck();

        if (check.canPunish(player.dataType)) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()
                    && PlayerEvidence.getRequiredPlayers(hackType, PlayerEvidence.punishmentProbability) == 0) {
                Collection<Enums.HackType> detectedHacks = player.protocol.getProfile().evidence.getKnowledgeList(
                        PlayerEvidence.punishmentProbability
                );
                detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish(player.dataType));

                if (!detectedHacks.isEmpty()
                        && detectedHacks.contains(hackType)) {
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
                            player.getInstance().getUniqueId(),
                            player.getInstance().getName(),
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

    // Notification

    private void notify(PlayerViolation playerViolation, String information) {
        String notification = ConfigUtils.replaceWithSyntax(
                player,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{vls:detection}", Integer.toString(playerViolation.sum()))
                        .replace("{vls:percentage}", Integer.toString(AlgebraUtils.integerRound(
                                PlayerEvidence.probabilityToCertainty(
                                        player.protocol.getProfile().evidence.getProbability(hackType)
                                ) * 100.0
                        ))),
                hackType
        );

        SpartanLocation location = player.movement.getLocation();
        information = player.getInstance().getName() + " failed " + hackType
                + " (" + violationLevelIdentifier + " " + playerViolation.level + "+" + playerViolation.increase + "), "
                + "(Server-Version: " + MultiVersion.versionString() + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(player.dataType, player.getWorld().getName()) + "), "
                + "(" + javaPlayerIdentifier + " " + (!player.bedrockPlayer) + ")" + ", "
                + "(Packets: " + SpartanBukkit.packetsEnabled() + "), "
                + "(Ping: " + player.protocol.getPing() + "ms), "
                + "(W-XYZ: " + location.world.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "[Information: " + information + "]";
        AntiCheatLogs.logInfo(player, notification, information, true, null, hackType, playerViolation);

        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", player.getInstance().getName());

        if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
            if (DetectionNotifications.isEnabled(player)) { // Attention
                ClickableMessage.sendCommand(player.getInstance(), notification, command, command);
            }
        } else {
            List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers();

            if (!notificationPlayers.isEmpty()) {
                for (SpartanPlayer staff : notificationPlayers) {
                    if (staff.getExecutor(this.hackType).canSendNotification(this.player)) {
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

    private int getNotificationTicksCooldown(SpartanPlayer detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.player);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.getInstance().equals(this.player.getInstance())
                || detected.getWorld().equals(this.player.getWorld())
                && detected.movement.getLocation().distance(this.player.movement.getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return (int) TPS.maximum;
        }
    }

    public final boolean canSendNotification(Object detected) {
        long tick = TPS.tick();

        if (this.notifications <= tick) {
            boolean player = detected instanceof SpartanPlayer;
            int ticks = this.getNotificationTicksCooldown(
                    player ? (SpartanPlayer) detected : null
            );

            if (ticks > 0) {
                if (player
                        ? ((SpartanPlayer) detected).protocol.getProfile().evidence.surpassed(this.hackType, PlayerEvidence.notificationProbability)
                        : ResearchEngine.getPlayerProfile(detected.toString(), true).evidence.surpassed(this.hackType, PlayerEvidence.notificationProbability)) {
                    this.notifications = tick + ticks;
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

}
