package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckDetection extends CheckProcess {

    public static final String
            javaPlayerIdentifier = "Java:",
            checkIdentifier = "Check:",
            detectionIdentifier = "Detection:";

    public final CheckRunner executor;
    public final String name, random;
    private final boolean def;
    private final Map<Check.DataType, Double> probability;
    protected long notifications;

    protected CheckDetection(CheckRunner executor, String name, boolean def) {
        super(executor.hackType, executor.protocol(), executor.playerName());
        this.executor = executor;
        this.name = name;
        this.def = def;
        this.probability = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.isEnabled();

        if (this.name == null) {
            String random;

            while (true) {
                random = Integer.toString(new Random().nextInt());

                if (executor.detections.putIfAbsent(random, this) == null) {
                    break;
                }
            }
            this.random = random;
        } else {
            if (executor.detections.putIfAbsent(this.name, this) != null) {
                throw new IllegalArgumentException(
                        "Detection '" + this.name + "' already exists for enum '" + executor.hackType.toString() + "'."
                );
            }
            this.random = this.name;
        }
    }

    @Override
    public int hashCode() {
        return this.hackType.hashCode() * SpartanBukkit.hashCodeMultiplier + this.random.hashCode();
    }

    // Check

    public final boolean isEnabled() {
        return this.name == null
                || this.hackType.getCheck().getBooleanOption("check_" + this.name, this.def);
    }

    // Data

    protected boolean hasData(Check.DataType dataType) {
        return true;
    }

    protected boolean hasSufficientData(Check.DataType dataType) {
        return true;
    }

    public void clearData(Check.DataType dataType) {

    }

    public void store(Check.DataType dataType, long time) {
    }

    public Double getData(Check.DataType dataType) {
        return null;
    }

    Long getFirstTime(Check.DataType dataType) {
        return null;
    }

    Long getLastTime(Check.DataType dataType) {
        return null;
    }

    double getDataCompletion(Check.DataType dataType) {
        return 1.0;
    }

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

    final double getProbability(Check.DataType dataType) {
        return isEnabled()
                ? this.probability.getOrDefault(dataType, PlayerEvidence.emptyProbability)
                : PlayerEvidence.emptyProbability;
    }

    // Notification

    public abstract boolean canSendNotification(Object detected, int probability);

    protected void notify(long time, String information) {
        boolean hasSufficientData = this.hasSufficientData(this.protocol().spartan.dataType);
        double probability = this.getProbability(this.protocol().spartan.dataType),
                certainty = PlayerEvidence.probabilityToCertainty(probability) * 100.0,
                dataCompletion = hasSufficientData ? 100.0 : this.getDataCompletion(this.protocol().spartan.dataType) * 100.0;
        int roundedCertainty = AlgebraUtils.integerRound(certainty);
        String notification = ConfigUtils.replaceWithSyntax(
                this.protocol(),
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{detection:percentage}",
                                hasSufficientData
                                        ? ((roundedCertainty == 0 ? 1 : roundedCertainty) + "%")
                                        : "unlikely (data " + AlgebraUtils.integerRound(dataCompletion) + "% complete)"
                        ),
                hackType
        );

        Location location = this.protocol().getLocation();
        information = "(" + AntiCheatLogs.playerIdentifier + " " + this.protocol().bukkit.getName() + "), "
                + "(" + checkIdentifier + " " + this.hackType + "), "
                + "(" + javaPlayerIdentifier + " " + (!this.protocol().spartan.isBedrockPlayer()) + ")" + ", "
                + "(" + detectionIdentifier + " " + this.name + ")" + ", "
                + "(Certainty: " + AlgebraUtils.cut(certainty, 2) + "), "
                + "(Data-Completion: " + AlgebraUtils.cut(dataCompletion, 2) + "), "
                + "(Server-Version: " + MultiVersion.serverVersion.toString() + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(this.protocol().spartan.dataType, this.protocol().spartan.getWorld().getName()) + "), "
                + "(Punish: " + hackType.getCheck().canPunish(this.protocol().spartan.dataType) + "), "
                + "(Packets: " + this.protocol().packetsEnabled() + "), "
                + "(Ping: " + this.protocol().getPing() + "ms), "
                + "(W-XYZ: " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "(Data: " + information + ")";
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
                time
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
                    if (staff.spartan.getRunner(this.hackType).getDetection(this.name).canSendNotification(this.protocol(), roundedCertainty)) {
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

    protected void punish() {
        Check check = hackType.getCheck();
        if (check.canPunish(this.protocol().spartan.dataType)) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()
                    && this.hasSufficientData(this.protocol().spartan.dataType)) {
                Collection<Enums.HackType> detectedHacks = this.profile().getEvidenceList(
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
                                    SpartanBukkit.runCommand(modifiedCommand);
                                }
                            };

                            if (SpartanBukkit.isSynchronised()) {
                                runnable.run();
                            } else {
                                SpartanBukkit.transferTask(this.protocol(), runnable);
                            }
                        } else {
                            SpartanBukkit.runCommand(modifiedCommand);
                        }
                    }
                    Location location = this.protocol().getLocation();
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

    public final void cancel(String information, Location location,
                             int cancelTicks, boolean groundTeleport, double damage) {
        if (!this.isEnabled()) {
            return;
        }
        long time = System.currentTimeMillis();

        if (!this.executor.canFunction()
                || CloudBase.isInformationCancelled(this.hackType, information)) {
            return;
        }
        CheckPrevention newPrevention = new CheckPrevention(
                location,
                cancelTicks,
                groundTeleport,
                damage
        );
        CheckCancellation disableCause = this.executor.getDisableCause();
        if (disableCause == null
                || disableCause.hasExpired()
                || !disableCause.pointerMatches(information)) {
            boolean event = Config.settings.getBoolean("Important.enable_developer_api");
            CheckCancellation silentCause = this.executor.getSilentCause();

            Runnable runnable = () -> {
                if (event) {
                    PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                            this.protocol().bukkit,
                            hackType,
                            information
                    );
                    Register.manager.callEvent(playerViolationEvent);

                    if (playerViolationEvent.isCancelled()) {
                        return;
                    }
                }
                this.store(
                        this.protocol().spartan.dataType,
                        time
                );
                ResearchEngine.queueToCache(this.hackType, this.protocol().spartan.dataType);
                this.notify(time, information);
                this.punish();
                this.executor.prevention = newPrevention;
                this.executor.prevention.canPrevent =
                        !hackType.getCheck().isSilent(this.protocol().spartan.dataType, this.protocol().spartan.getWorld().getName())
                                && (silentCause == null
                                || silentCause.hasExpired()
                                || !silentCause.pointerMatches(information))
                                && this.hasSufficientData(this.protocol().spartan.dataType)
                                && PlayerEvidence.surpassedProbability(this.getProbability(this.protocol().spartan.dataType), PlayerEvidence.preventionProbability);

            };

            if (!event || SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.transferTask(this.protocol(), runnable);
            }
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
