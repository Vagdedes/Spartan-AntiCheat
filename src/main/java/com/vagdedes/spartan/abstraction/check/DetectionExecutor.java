package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
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
import java.util.concurrent.ConcurrentHashMap;

public abstract class DetectionExecutor extends CheckDetection {

    public final String name, random;
    private final boolean def;
    private int lastProbability;
    private long notifications;
    private final Map<Check.DataType, Double> probability;
    private final Map<Check.DataType, Map<Long, Double>> data;

    public DetectionExecutor(CheckExecutor executor, String name, boolean def) {
        super(executor);
        this.name = name;
        this.def = def;
        this.probability = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.isEnabled();
        this.random = this.storeDetection(executor);
    }

    public DetectionExecutor(DetectionExecutor executor, String name, boolean def) {
        super(executor.executor);
        this.name = name;
        this.def = def;
        this.probability = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.isEnabled();
        this.random = this.storeDetection(executor.executor);
    }

    private String storeDetection(CheckExecutor executor) {
        if (this.name == null) {
            while (true) {
                String random = Integer.toString(new Random().nextInt());

                if (executor.detections.putIfAbsent(random, this) == null) {
                    return random;
                }
            }
        } else {
            if (executor.detections.putIfAbsent(this.name, this) != null) {
                throw new IllegalArgumentException(
                        "Detection '" + this.name + "' already exists for enum '" + executor.hackType.toString() + "'."
                );
            }
            return this.name;
        }
    }

    @Override
    public int hashCode() {
        return this.hackType.hashCode() * SpartanBukkit.hashCodeMultiplier + this.random.hashCode();
    }

    // Check

    public final boolean isEnabled() {
        return this.hackType.getCheck().isEnabled(
                this.protocol() == null || this.protocol().spartan == null
                        ? null
                        : this.protocol().spartan.dataType,
                this.protocol() == null || this.protocol().spartan == null
                        ? null
                        : this.protocol().spartan.getWorld().getName()
        )
                && (this.name == null
                || this.hackType.getCheck().getBooleanOption("check_" + this.name, this.def));
    }

    // Data

    public final boolean hasDataToCompare(Check.DataType dataType) {
        Map<Long, Double> map = this.data.get(dataType);
        return map != null && map.size() > 1;
    }

    public final void store(Check.DataType dataType, PlayerViolation playerViolation) {
        synchronized (this.data) {
            Map<Long, Double> map = this.data.computeIfAbsent(
                    dataType,
                    k -> new TreeMap<>()
            );

            if (map.size() == 1_024) {
                Iterator<Long> iterator = map.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
            map.put(playerViolation.time, playerViolation.increase);
        }
        ResearchEngine.queueToCache(this.hackType);
    }

    public final Double getTimeDifference(Check.DataType dataType) {
        if (this.hasDataToCompare(dataType)) {
            double squareSum = 0.0;
            int size;

            synchronized (this.data) {
                Map<Long, Double> map = this.data.get(dataType);
                size = map.size();
                Iterator<Map.Entry<Long, Double>> iterator = map.entrySet().iterator();
                long previous = iterator.next().getKey();

                while (iterator.hasNext()) {
                    Map.Entry<Long, Double> entry = iterator.next();
                    double difference = Math.min(
                            (entry.getKey() - previous) / entry.getValue(),
                            60_000L
                    );
                    previous = entry.getKey();
                    squareSum += difference * difference;
                }
            }
            return Math.sqrt(squareSum / (size - 1.0));
        } else {
            return null;
        }
    }

    // Probability

    final boolean hasSufficientData(Check.DataType dataType) {
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
        int requirement = PlayerEvidence.probabilityToFactors(PlayerEvidence.punishmentProbability);

        if (playerProfiles.size() >= requirement) {
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                if (profile.getExecutor(this.hackType).getDetection(this.name).hasDataToCompare(dataType)) {
                    count++;

                    if (count == requirement) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean surpassedProbability(Check.DataType dataType, double threshold) {
        return PlayerEvidence.surpassedProbability(this.getProbability(dataType), threshold);
    }

    public final void clearProbability(Check.DataType dataType) {
        this.probability.put(dataType, PlayerEvidence.emptyProbability);
    }

    public final void setProbability(Check.DataType dataType, double probability) {
        this.probability.put(dataType, probability);
    }

    final double getProbability(Check.DataType dataType) {
        return isEnabled()
                ? this.probability.getOrDefault(dataType, PlayerEvidence.emptyProbability)
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
                            this.protocol().bukkit,
                            hackType,
                            playerViolation.increase,
                            information
                    );
                    Register.manager.callEvent(playerViolationEvent);

                    if (playerViolationEvent.isCancelled()) {
                        return;
                    }
                }
                this.store(this.protocol().spartan.dataType, playerViolation);
                this.notify(playerViolation, information);
                this.punish();
                this.executor.prevention = newPrevention;
                this.executor.prevention.canPrevent =
                        !hackType.getCheck().isSilent(this.protocol().spartan.dataType, this.protocol().spartan.getWorld().getName())
                                && (silentCause == null
                                || silentCause.hasExpired()
                                || !silentCause.pointerMatches(information))
                                && this.hasSufficientData(this.protocol().spartan.dataType)
                                && this.surpassedProbability(this.protocol().spartan.dataType, PlayerEvidence.preventionProbability);
            };

            if (!event || SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.transferTask(this.protocol().spartan, runnable);
            }
        }
    }

