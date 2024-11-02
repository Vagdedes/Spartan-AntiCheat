package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;

import java.util.*;

public abstract class DetectionExecutor extends CheckDetection {

    public final String name;
    private final boolean def;
    private long notifications;
    private double probability;
    private final Map<Long, PlayerViolation> data;

    public DetectionExecutor(CheckExecutor executor, String name, boolean def) {
        super(executor);
        this.name = name;
        this.def = def;
        this.probability = PlayerEvidence.emptyProbability;
        this.data = new TreeMap<>();
        this.isEnabled();
        this.storeDetection(executor, this.name);
    }

    public DetectionExecutor(DetectionExecutor executor, String name, boolean def) {
        super(executor.executor);
        this.name = name;
        this.def = def;
        this.probability = PlayerEvidence.emptyProbability;
        this.data = new TreeMap<>();
        this.isEnabled();
        this.storeDetection(executor.executor, this.name);
    }

    private void storeDetection(CheckExecutor executor, String name) {
        if (this.name == null) {
            while (true) {
                if (executor.detections.putIfAbsent(Integer.toString(new Random().nextInt()), this) == null) {
                    break;
                }
            }
        } else if (executor.detections.putIfAbsent(this.name, this) != null) {
            throw new IllegalArgumentException(
                    "Detection '" + this.name + "' already exists for enum '" + executor.hackType.toString() + "'."
            );
        }
    }

    @Override
    public int hashCode() {
        return this.hackType.hashCode() * SpartanBukkit.hashCodeMultiplier
                + (this.name == null ? 0 : this.name.hashCode());
    }

    // Check

    public final boolean isEnabled() {
        return this.hackType.getCheck().isEnabled(
                this.player == null ? null : this.player.dataType,
                this.player == null ? null : this.player.getWorld().getName()
        )
                && (this.name == null
                || this.hackType.getCheck().getBooleanOption("check_" + this.name, this.def));
    }

    // Data

    public final boolean hasDataToCompare() {
        return data.size() > 1;
    }

    public final void store(PlayerViolation playerViolation) {
        synchronized (this.data) {
            if (this.data.size() == 1_000) {
                Iterator<Long> iterator = this.data.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
            this.data.put(playerViolation.time, playerViolation);
        }
        ResearchEngine.queueToCache(this.hackType);
    }

    public final Double getTimeDifference() {
        if (this.hasDataToCompare()) {
            double squareSum = 0.0;

            synchronized (this.data) {
                Iterator<PlayerViolation> iterator = this.data.values().iterator();
                long previous = iterator.next().time;

                while (iterator.hasNext()) {
                    PlayerViolation violation = iterator.next();
                    double difference = Math.min(
                            (violation.time - previous) / violation.increase,
                            60_000L
                    );
                    previous = violation.time;
                    squareSum += difference * difference;
                }
            }
            return Math.sqrt(squareSum / (this.data.size() - 1.0));
        } else {
            return null;
        }
    }

    // Probability

    public final boolean surpassedProbability(double threshold) {
        return PlayerEvidence.surpassedProbability(this.getProbability(), threshold);
    }

    public final void clearProbability() {
        this.probability = PlayerEvidence.emptyProbability;
    }

    public final void setProbability(double probability) {
        this.probability = probability;
    }

    public final double getProbability() {
        return isEnabled()
                ? this.probability
                : PlayerEvidence.emptyProbability;
    }

    // Violation

    final void violate(HackPrevention newPrevention, String information, double increase, long time) {
        CancelCause disableCause = this.executor.getDisableCause();

        if (disableCause == null
                || disableCause.hasExpired()
                || !disableCause.pointerMatches(information)) {
            if (increase < 1.0) {
                increase = 1.0;
            }
            PlayerViolation playerViolation = new PlayerViolation(
                    time,
                    increase
            );
            boolean event = Config.settings.getBoolean("Important.enable_developer_api");
            CancelCause silentCause = this.executor.getSilentCause();

            Runnable runnable = () -> {
                if (event) {
                    PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                            player.getInstance(),
                            hackType,
                            playerViolation.increase,
                            information
                    );
                    Register.manager.callEvent(playerViolationEvent);

                    if (playerViolationEvent.isCancelled()) {
                        return;
                    }
                }
                this.store(playerViolation);
                this.notify(playerViolation, information);
                this.punish();
                this.executor.prevention = newPrevention;
                this.executor.prevention.canPrevent =
                        !hackType.getCheck().isSilent(player.dataType, player.getWorld().getName())
                                && (silentCause == null
                                || silentCause.hasExpired()
                                || !silentCause.pointerMatches(information))
                                && ResearchEngine.getRequiredPlayers(hackType, name, PlayerEvidence.preventionProbability) == 0
                                && this.surpassedProbability(PlayerEvidence.preventionProbability);
            };

            if (!event || SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.transferTask(player, runnable);
            }
        }
    }

