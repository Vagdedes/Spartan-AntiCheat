package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.InformationAnalysis;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiveViolation {

    public static final String violationLevelIdentifier = "VL:";

    private final SpartanPlayer player;
    private final Enums.HackType hackType;
    private final Map<Integer, Long> level;
    private CancelCause disableCause, silentCause;
    private HackPrevention prevention;

    public LiveViolation(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.level = new ConcurrentHashMap<>();
        this.prevention = new HackPrevention();
    }

    // Separator

    void run(HackPrevention newPrevention, String information, double violations, long time) {
        synchronized (this) {
            if (PlayerDetectionSlots.isChecked(player.uuid)
                    && !CloudBase.isInformationCancelled(hackType, information)
                    && (disableCause == null
                    || disableCause.hasExpired()
                    || !disableCause.pointerMatches(information))) {
                InformationAnalysis analysis = new InformationAnalysis(hackType, information);
                int violationsInt = Math.max(AlgebraUtils.integerRound(violations), 1);
                PlayerViolation playerViolation = new PlayerViolation(
                        time,
                        hackType,
                        information,
                        Math.max(
                                this.getLevel(analysis.identity) + violationsInt,
                                AlgebraUtils.integerRound(Math.sqrt(this.getTotalLevel() + violationsInt))
                        ),
                        analysis
                );
                boolean event = Config.settings.getBoolean("Important.enable_developer_api");

                Runnable runnable = () -> {
                    if (event) {
                        PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                                player.getInstance(),
                                hackType,
                                playerViolation.level,
                                information
                        );
                        Register.manager.callEvent(playerViolationEvent);

                        if (playerViolationEvent.isCancelled()) {
                            return;
                        }
                    }
                    player.protocol.getProfile().getViolationHistory(hackType).store(playerViolation);
                    this.increaseLevel(playerViolation.identity, violationsInt);
                    this.performNotification(playerViolation);
                    this.performPunishments();
                    this.prevention = newPrevention;
                    this.prevention.canPrevent = !hackType.getCheck().isSilent(player.getWorld().getName())
                            && (silentCause == null
                            || silentCause.hasExpired()
                            || !silentCause.pointerMatches(information))
                            && playerViolation.level >= playerViolation.getIgnoredViolations(player.dataType);
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

    private int getLevel(int hash) {
        Long time = this.level.get(hash);
        return time != null ? this.calculateLevel(time) : 0;
    }

    private int calculateLevel(long time) {
        time -= System.currentTimeMillis();
        return time > 0L
                ? AlgebraUtils.integerCeil(time / (double) hackType.violationTimeWorth)
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
            Iterator<Long> iterator = this.level.values().iterator();

            while (iterator.hasNext()) {
                long level = iterator.next();

                if (this.calculateLevel(level) > 0) {
                    return true;
                } else {
                    iterator.remove();
                }
            }
        }
        return false;
    }

    private void increaseLevel(int hash, int amount) {
        Long time = this.level.get(hash);

        if (time == null) {
            this.level.put(hash, System.currentTimeMillis() + (hackType.violationTimeWorth * amount));
        } else {
            long current = System.currentTimeMillis();

            if (time < current) {
                this.level.put(hash, current + (hackType.violationTimeWorth * amount));
            } else {
                this.level.put(hash, time + (hackType.violationTimeWorth * amount));
            }
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public void reset() {
        this.level.clear();
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

    public double getSuspicionRatio() {
        if (!this.level.isEmpty()) {
            Iterator<Map.Entry<Integer, Long>> iterator = this.level.entrySet().iterator();
            int latency = AlgebraUtils.integerCeil(Latency.getDelay(player));

            while (iterator.hasNext()) {
                Map.Entry<Integer, Long> entry = iterator.next();
                int level = this.calculateLevel(entry.getValue());

                if (level > 0) {
                    double violationCount = level
                            - latency
                            - this.hackType.getCheck().getIgnoredViolations(player.dataType, entry.getKey());

                    if (violationCount > 0.0) {
                        double ratio = violationCount / Check.standardIgnoredViolations;

                        if (ratio >= Check.standardIgnoredViolations) {
                            return AlgebraUtils.cut(ratio, 2);
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
        }
        return -1.0;
    }

    // Separator

    private void performPunishments() {
        Check check = hackType.getCheck();

        if (check.canPunish) {
            List<String> commands = Config.settings.getPunishmentCommands();

            if (!commands.isEmpty()) {
                Collection<Enums.HackType> detectedHacks = player.protocol.getProfile().evidence.getKnowledgeList(false);
                detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish);

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

    private void performNotification(PlayerViolation playerViolation) {
        boolean canPrevent = this.prevention != null,
                individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
        int ignoredViolations = playerViolation.getIgnoredViolations(player.dataType);
        String message = ConfigUtils.replaceWithSyntax(
                player,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", playerViolation.information),
                hackType
        );
        CrossServerInformation.queueNotification(message, true);

        SpartanLocation location = player.movement.getLocation();
        String information = Config.getConstruct() + player.name + " failed " + hackType
                + " (" + violationLevelIdentifier + " " + playerViolation.level + ") "
                + "[(Version: " + MultiVersion.versionString() + "), (IV: " + (canPrevent ? "-" : "") + ignoredViolations + ")"
                + " (Silent: " + hackType.getCheck().isSilent(player.getWorld().getName()) + "),"
                + " (Packets: " + SpartanBukkit.packetsEnabled() + "), (Ping: " + player.getPing() + "ms), " +
                "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " "
                + location.getBlockZ() + "), (" + playerViolation.information + ")]";
        AntiCheatLogs.logInfo(player, information, true, null, playerViolation);

        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", player.name);

        if (individualOnlyNotifications) {
            Integer frequency = DetectionNotifications.getFrequency(player, false);

            if (DetectionNotifications.canAcceptMessages(player, frequency, false)) { // Attention
                Player realPlayer = player.getInstance();

                if (realPlayer != null) {
                    ClickableMessage.sendCommand(realPlayer, message, command, command);
                }
            }
        } else {
            List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

            if (!notificationPlayers.isEmpty()) {
                for (SpartanPlayer staff : notificationPlayers) {
                    if (!hasNotificationCooldown(staff, player, playerViolation)) {
                        Player realPlayer = staff.getInstance();

                        if (realPlayer != null) {
                            ClickableMessage.sendCommand(
                                    realPlayer,
                                    message,
                                    command,
                                    command
                            );
                        }
                    }
                }
            }
        }
    }

    // Separator

    private static int getNotificationTicksCooldown(SpartanPlayer staff,
                                                    SpartanPlayer detectedPlayer,
                                                    int def) {
        Integer frequency = DetectionNotifications.getFrequency(staff, true);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detectedPlayer != null
                && (staff.uuid.equals(detectedPlayer.uuid)
                || staff.getWorld().equals(detectedPlayer.getWorld())
                && AlgebraUtils.getHorizontalDistance(staff.movement.getLocation(), detectedPlayer.movement.getLocation()) <= PlayerUtils.chunk)) {
            return 0;
        } else {
            return -def;
        }
    }

    public static boolean hasNotificationCooldown(SpartanPlayer staff, SpartanPlayer player, PlayerViolation violation) {
        if (staff.cooldowns.canDo("notification")) {
            boolean hasViolations = violation != null;
            int frequency = getNotificationTicksCooldown(
                    staff,
                    player,
                    hasViolations
                            ? violation.getIgnoredViolations(player.dataType)
                            : Check.standardIgnoredViolations
            );
            boolean def = !hasViolations || frequency < 0;

            if (def || violation.level >= frequency) {
                staff.cooldowns.add(
                        "notification",
                        def ? Math.abs(frequency) * TPS.tickTimeInteger : frequency
                );
            }
        }
        return false;
    }

}
