package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.server.*;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckDetection extends CheckProcess {

    public static final String
            javaPlayerIdentifier = "Java:",
            checkIdentifier = "Check:",
            detectionIdentifier = "Detection:",
            certaintyIdentifier = "Certainty:";

    // Separator

    private long accumulatedProbabilityTime;
    public final CheckRunner executor;
    public final String name;
    public final boolean hasName;
    private final Boolean def;
    private final Map<Check.DataType, Double> probability;
    public final Check.DataType forcedDataType;
    public final Check.DetectionType detectionType;
    protected long notifications;

    protected CheckDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def
    ) {
        super(executor.hackType, executor.protocol);
        this.executor = executor;
        this.name = name == null
                ? Integer.toString(this.getClass().getName().hashCode())
                : name;
        this.hasName = name != null;
        this.def = def;
        this.probability = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.forcedDataType = forcedDataType;
        this.detectionType = detectionType;
        this.isEnabled();

        if (executor.addDetection(this.name, this) != null) {
            throw new IllegalArgumentException(
                    "Detection '" + this.name + "' already exists for enum '" + executor.hackType.toString() + "'."
            );
        }
    }

    @Override
    public int hashCode() {
        return this.hackType.hashCode() * SpartanBukkit.hashCodeMultiplier + this.name.hashCode();
    }

    // Check

    public final boolean isEnabled() {
        return !this.hasName
                || this.def == null
                || this.hackType.getCheck().getBooleanOption("check_" + this.name, this.def);
    }

    public final boolean supportsDataType(Check.DataType dataType) {
        return this.forcedDataType == null
                || this.forcedDataType == dataType;
    }

    public final boolean supportsDetectionType(Check.DetectionType detectionType) {
        return this.detectionType == null
                || this.detectionType == detectionType;
    }

    public final boolean canCall() {
        return this.protocol != null
                && this.supportsDataType(this.protocol.spartan.dataType)
                && this.supportsDetectionType(this.protocol.spartan.detectionType);
    }

    public final void call(Runnable runnable) {
        if (this.canCall()) {
            runnable.run();
        }
    }

    // Data

    abstract protected boolean hasSufficientData(Check.DataType dataType);

    public void clearData(Check.DataType dataType) {
    }

    public void store(Check.DataType dataType, long time) {
    }

    public void sort() {
    }

    protected boolean hasData(
            PlayerProfile profile,
            Check.DataType dataType
    ) {
        return true;
    }

    public double getData(PlayerProfile profile, Check.DataType dataType) {
        return -1.0;
    }

    long getFirstTime(Check.DataType dataType) {
        return -1L;
    }

    long getLastTime(Check.DataType dataType) {
        return -1L;
    }

    abstract double getDataCompletion(Check.DataType dataType);

    // Probability

    public void clearProbability(Check.DataType dataType) {
        this.probability.remove(dataType);
    }

    public final void setProbability(Check.DataType dataType, double probability) {
        if (probability == PlayerEvidence.emptyProbability) {
            this.probability.remove(dataType);
        } else {
            this.probability.put(dataType, probability);
        }
    }

    public double getProbability(Check.DataType dataType) {
        return isEnabled()
                ? this.probability.getOrDefault(dataType, PlayerEvidence.emptyProbability)
                : PlayerEvidence.emptyProbability;
    }

    // Notification

    private long getAccumulatedProbabilityTime(long time) {
        return Math.max(this.accumulatedProbabilityTime - time, 0L);
    }

    public final boolean canSendNotification(CheckDetection detection, long time, double certainty) {
        int ticks = this.executor.getNotificationTicksCooldown(protocol);
        boolean canSend = PlayerEvidence.surpassedProbability(
                PlayerEvidence.probabilityToCertainty(certainty / 100.0),
                PlayerEvidence.preventionProbability
        ) || DetectionNotifications.isVerboseEnabled(this.protocol);

        if (ticks == 0) {
            return canSend;
        } else if (canSend && this.notifications <= time) {
            if (certainty == -1.0
                    || detection == null) {
                this.notifications = System.currentTimeMillis() + (ticks * TPS.tickTime);
                return true;
            } else {
                int threshold = 1_000;

                if (detection.accumulatedProbabilityTime < time) {
                    detection.accumulatedProbabilityTime = System.currentTimeMillis()
                            + AlgebraUtils.integerRound((certainty / 100.0) * threshold);
                } else {
                    detection.accumulatedProbabilityTime += AlgebraUtils.integerRound(
                            (certainty / 100.0) * threshold
                    );
                }
                long remaining = detection.getAccumulatedProbabilityTime(time);

                if (remaining >= threshold) {
                    Collection<SpartanProtocol> protocols = SpartanBukkit.getProtocols();
                    long sum = 0L;
                    int total = 0;

                    if (!protocols.isEmpty()) {
                        long individual;

                        for (SpartanProtocol protocol : protocols) {
                            if (!protocol.spartan.isAFK()) {
                                CheckDetection protocolDetection = protocol.profile().getRunner(
                                        this.hackType
                                ).getDetection(
                                        this.name
                                );

                                if (protocolDetection != null) {
                                    individual = protocolDetection.getAccumulatedProbabilityTime(time);
                                    sum += individual * individual;
                                    total++;
                                }
                            }
                        }
                    }

                    if (total > 0) {
                        if (remaining >= Math.sqrt(sum / (double) total)) {
                            detection.notifications = System.currentTimeMillis() + (ticks * TPS.tickTime);
                            return true;
                        }
                    } else {
                        this.notifications = System.currentTimeMillis() + (ticks * TPS.tickTime);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void notify(double probability, boolean enabled, boolean sufficientData, long time, String information) {
        double certainty = PlayerEvidence.probabilityToCertainty(probability) * 100.0,
                dataCompletion = sufficientData
                        ? 100.0
                        : this.getDataCompletion(this.protocol.spartan.dataType) * 100.0;
        int roundedCertainty = AlgebraUtils.integerRound(certainty);
        String notification = ConfigUtils.replaceWithSyntax(
                this.protocol,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{detection:percentage}",
                                sufficientData
                                        ? ((roundedCertainty == 0 ? 1 : roundedCertainty) + "/100")
                                        : "DATA " + AlgebraUtils.integerRound(100.0 - dataCompletion) + "% INCOMPLETE"
                        ),
                hackType
        );

        Location location = this.protocol.getLocation();
        information = "(" + AntiCheatLogs.playerIdentifier + " " + this.protocol.bukkit().getName() + "), "
                + "(" + checkIdentifier + " " + this.hackType + "), "
                + "(" + javaPlayerIdentifier + " " + (!this.protocol.spartan.isBedrockPlayer()) + ")" + ", "
                + "(" + detectionIdentifier + " " + this.name + ")" + ", "
                + "(" + certaintyIdentifier + " " + AlgebraUtils.cut(certainty, 2) + "),"
                + "(Data-Completion: " + AlgebraUtils.cut(dataCompletion, 2) + "), "
                + "(Server-Version: " + MultiVersion.serverVersion.toString() + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(this.protocol.spartan.dataType, this.protocol.getWorld().getName()) + "), "
                + "(Punish: " + hackType.getCheck().canPunish(this.protocol.spartan.dataType) + "), "
                + "(Packets: " + this.protocol.packetsEnabled() + "), "
                + "(Ping: " + this.protocol.getPing() + "ms), "
                + "(W-XYZ: " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "(Data: " + information + ")";
        AntiCheatLogs.logInfo(
                this.protocol,
                enabled ? notification : null,
                information,
                enabled
                        && sufficientData
                        && PlayerEvidence.surpassedProbability(
                        probability,
                        PlayerEvidence.preventionProbability
                ),
                null,
                this.hackType,
                time
        );

        if (enabled) {
            // Local Notifications
            String command = Config.settings.getString("Notifications.message_clickable_command")
                    .replace("{player}", this.protocol.bukkit().getName());

            if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
                if (DetectionNotifications.isEnabled(this.protocol)) { // Attention
                    ClickableMessage.sendCommand(this.protocol.bukkit(), notification, command, command);
                }
            } else {
                List<SpartanProtocol> protocols = DetectionNotifications.getPlayers();

                if (!protocols.isEmpty()) {
                    for (SpartanProtocol staff : protocols) {
                        CheckDetection detection = staff.profile().getRunner(this.hackType).getDetection(this.name);

                        if (detection != null
                                && detection.canSendNotification(this, time, certainty)) {
                            ClickableMessage.sendCommand(
                                    staff.bukkit(),
                                    notification,
                                    command,
                                    command
                            );
                        }
                    }
                }
            }
        }
    }

    // Punishment

    private void punish(double probability) {
        Check check = hackType.getCheck();

        if (check.canPunish(this.protocol.spartan.dataType)
                && PlayerEvidence.surpassedProbability(
                probability,
                PlayerEvidence.punishmentProbability
        )) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()) {
                int index = 0;
                boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");

                for (String command : commands) {
                    String modifiedCommand = ConfigUtils.replaceWithSyntax(
                            this.protocol,
                            command.replaceAll("\\{detections}|\\{detection}", check.getName()),
                            null
                    );
                    commands.set(index++, modifiedCommand);

                    if (enabledDeveloperAPI) {
                        Runnable runnable = () -> {
                            PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(
                                    this.protocol.bukkit(),
                                    hackType,
                                    modifiedCommand
                            );
                            Bukkit.getPluginManager().callEvent(event);

                            if (!event.isCancelled()) {
                                SpartanBukkit.runCommand(modifiedCommand);
                            }
                        };

                        if (SpartanBukkit.isSynchronised()) {
                            runnable.run();
                        } else {
                            SpartanBukkit.transferTask(this.protocol, runnable);
                        }
                    } else {
                        SpartanBukkit.runCommand(modifiedCommand);
                    }
                }
                Location location = this.protocol.getLocation();
                CloudConnections.executeDiscordWebhook(
                        "punishments",
                        this.protocol.getUUID(),
                        this.protocol.bukkit().getName(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ(),
                        "Punishment",
                        StringUtils.toString(commands, "\n")
                );
            }
        }
    }

    // Prevention

    public final void cancel(String information, Location location,
                             int cancelTicks, boolean groundTeleport, double damage) {
        if (!this.executor.canFunction()
                || !this.executor.canRun()) {
            return;
        }
        long time = System.currentTimeMillis();
        CheckPrevention newPrevention = new CheckPrevention(
                location,
                cancelTicks,
                groundTeleport,
                damage
        );
        boolean isEnabled =
                hackType.getCheck().isEnabled(this.protocol.spartan.dataType, this.protocol.getWorld().getName())
                        && this.isEnabled()
                        && !Permissions.isBypassing(this.protocol.bukkit(), hackType)
                        && !CloudBase.isInformationCancelled(this.hackType, information);

        if (isEnabled) {
            CheckCancellation disableCause = this.executor.getDisableCause();
            isEnabled = disableCause == null
                    || disableCause.hasExpired()
                    || !disableCause.pointerMatches(information);
        }
        boolean event = isEnabled
                && Config.settings.getBoolean("Important.enable_developer_api");
        CheckCancellation silentCause = this.executor.getSilentCause();
        boolean finalIsEnabled = isEnabled;

        Runnable runnable = () -> {
            if (event) {
                PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                        this.protocol.bukkit(),
                        hackType,
                        information
                );
                Bukkit.getPluginManager().callEvent(playerViolationEvent);

                if (playerViolationEvent.isCancelled()) {
                    return;
                }
            }
            // Store, potentially recalculate and check data
            this.store(
                    this.protocol.spartan.dataType,
                    time
            );
            ResearchEngine.queueToCache(this.hackType, this.protocol.spartan.dataType);
            boolean sufficientData = this.hasSufficientData(this.protocol.spartan.dataType);
            double probability = this.getProbability(this.protocol.spartan.dataType);

            // Notification
            this.notify(probability, finalIsEnabled, sufficientData, time, information);

            // Prevention & Punishment
            if (finalIsEnabled) {
                if (sufficientData) {
                    this.punish(probability);
                }
                this.executor.prevention = newPrevention;
                this.executor.prevention.canPrevent =
                        sufficientData
                                && !hackType.getCheck().isSilent(this.protocol.spartan.dataType, this.protocol.getWorld().getName())
                                && (silentCause == null
                                || silentCause.hasExpired()
                                || !silentCause.pointerMatches(information))
                                && PlayerEvidence.surpassedProbability(probability, PlayerEvidence.preventionProbability);
            }
        };

        if (SpartanBukkit.isSynchronised()) {
            runnable.run();
        } else if (!event) {
            SpartanBukkit.dataThread.executeIfUnknownThreadElseHere(runnable);
        } else {
            SpartanBukkit.transferTask(this.protocol, runnable);
        }
    }

    public final void cancel(String information, Location location,
                             int cancelTicks, boolean groundTeleport) {
        cancel(information, location, cancelTicks, groundTeleport, 0.0);
    }

    public final void cancel(String information, Location location,
                             int cancelTicks) {
        cancel(information, location, cancelTicks, false, 0.0);
    }

    public final void cancel(String information, Location location) {
        cancel(information, location, 0, false, 0.0);
    }

    public final void cancel(String information) {
        cancel(information, null, 0, false, 0.0);
    }

}