    // Notification

    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time
                && (this.lastProbability != probability
                || time - this.notifications >= 5_000L)) {
            boolean player = detected instanceof SpartanPlayer;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanProtocol) detected : null
            );

            if (ticks > 0) {
                boolean access;

                if (player) {
                    access = ((SpartanPlayer) detected).getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            ((SpartanPlayer) detected).dataType,
                            PlayerEvidence.notificationProbability
                    );
                } else {
                    PlayerProfile profile = ResearchEngine.getPlayerProfile(detected.toString(), true);
                    access = profile.getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            profile.getLastDataType(),
                            PlayerEvidence.notificationProbability
                    );
                }

                if (access) {
                    this.notifications = time + (ticks * TPS.tickTime);
                    this.lastProbability = probability;
                    return true;
                }
            } else {
                this.notifications = time;
                this.lastProbability = probability;
                return true;
            }
        }
        return false;
    }

    private void notify(PlayerViolation playerViolation, String information) {
        double probability = this.getProbability(this.protocol().spartan.dataType),
                certainty = PlayerEvidence.probabilityToCertainty(probability) * 100.0;
        int roundedCertainty = AlgebraUtils.integerRound(certainty);
        boolean hasSufficientData = this.hasSufficientData(this.protocol().spartan.dataType);
        String notification = ConfigUtils.replaceWithSyntax(
                this.protocol(),
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{vls:detection}", String.valueOf(AlgebraUtils.cut(playerViolation.increase, 2)))
                        .replace("{vls:percentage}",
                                hasSufficientData && roundedCertainty > 0
                                        ? (roundedCertainty + "%")
                                        : "unlikely"
                        ),
                hackType
        );

        SpartanLocation location = this.protocol().spartan.movement.getLocation();
        information = this.protocol().bukkit.getName() + " failed " + hackType
                + " (" + CheckExecutor.violationLevelIdentifier + " " + playerViolation.increase + "), "
                + "(Server-Version: " + MultiVersion.serverVersion.toString() + "), "
                + "(Certainty: " + AlgebraUtils.cut(certainty, 2) + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(this.protocol().spartan.dataType, this.protocol().spartan.getWorld().getName()) + "), "
                + "(" + CheckExecutor.javaPlayerIdentifier + " " + (!this.protocol().spartan.isBedrockPlayer()) + ")" + ", "
                + "(" + CheckExecutor.detectionIdentifier + " " + this.name + ")" + ", "
                + "(Packets: " + this.protocol().packetsEnabled() + "), "
                + "(Ping: " + this.protocol().getPing() + "ms), "
                + "(W-XYZ: " + location.world.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "[Information: " + information + "]";
        AntiCheatLogs.logInfo(
                this.protocol(),
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
                .replace("{player}", this.protocol().bukkit.getName());

        if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
            if (DetectionNotifications.isEnabled(this.protocol())) { // Attention
                ClickableMessage.sendCommand(this.protocol().bukkit, notification, command, command);
            }
        } else {
            List<SpartanProtocol> protocols = DetectionNotifications.getPlayers();

            if (!protocols.isEmpty()) {
                for (SpartanProtocol staff : protocols) {
                    if (staff.spartan.getExecutor(this.hackType).getDetection(this.name).canSendNotification(this.protocol(), roundedCertainty)) {
                        ClickableMessage.sendCommand(
                                staff.bukkit,
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

        if (check.canPunish(this.protocol().spartan.dataType)) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()
                    && this.hasSufficientData(this.protocol().spartan.dataType)) {
                Collection<Enums.HackType> detectedHacks = this.protocol().getProfile().getEvidenceList(
                        PlayerEvidence.punishmentProbability
                );
                detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish(this.protocol().spartan.dataType));

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
                                this.protocol(),
                                command.replaceAll("\\{detections}|\\{detection}", detections),
                                null
                        );
                        commands.set(index++, modifiedCommand);

                        if (enabledDeveloperAPI) {
                            Runnable runnable = () -> {
                                PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(
                                        this.protocol().bukkit,
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
                                SpartanBukkit.transferTask(this.protocol().spartan, runnable);
                            }
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                        }
                    }
                    SpartanLocation location = this.protocol().spartan.movement.getLocation();
                    CloudConnections.executeDiscordWebhook(
                            "punishments",
                            this.protocol().getUUID(),
                            this.protocol().bukkit.getName(),
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
