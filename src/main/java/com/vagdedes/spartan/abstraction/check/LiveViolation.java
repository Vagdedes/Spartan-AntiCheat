package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.performance.CancelViolation;
import com.vagdedes.spartan.functionality.performance.NotifyViolation;
import com.vagdedes.spartan.functionality.protections.CheckDelay;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.server.TestServer;
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

    private static final Map<LiveViolation, String> maxLevel
            = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>()));
    public static final String falsePositiveDisclaimer = "§4(False Positive)§f ";

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!maxLevel.isEmpty()) {
                    synchronized (maxLevel) {
                        Iterator<Map.Entry<LiveViolation, String>> iterator = maxLevel.entrySet().iterator();

                        while (iterator.hasNext()) {
                            Map.Entry<LiveViolation, String> entry = iterator.next();
                            LiveViolation violations = entry.getKey();

                            if (violations.getLevel() < Check.maxViolationsPerCycle) {
                                new HackPrevention(violations.player, violations.hackType, entry.getValue(),
                                        null, 0, false, 0.0, 1);
                            } else {
                                iterator.remove();
                            }
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    private final SpartanPlayer player;
    private final Enums.HackType hackType;
    private long cycleExpiration, lastTick;
    private final Map<Integer, Long> cancelledLevel;
    private int level;
    private CancelCause disableCause, silentCause;
    private final Map<Integer, HackPrevention> queue;

    public LiveViolation(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = Collections.synchronizedMap(new LinkedHashMap<>());
        this.lastTick = 0L;
        this.queue = Collections.synchronizedMap(new LinkedHashMap<>());
        reset(); // Do not make it local as it takes part in object initiation and is not important
    }

    // Separator

    void queue(HackPrevention hackPrevention, Check check) {
        if (queue.size() < check.cancelViolation) {
            synchronized (queue) {
                queue.putIfAbsent(hackPrevention.information.hashCode(), hackPrevention);
            }
        }
    }

    public boolean process() {
        HackPrevention hp;
        boolean processed;

        synchronized (queue) {
            Iterator<HackPrevention> iterator = queue.values().iterator();

            if (iterator.hasNext()) {
                hp = iterator.next();
                processed = hp.processed;

                if (hp.expiration < TPS.getTick(player)) {
                    iterator.remove();
                } else {
                    hp.processed = true;
                }
            } else {
                hp = null;
                processed = false;
            }
        }

        if (hp != null
                && !CheckDelay.hasCooldown(player)
                && !CloudBase.isPublicInformationCancelled(hackType, hp.information)
                && (disableCause == null
                || !disableCause.pointerMatches(hp.information))) {
            Check check = hackType.getCheck();
            int violations = this.getLevel();
            boolean canPrevent, falsePositive,
                    suspectedOrHacker = player.getProfile().isSuspectedOrHacker(hackType);
            PlayerViolation playerViolation = new PlayerViolation(
                    hackType,
                    hp.time,
                    hp.information,
                    violations + 1
            );

            if (!player.getProfile().isHacker()
                    && !TestServer.isIdentified()
                    && Config.settings.getBoolean("Performance.enable_false_positive_detection")
                    && !this.hasMaxCancelledLevel(playerViolation.similarityIdentity)) {
                canPrevent = false;
                falsePositive = true;
            } else {
                canPrevent = !check.isSilent(player.getWorld().getName())
                        && (silentCause == null
                        || !silentCause.pointerMatches(hp.information))
                        && (suspectedOrHacker
                        || hasMaxCancelledLevel(playerViolation.similarityIdentity)
                        || CancelViolation.isForced(player, hackType)
                        || violations >= CancelViolation.get(hackType, player.dataType));
                falsePositive = false;
                violations += 1;
            }
            this.lastTick = hp.tick;
            this.player.setLastViolation(this);

            if (falsePositive) {
                synchronized (maxLevel) {
                    String cached = maxLevel.get(this);

                    if (cached != null && cached.equals(hp.information)) {
                        maxLevel.remove(this);
                    }
                }
                violations = this.increaseCancelledLevel(playerViolation.similarityIdentity);
                performNotification(hp.information, violations, false, true, false, suspectedOrHacker);
            } else {
                boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");

                if (processed) {
                    performNotification(hp.information, violations, false, false, canPrevent, suspectedOrHacker);
                } else {
                    switch (hp.violation) {
                        case 2:
                            // Algorithm will redirect this to case '1'.
                            synchronized (maxLevel) {
                                maxLevel.put(this, hp.information);
                            }
                            break;
                        case 1:
                            if (enabledDeveloperAPI) {
                                PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(player.getPlayer(), hackType, violations, hp.information, falsePositive);
                                Register.manager.callEvent(playerViolationEvent);

                                if (playerViolationEvent.isCancelled()) {
                                    break;
                                }
                            }
                            player.getProfile().getViolationHistory(hackType).store(playerViolation);
                            this.setLevel(playerViolation.similarityIdentity, violations);
                            performNotification(hp.information, violations, true, false, canPrevent, suspectedOrHacker);
                            this.performPunishments(violations);
                            break;
                        default:
                            if (enabledDeveloperAPI) {
                                PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(player.getPlayer(), hackType, violations, hp.information, falsePositive);
                                Register.manager.callEvent(playerViolationEvent);

                                if (playerViolationEvent.isCancelled()) {
                                    break;
                                }
                            }
                            player.getProfile().getViolationHistory(hackType).store(playerViolation);
                            performNotification(hp.information, violations, false, false, canPrevent, suspectedOrHacker);
                            break;
                    }
                }

                if (canPrevent) {
                    if (enabledDeveloperAPI) {
                        CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getPlayer(), hackType);
                        Register.manager.callEvent(checkCancelEvent);

                        if (!checkCancelEvent.isCancelled()) {
                            hp.handle(player, hackType);
                            return true;
                        }
                    } else {
                        hp.handle(player, hackType);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Separator

    public int getLevel() {
        timeReset();
        return level;
    }

    public boolean hasLevel() {
        return getLevel() > 0;
    }

    private boolean hasMaxCancelledLevel(int hash) {
        Long time;

        synchronized (cancelledLevel) {
            time = cancelledLevel.get(hash);
        }
        if (time != null) {
            time -= System.currentTimeMillis();
            return time > 0L
                    && (time / 1000.0) >= (hackType.getCheck().getMaxCancelledViolations(player.dataType, hash) - 1); // -1 so we don't ceil the number
        } else {
            return false;
        }
    }

    private void setLevel(int hash, int amount) {
        timeReset();
        int previousLevel = level;

        // Violations
        if (amount < Check.maxViolationsPerCycle) {
            this.level = amount;
            increaseCancelledLevel(hash); // Increase it in case a player has reached the max level and is still cheating
        }
        timeReset(); // Always after changing the level

        // Always last
        if (previousLevel != level) {
            InteractiveInventory.playerInfo.refresh(player.name);
        }
    }

    private int increaseCancelledLevel(int hash) {
        double multiplier = 1.0 + player.getProfile().evidence.getKnowledgeList().size();

        synchronized (cancelledLevel) {
            Long time = cancelledLevel.get(hash);

            if (time == null) {
                cancelledLevel.put(hash, System.currentTimeMillis() + AlgebraUtils.integerRound(1_000 * multiplier));
                return 1;
            } else {
                long current = System.currentTimeMillis();

                if (time < current) {
                    this.cancelledLevel.put(hash, current + AlgebraUtils.integerRound(1_000 * multiplier));
                    return 1;
                } else {
                    time += AlgebraUtils.integerRound(1_000 * multiplier);
                    this.cancelledLevel.put(hash, time);
                    return AlgebraUtils.integerCeil(time / 1000.0);
                }
            }
        }
    }

    // Separator

    public void reset() {
        reset(false);
    }

    private void reset(boolean local) {
        long tick = TPS.getTick(player);
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

        if (event == null
                || !event.isCancelled()
                || this.cycleExpiration <= tick) {
            synchronized (maxLevel) {
                maxLevel.remove(this);
            }
            this.level = 0;
        }
        this.cycleExpiration = tick + Check.violationCycleTicks;

        // Always last
        player.getProfile().evidence.remove(hackType, true, false, true);

        if (local) {
            InteractiveInventory.playerInfo.refresh(player.name);
        }
    }

    private void timeReset() {
        if (this.cycleExpiration <= TPS.getTick(player)) {
            reset(true);
        }
    }

    // Separator

    public boolean isDetected(boolean prevention) {
        return lastTick == TPS.getTick(player)
                && (!prevention
                || !hackType.getCheck().isSilent(player.getWorld().getName()));
    }

    public boolean wasDetected(boolean prevention) {
        return lastTick >= (TPS.getTick(player) - 1)
                && (!prevention
                || !hackType.getCheck().isSilent(player.getWorld().getName()));
    }

    public long getLastTick() {
        return lastTick;
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

    private void performPunishments(int violation) {
        Check check = hackType.getCheck();

        if (check.canPunish) {
            boolean legacy = Config.isLegacy();
            Collection<String> commands = legacy
                    ? check.getLegacyCommands(violation)
                    : Config.settings.getPunishmentCommands();

            if (!commands.isEmpty()) {
                Player n = player.getPlayer();

                if (n != null && n.isOnline()) {
                    boolean performed = false, found = false;

                    if (legacy) {
                        boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");

                        for (String command : commands) {
                            if (command != null) {
                                found = true;
                                String modifiedCommand = ConfigUtils.replaceWithSyntax(player, command, hackType);

                                if (enabledDeveloperAPI) {
                                    PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, modifiedCommand);
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
                        }
                    } else {
                        Collection<Enums.HackType> detectedHacks = player.getProfile().evidence.calculate(player, hackType);
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
                            }
                        }
                    }

                    if (performed) {
                        String commandsString = StringUtils.toString(commands, "\n");

                        if (!commandsString.isEmpty()) {
                            player.getProfile().punishmentHistory.increasePunishments(player, commandsString);
                        }
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

    private void performNotification(String info,
                                     int level,
                                     boolean log,
                                     boolean falsePositive,
                                     boolean canPrevent,
                                     boolean suspectedOrHacker) {
        if (level < Check.maxViolationsPerCycle) {
            boolean individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
            int cancelViolation = CancelViolation.get(hackType, player.dataType),
                    playerCount = SpartanBukkit.getPlayerCount();
            String message = Config.messages.getColorfulString("detection_notification")
                    .replace("{info}", info);

            if (falsePositive) {
                message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(player, message, hackType);
            } else {
                message = ConfigUtils.replaceWithSyntax(player, message, hackType);
            }
            if (suspectedOrHacker
                    || level % cancelViolation == 0
                    || !hackType.getCheck().supportsLiveEvidence) {
                CrossServerInformation.queueNotification(message, true);
            }
            if (log) {
                SpartanLocation location = player.getLocation();
                String cancelViolationString = (canPrevent ? "-" : "") + cancelViolation,
                        information = (falsePositive ? "(False Positive) " : "")
                                + Config.getConstruct() + player.name + " failed " + hackType + " (VL: " + level
                                + ") " + "[(Version: " + MultiVersion.fork() + " " + MultiVersion.versionString()
                                + "), (C-V: " + cancelViolationString + ") (Silent: "
                                + hackType.getCheck().isSilent(player.getWorld().getName()) + "), "
                                + "(Ping: " + player.getPing() + "ms), (TPS: "
                                + AlgebraUtils.cut(TPS.get(player, false), 3) + "), (Hacker: " + suspectedOrHacker
                                + "), (Online: " + playerCount + "), " +
                                "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " "
                                + location.getBlockZ() + "), (" + info + ")]";
                AntiCheatLogs.logInfo(player, information, information, null, hackType, true, level);
            }

            // Local Notifications
            String command = Config.settings.getString("Notifications.message_clickable_command")
                    .replace("{player}", player.name);

            if (individualOnlyNotifications) {
                Integer divisor = DetectionNotifications.getDivisor(player, false);

                if (DetectionNotifications.canAcceptMessages(player, divisor, false)
                        && isDivisorValid(level, divisor)) { // Attention
                    ClickableMessage.sendCommand(player, message, "Command: " + command, command);
                }
            } else {
                List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

                if (!notificationPlayers.isEmpty()) {
                    for (SpartanPlayer staff : notificationPlayers) {
                        int divisor = NotifyViolation.get(staff, player, hackType, playerCount);

                        if (isDivisorValid(level, divisor)) {
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
    }

    private boolean isDivisorValid(int level, int divisor) {
        return divisor == 0 || level % divisor == 0;
    }
}