    // Notification

    public final boolean canSendNotification(Object detected) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time) {
            boolean player = detected instanceof SpartanPlayer;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanPlayer) detected : null
            );

            if (ticks > 0) {
                if (player
                        ? ((SpartanPlayer) detected).getExecutor(hackType).getDetection(this.name).surpassedProbability(PlayerEvidence.notificationProbability)
                        : ResearchEngine.getPlayerProfile(detected.toString(), true).getExecutor(hackType).getDetection(this.name).surpassedProbability(PlayerEvidence.notificationProbability)) {
                    this.notifications = time + (ticks * TPS.tickTime);
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private void notify(PlayerViolation playerViolation, String information) {
        double certainty = PlayerEvidence.probabilityToCertainty(this.getProbability()) * 100.0;
        int roundedCertainty = AlgebraUtils.integerRound(certainty);
        boolean hasSufficientData = ResearchEngine.getRequiredPlayers(hackType, name, PlayerEvidence.notificationProbability) == 0;
        String notification = ConfigUtils.replaceWithSyntax(
                player,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{vls:detection}", String.valueOf(AlgebraUtils.cut(playerViolation.increase, 2)))
                        .replace("{vls:percentage}",
                                hasSufficientData && roundedCertainty > 0 ? (roundedCertainty + "%") : "unlikely"
                        ),
                hackType
        );

        SpartanLocation location = player.movement.getLocation();
        information = player.getInstance().getName() + " failed " + hackType
                + " (" + CheckExecutor.violationLevelIdentifier + " " + playerViolation.increase + "), "
                + "(Server-Version: " + MultiVersion.serverVersion.toString() + "), "
                + "(Certainty: " + AlgebraUtils.cut(certainty, 2) + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(player.dataType, player.getWorld().getName()) + "), "
                + "(" + CheckExecutor.javaPlayerIdentifier + " " + (!player.isBedrockPlayer()) + ")" + ", "
                + "(" + CheckExecutor.detectionIdentifier + " " + this.name + ")" + ", "
                + "(Packets: " + player.protocol.packetsEnabled() + "), "
                + "(Ping: " + player.protocol.getPing() + "ms), "
                + "(W-XYZ: " + location.world.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "[Information: " + information + "]";
        AntiCheatLogs.logInfo(
                player,
                notification,
                information,
                hasSufficientData
                        && PlayerEvidence.surpassedProbability(
                        probability,
                        PlayerEvidence.notificationProbability
                ),
                null,
                hackType,
                playerViolation,
                !hasSufficientData
        );

        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", player.getInstance().getName());

        if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
            if (DetectionNotifications.isEnabled(player.protocol)) { // Attention
                ClickableMessage.sendCommand(player.getInstance(), notification, command, command);
            }
        } else {
            List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers();

            if (!notificationPlayers.isEmpty()) {
                for (SpartanPlayer staff : notificationPlayers) {
                    if (staff.getExecutor(this.hackType).getDetection(this.name).canSendNotification(this.player)) {
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

    // Punishment

    private void punish() {
        Check check = hackType.getCheck();

        if (check.canPunish(player.dataType)) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()
                    && ResearchEngine.getRequiredPlayers(hackType, name, PlayerEvidence.punishmentProbability) == 0) {
                Collection<Enums.HackType> detectedHacks = player.protocol.getProfile().getEvidenceList(
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
                            player.protocol.getUUID(),
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

    // Prevention

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks, boolean groundTeleport, double damage) {
        if (this.isEnabled()) {
            long time = System.currentTimeMillis();

            if (this.executor.canFunction()
                    && !CloudBase.isInformationCancelled(this.hackType, information)) {
                this.violate(
                        new HackPrevention(
                                location,
                                cancelTicks,
                                groundTeleport,
                                damage
                        ),
                        information,
                        violations,
                        time
                );
            }
        }
    }

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks, boolean groundTeleport) {
        cancel(information, violations, location, cancelTicks, groundTeleport, 0.0);
    }

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks) {
        cancel(information, violations, location, cancelTicks, false, 0.0);
    }

    public final void cancel(String information, double violations, SpartanLocation location) {
        cancel(information, violations, location, 0, false, 0.0);
    }

    public final void cancel(String information, double violations) {
        cancel(information, violations, null, 0, false, 0.0);
    }

    public final void cancel(String information) {
        cancel(information, 1.0, null, 0, false, 0.0);
    }

}
