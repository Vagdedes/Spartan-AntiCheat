package ai.idealistic.spartan.abstraction.check;

import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.CheckCancelEvent;
import ai.idealistic.spartan.api.PlayerViolationCommandEvent;
import ai.idealistic.spartan.api.PlayerViolationEvent;
import ai.idealistic.spartan.api.SpartanAPI;
import ai.idealistic.spartan.functionality.connection.CloudConnections;
import ai.idealistic.spartan.functionality.moderation.DetectionNotifications;
import ai.idealistic.spartan.functionality.moderation.clickable.ClickableMessage;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.server.TPS;
import ai.idealistic.spartan.functionality.tracking.AntiCheatLogs;
import ai.idealistic.spartan.functionality.tracking.ResearchEngine;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import ai.idealistic.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.List;

public abstract class CheckDetection extends CheckProcess {

    private static long lastPrevention = 0L;
    private static Method tpsMethod = null;

    private static void setLastPrevention() {
        CheckDetection.lastPrevention = System.currentTimeMillis();
    }

    public static long getLastPrevention() {
        return System.currentTimeMillis() - CheckDetection.lastPrevention;
    }

    static {
        try {
            tpsMethod = Bukkit.class.getMethod("getTPS");
        } catch (Exception ignored) {
        }
    }

    public static final String
            javaPlayerIdentifier = "Java:",
            checkIdentifier = "Check:",
            detectionIdentifier = "Detection:",
            levelIdentifier = "Level:",
            notificationLevelIdentifier = "Notification-Level:",
            preventionLevelIdentifier = "Prevention-Level:",
            punishmentLevelIdentifier = "Punishment-Level:";

    public static final long
            DEFAULT_AVERAGE_TIME = 500L, // Attention, connect to many classes
            TIME_TO_PUNISH = 60_000L,
            TIME_TO_PREVENT = (TIME_TO_PUNISH / 3L) * 2L,
            TIME_TO_NOTIFY = TIME_TO_PUNISH / 2L;

    public static int generalHashCode(CheckEnums.HackType hackType, String name) {
        return (hackType.hashCode() * PluginBase.hashCodeMultiplier)
                + (name == null ? 0 : name.hashCode());
    }

    // Separator

    CheckPrevention prevention;
    public final CheckRunner executor;
    public final String name;
    public final boolean hasName;
    private final Boolean def;
    public final Check.DataType forcedDataType;
    public final Check.DetectionType detectionType;

    public final long defaultAverageTime, timeToNotify, timeToPrevent, timeToPunish;
    private final long[] accumulatedTime, previousTime;
    private long notifications;

    public CheckDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def,
            long defaultAverageTime,
            long timeToNotify,
            long timeToPrevent,
            long timeToPunish
    ) {
        super(executor.hackType, executor.protocol);
        this.executor = executor;
        this.name = name == null
                ? Integer.toString(this.getClass().getName().hashCode())
                : name;
        this.hasName = name != null;
        this.def = def;
        this.forcedDataType = forcedDataType;
        this.detectionType = detectionType;
        this.prevention = new CheckPrevention();
        this.notifications = 0L;
        this.defaultAverageTime = defaultAverageTime;
        this.timeToNotify = timeToNotify;
        this.timeToPrevent = timeToPrevent;
        this.timeToPunish = timeToPunish;
        this.accumulatedTime = new long[Check.DataType.values().length];
        this.previousTime = new long[Check.DataType.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            this.accumulatedTime[dataType.ordinal()] = 0L;
            this.previousTime[dataType.ordinal()] = -1L;
        }
        this.isEnabled();

        if (executor.addDetection(this.name, this) != null) {
            throw new IllegalArgumentException(
                    "Detection '" + this.name + "' already exists for enum '" + executor.hackType.toString() + "'."
            );
        } else {
            this.hackType.addDetection(this.name, defaultAverageTime);
        }
    }

    public CheckDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def
    ) {
        this(
                executor,
                forcedDataType,
                detectionType,
                name,
                def,
                DEFAULT_AVERAGE_TIME,
                TIME_TO_NOTIFY,
                TIME_TO_PREVENT,
                TIME_TO_PUNISH
        );
    }

    public final int generalHashCode() {
        return generalHashCode(this.hackType, this.name);
    }

    // Check

    public final boolean isEnabled() {
        return !this.hasName
                || this.def == null
                || this.hackType.getCheck().getBooleanOption("check_" + this.name, this.def);
    }

    public final boolean canCall() {
        return this.executor.canCall()
                && this.isEnabled()
                && (this.forcedDataType == null
                || this.forcedDataType == this.protocol.getDataType())
                && (this.detectionType == null
                || this.detectionType == this.protocol.detectionType);
    }

    public final void call(Runnable runnable) {
        if (this.canCall()) {
            runnable.run();
        }
    }

    public final void call(Runnable runnable, Runnable ifNot) {
        if (this.canCall()) {
            runnable.run();
        } else {
            ifNot.run();
        }
    }

    // Data

    public final void clearData(Check.DataType dataType) {
        this.protocol.profile().clearTimeDifferences(
                this.hackType,
                dataType,
                this.name
        );
        this.accumulatedTime[this.protocol.getDataType().ordinal()] = 0L;
        this.previousTime[this.protocol.getDataType().ordinal()] = -1L;
    }

    // Accumulation

    private void increaseAccumulationTime(double amplitude, long time) {
        double average = ResearchEngine.getAverageViolationTime(this, this.protocol.getDataType()),
                tpsLost = tpsMethod != null ? (TPS.maximum - Bukkit.getTPS()[0]) : 0.0,
                ratioFromMax = (TPS.maximum - tpsLost * 2) / TPS.maximum;
        long previousTime = this.previousTime[this.protocol.getDataType().ordinal()],
                add;

        if (previousTime == -1L) {
            add = AlgebraUtils.integerRound(average * ratioFromMax * amplitude);
        } else {
            long difference = time - previousTime;
            this.protocol.profile().addTimeDifference(
                    this.hackType,
                    this.protocol.getDataType(),
                    this.name,
                    time
            ); // Add negative purposely for multi-threading
            double multiplier = Math.min(average / (double) difference, 1.0);
            add = AlgebraUtils.integerRound(average * ratioFromMax * amplitude * multiplier);
        }
        this.previousTime[this.protocol.getDataType().ordinal()] = time;

        if (this.accumulatedTime[this.protocol.getDataType().ordinal()] <= System.currentTimeMillis()) {
            this.accumulatedTime[this.protocol.getDataType().ordinal()] = System.currentTimeMillis() + add;
        } else {
            this.accumulatedTime[this.protocol.getDataType().ordinal()] += add;
        }
    }

    private long getAccumulatedTime(Check.DataType dataType) {
        return Math.max(this.accumulatedTime[dataType.ordinal()] - System.currentTimeMillis(), 0L);
    }

    public final int getLevel(Check.DataType dataType) {
        return AlgebraUtils.integerFloor( // Attention
                this.getAccumulatedTime(dataType) / ResearchEngine.getAverageViolationTime(this, dataType)
        );
    }

    public final int getNotificationLevel(Check.DataType dataType) {
        return AlgebraUtils.integerCeil(
                this.timeToNotify / ResearchEngine.getAverageViolationTime(this, dataType)
        );
    }

    public final int getPreventionLevel(Check.DataType dataType) {
        return AlgebraUtils.integerCeil(
                this.timeToPrevent / ResearchEngine.getAverageViolationTime(this, dataType)
        );
    }

    public final int getPunishmentLevel(Check.DataType dataType) {
        return AlgebraUtils.integerCeil(
                this.timeToPunish / ResearchEngine.getAverageViolationTime(this, dataType)
        );
    }

    // Notification

    public final boolean canSendNotification(long time, int level, int notification) {
        int ticks = this.executor.getNotificationTicksCooldown(protocol);
        boolean canSend = level >= notification
                || DetectionNotifications.isVerboseEnabled(this.protocol);

        if (ticks == 0) {
            return canSend;
        } else if (canSend && this.notifications <= time) {
            this.notifications = System.currentTimeMillis() + (ticks * TPS.tickTime);
            return true;
        } else {
            return false;
        }
    }

    private void notify(
            double amplitude,
            int level,
            int notification,
            int prevention,
            int punishment,
            long time,
            String information
    ) {
        String notificationMsg = ConfigUtils.replaceWithSyntax(
                this.protocol,
                Config.messages.getColorfulString("detection_notification")
                        .replace(
                                "{info}",
                                this.hackType.getCheck().isDetectionDetails(this.protocol.getDataType())
                                        || this.name == null
                                        ? information
                                        : this.name.toLowerCase().replace("_", "-")
                        ).replace("{detection:level}", (level >= punishment
                                ? "§4" :
                                level >= prevention ? "§6"
                                        : level >= notification ? "§3"
                                        : "§2") + AlgebraUtils.integerFloor(level / (double) punishment * 100.0) + "/100"),
                hackType
        );

        Location location = this.protocol.getLocation();
        information = "(" + AntiCheatLogs.playerIdentifier + " " + this.protocol.bukkit().getName() + "), "
                + "(" + checkIdentifier + " " + this.hackType + "), "
                + "(" + javaPlayerIdentifier + " " + (!this.protocol.isBedrockPlayer()) + ")" + ", "
                + "(" + detectionIdentifier + " " + this.name + ")" + ", "
                + "(Level-Ratio: " + (level / (double) punishment) + "), "
                + "(Amplitude: " + AlgebraUtils.cut(amplitude, 2) + "),"
                + "(" + levelIdentifier + " " + level + "),"
                + "(" + notificationLevelIdentifier + " " + notification + "),"
                + "(" + preventionLevelIdentifier + " " + prevention + "),"
                + "(" + punishmentLevelIdentifier + " " + punishment + "),"
                + "(Server-Version: " + MultiVersion.serverVersion.toString() + "), "
                + "(Plugin-Version: " + SpartanAPI.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(this.protocol.getDataType(), this.protocol.getWorld().getName()) + "), "
                + "(Punish: " + hackType.getCheck().canPunish(this.protocol.getDataType()) + "), "
                + "(Packets: " + this.protocol.packetsEnabled() + "), "
                + "(Ping: " + this.protocol.getPing() + "ms), "
                + "(W-XYZ: " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "(Data: " + information + ")";
        AntiCheatLogs.logInfo(
                this.protocol,
                notificationMsg,
                information,
                level >= prevention,
                null,
                this.hackType,
                time
        );

        if (level >= notification) {
            CloudConnections.executeDiscordWebhook(
                    "checks",
                    protocol.getUUID(),
                    protocol.bukkit().getName(),
                    protocol.getPing(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    "Detection",
                    notificationMsg
            );
        }
        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", this.protocol.bukkit().getName());

        if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
            if (this.canSendNotification(time, level, notification)) {
                ClickableMessage.sendCommand(this.protocol.bukkit(), notificationMsg, command, command);
            }
        } else {
            List<PlayerProtocol> protocols = DetectionNotifications.getPlayers();

            if (!protocols.isEmpty()) {
                for (PlayerProtocol staff : protocols) {
                    CheckDetection detection = staff.getRunner(this.hackType).getDetection(this.name);

                    if (detection != null
                            && detection.canSendNotification(time, level, notification)) {
                        ClickableMessage.sendCommand(
                                staff.bukkit(),
                                notificationMsg,
                                command,
                                command
                        );
                    }
                }
            }
        }
    }

    // Punishment

    private void punish(int level, int max) {
        Check check = hackType.getCheck();

        if (level >= max
                && check.canPunish(this.protocol.getDataType())) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()) {
                this.accumulatedTime[this.protocol.getDataType().ordinal()] = 0L;
                this.previousTime[this.protocol.getDataType().ordinal()] = -1L;
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
                                PluginBase.runCommand(modifiedCommand);
                            }
                        };

                        if (PluginBase.isSynchronised()) {
                            runnable.run();
                        } else {
                            PluginBase.transferTask(this.protocol, runnable);
                        }
                    } else {
                        PluginBase.runCommand(modifiedCommand);
                    }
                }
            }
        }
    }

    // Prevention

    public final boolean prevent() {
        if (this.prevention.complete()) {
            if (PluginBase.isSynchronised()
                    && Config.settings.getBoolean("Important.enable_developer_api")) {
                CheckCancelEvent checkCancelEvent = new CheckCancelEvent(this.protocol.bukkit(), hackType);
                Bukkit.getPluginManager().callEvent(checkCancelEvent);

                if (checkCancelEvent.isCancelled()) {
                    return false;
                } else {
                    this.prevention.handle(this);
                    return true;
                }
            } else {
                this.prevention.handle(this);
                return true;
            }
        } else {
            return false;
        }
    }

    protected Runnable prevention(Location location, boolean groundTeleport, double damage) {
        return () -> {
            if (location != null
                    || groundTeleport) {
                if (location != null
                        && protocol.teleport(location)) {
                    CheckDetection.setLastPrevention();
                }
                if (groundTeleport
                        && protocol.groundTeleport()) {
                    CheckDetection.setLastPrevention();
                }
            }
            if (damage > 0.0) {
                protocol.bukkit().damage(damage);
            }
        };
    }

    // Cancel

    public final void cancel(
            double amplitude,
            String information,
            Location location,
            int cancelTicks,
            boolean groundTeleport,
            double damage
    ) {
        if (!this.executor.canCancel()) {
            return;
        }
        if (!this.executor.canRun()) {
            this.executor.addInformationalDisableCause(
                    "Check executor will not let the detection/s run."
            );
            return;
        }
        CheckCancellation disableCause = this.executor.getDisableCause(false);

        if (disableCause != null
                && !disableCause.hasExpired()
                && disableCause.pointerMatches(information)) {
            return;
        }
        long time = System.currentTimeMillis();
        CheckPrevention newPrevention = new CheckPrevention(
                location,
                cancelTicks,
                groundTeleport,
                damage
        );
        boolean event = Config.settings.getBoolean("Important.enable_developer_api");
        CheckCancellation silentCause = this.executor.getSilentCause();
        double amplitudeFinal = Math.min(amplitude, 1.0);

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
            this.increaseAccumulationTime(
                    amplitudeFinal,
                    time
            );
            ResearchEngine.queueToCache(this.hackType, this.protocol.getDataType());
            int max = this.getPunishmentLevel(this.protocol.getDataType()),
                    notification = this.getNotificationLevel(this.protocol.getDataType()),
                    prevention = this.getPreventionLevel(this.protocol.getDataType()),
                    level = Math.min(this.getLevel(this.protocol.getDataType()), max);

            // Notification
            this.notify(amplitudeFinal, level, notification, prevention, max, time, information);

            // Prevention & Punishment
            this.prevention = newPrevention;
            this.prevention.canPrevent =
                    level >= prevention
                            && !hackType.getCheck().isSilent(this.protocol.getDataType(), this.protocol.getWorld().getName())
                            && (silentCause == null
                            || silentCause.hasExpired()
                            || !silentCause.pointerMatches(information));

            if (this.prevention.hasDetails()) {
                this.prevent();
            }
            this.punish(level, max);
        };

        if (PluginBase.isSynchronised() || !event) {
            runnable.run();
        } else {
            PluginBase.transferTask(this.protocol, runnable);
        }
    }

    public final void cancel(
            double amplitude,
            String information,
            Location location,
            int cancelTicks,
            boolean groundTeleport
    ) {
        cancel(amplitude, information, location, cancelTicks, groundTeleport, 0.0);
    }

    public final void cancel(
            String information,
            Location location,
            int cancelTicks,
            boolean groundTeleport
    ) {
        cancel(1.0, information, location, cancelTicks, groundTeleport, 0.0);
    }

    public final void cancel(double amplitude,
                             String information,
                             Location location,
                             int cancelTicks
    ) {
        cancel(amplitude, information, location, cancelTicks, false, 0.0);
    }

    public final void cancel(
            String information,
            Location location,
            int cancelTicks
    ) {
        cancel(1.0, information, location, cancelTicks, false, 0.0);
    }

    public final void cancel(double amplitude, String information, Location location) {
        cancel(amplitude, information, location, 0, false, 0.0);
    }

    public final void cancel(String information, Location location) {
        cancel(1.0, information, location, 0, false, 0.0);
    }

    public final void cancel(double amplitude, String information) {
        cancel(amplitude, information, null, 0, false, 0.0);
    }

    public final void cancel(String information) {
        cancel(1.0, information, null, 0, false, 0.0);
    }

}
