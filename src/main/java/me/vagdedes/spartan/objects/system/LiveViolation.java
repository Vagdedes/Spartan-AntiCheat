package me.vagdedes.spartan.objects.system;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.api.ViolationResetEvent;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import me.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LiveViolation {

    private static final Map<LiveViolation, String> maxLevel =
            MultiVersion.folia ? new LinkedHashMap<>() : new ConcurrentHashMap<>();

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!maxLevel.isEmpty()) {
                    for (Map.Entry<LiveViolation, String> entry : maxLevel.entrySet()) {
                        LiveViolation violations = entry.getKey();
                        SpartanPlayer player = violations.player;

                        if (player != null) {
                            Enums.HackType hackType = violations.getHackType();

                            if (violations.getLevel() < Check.maxViolationsPerCycle) {
                                new HackPrevention(player, hackType, entry.getValue(),
                                        null, 0, false, 0.0, 1);
                            } else {
                                maxLevel.remove(violations);
                            }
                        } else {
                            maxLevel.remove(violations);
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    private static class LoopLevelCache {
        private long expiration;
        private boolean result;

        private LoopLevelCache() {
            this.expiration = 0L;
            this.result = false;
        }

        private boolean isValid() {
            return expiration >= System.currentTimeMillis();
        }

        private boolean setResult(boolean result) {
            this.result = result;
            this.expiration = System.currentTimeMillis() + 1_000L;
            return result;
        }
    }

    private final SpartanPlayer player;
    private final ResearchEngine.DataType dataType;
    private final Enums.HackType hackType;
    private long cycleExpiration, lastViolation;
    private final Map<Integer, Long> cancelledLevel;
    private int level, lastCancelledLevel;
    private final LoopLevelCache cancelledLevelCache, maxCancelledLevelCache;
    private final Collection<HackPrevention> queue;

    public LiveViolation(SpartanPlayer player, ResearchEngine.DataType dataType, Enums.HackType hackType) {
        this.player = player;
        this.dataType = dataType;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = MultiVersion.folia ? new LinkedHashMap<>() : new ConcurrentHashMap<>();
        this.lastViolation = 0L;
        this.lastCancelledLevel = 0;
        this.cancelledLevelCache = new LoopLevelCache();
        this.maxCancelledLevelCache = new LoopLevelCache();
        this.queue = new LinkedList<>();
        reset(); // Do not make it local as it takes part in object initiation and is not important
    }

    // Separator

    void queue(HackPrevention hackPrevention, Check check) {
        if (queue.size() < check.getDefaultCancelViolation()) {
            synchronized (queue) {
                queue.add(hackPrevention);
            }
        }
    }

    public boolean process() {
        HackPrevention hp;
        boolean processed;

        synchronized (queue) {
            Iterator<HackPrevention> iterator = queue.iterator();

            if (iterator.hasNext()) {
                hp = iterator.next();
                processed = hp.processed;

                if (hp.expiration < System.currentTimeMillis()) {
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
                && !LagLeniencies.hasInconsistencies(player, null)
                && !player.getHandlers().has(Handlers.HandlerType.GameMode)
                && !CloudFeature.isInformationCancelled(hackType, hp.information)) {
            UUID uuid = player.getUniqueId();
            Check check = hackType.getCheck();
            CancelCause disableCause = check.getDisabledCause(uuid);
            ResearchEngine.DataType dataType = player.getDataType();

            if (disableCause == null
                    || !disableCause.pointerMatches(hp.information)) {
                int violations = this.getLevel();
                PlayerViolation playerViolation = new PlayerViolation(player.getName(), hackType, hp.time, hp.information, violations + 1, TPS.get(player, false) < TPS.minimum);

                if (!CheckProtection.hasCooldown(uuid, playerViolation)) {
                    Player realPlayer = player.getPlayer();
                    PlayerProfile playerProfile = player.getProfile();
                    boolean canPrevent, canPreventAlternative, falsePositive,
                            suspectedOrHacker = playerProfile.isSuspectedOrHacker(hackType),
                            enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                    PlayerViolationEvent playerViolationEvent;

                    if (suspectedOrHacker) {
                        CancelCause silentCause = check.getSilentCause(uuid);

                        if (silentCause == null || !silentCause.pointerMatches(hp.information)) {
                            canPrevent = true;
                            canPreventAlternative = true;
                        } else {
                            canPrevent = false;
                            canPreventAlternative = false;
                        }
                    } else if (CancelViolation.isForced(player, hackType, this, playerViolation.getSimilarityIdentity())
                            || violations >= CancelViolation.get(hackType, dataType)) {
                        CancelCause silentCause = check.getSilentCause(uuid);

                        if (silentCause == null || !silentCause.pointerMatches(hp.information)) {
                            canPrevent = true;
                            canPreventAlternative = true;
                        } else {
                            canPrevent = false;
                            canPreventAlternative = false;
                        }
                    } else {
                        canPrevent = false;
                        canPreventAlternative = violations > 0  // Testing scenario
                                && !check.hasMaximumDefaultCancelViolation()
                                && player.getLastViolation().getLastViolationTime(true) <= Check.violationCycleSeconds
                                && !SpartanBukkit.isProductionServer()
                                && DetectionNotifications.isEnabled(player);
                    }
                    if (FalsePositiveDetection.canCorrect(playerViolation, this)) {
                        falsePositive = true;
                    } else {
                        falsePositive = false;
                        violations += 1;
                    }

                    this.setLastViolationTime(hp.time); // Always after false-positive check

                    if (!processed) {
                        // API Event
                        if (enabledDeveloperAPI) {
                            playerViolationEvent = new PlayerViolationEvent(realPlayer, hackType, violations, hp.information, falsePositive);
                            Register.manager.callEvent(playerViolationEvent);
                        } else {
                            playerViolationEvent = null;
                        }
                    } else {
                        playerViolationEvent = null;
                    }

                    if (playerViolationEvent == null || !playerViolationEvent.isCancelled()) {
                        if (falsePositive) {
                            this.removeMaxLevel(hp.information);
                            violations = this.increaseCancelledLevel(playerViolation.getSimilarityIdentity());
                            Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), hp.information, violations, false, true, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                        } else {
                            if (processed) {
                                Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), hp.information, violations, false, false, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                            } else {
                                switch (hp.violation) {
                                    case 2:
                                        this.setMaxLevel(hp.information); // Algorithm will redirect this to case '1'.
                                        break;
                                    case 1:
                                        playerProfile.getViolationHistory(hackType).increaseViolations(playerViolation);
                                        this.setLevel(playerViolation.getSimilarityIdentity(), violations);
                                        Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), hp.information, violations, true, false, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                                        Moderation.performPunishments(player, hackType, violations, dataType);
                                        break;
                                    default:
                                        playerProfile.getViolationHistory(hackType).increaseViolations(playerViolation);
                                        Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), hp.information, violations, false, false, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                                        break;
                                }
                            }

                            if (!check.isSilent(player.getWorld().getName(), uuid)
                                    && (canPrevent || canPreventAlternative)) {
                                if (enabledDeveloperAPI) {
                                    CheckCancelEvent checkCancelEvent = new CheckCancelEvent(realPlayer, hackType);
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

    public int getCancelledLevel(int hash) {
        Long time = cancelledLevel.get(hash);

        if (time == null) {
            return 0;
        } else {
            time -= System.currentTimeMillis();
            return time > 0L
                    ? Math.min(AlgebraUtils.integerCeil(time / 1000.0), Check.maxViolationsPerCycle)
                    : 0;
        }
    }

    public int getLastCancelledLevel() {
        return lastCancelledLevel;
    }

    public boolean hasLevel() {
        return getLevel() > 0;
    }

    public boolean hasCancelledLevel() {
        if (cancelledLevelCache.isValid()) {
            return cancelledLevelCache.result;
        } else if (!cancelledLevel.isEmpty()) {
            Iterator<Long> iterator = cancelledLevel.values().iterator();
            long current = System.currentTimeMillis();

            while (iterator.hasNext()) {
                if (iterator.next() > current) {
                    return cancelledLevelCache.setResult(true);
                } else {
                    iterator.remove();
                }
            }
        }
        return cancelledLevelCache.setResult(false);
    }

    public boolean hasMaxCancelledLevel() {
        if (maxCancelledLevelCache.isValid()) {
            return maxCancelledLevelCache.result;
        } else if (!cancelledLevel.isEmpty()) {
            Iterator<Map.Entry<Integer, Long>> iterator = cancelledLevel.entrySet().iterator();
            long current = System.currentTimeMillis();

            while (iterator.hasNext()) {
                Map.Entry<Integer, Long> entry = iterator.next();
                long time = entry.getValue() - current;

                if (time > 0L) {
                    if (time / 1000.0 >= (hackType.getCheck().getMaxCancelledViolations(dataType, entry.getKey()) - 1)) { // Minus one so we don't ceil each loop number
                        return maxCancelledLevelCache.setResult(true);
                    }
                } else {
                    iterator.remove();
                }
            }
        }
        return maxCancelledLevelCache.setResult(false);
    }

    public boolean hasMaxCancelledLevel(int hash) {
        Long time = cancelledLevel.get(hash);

        if (time != null) {
            time -= System.currentTimeMillis();
            return time > 0L
                    && (time / 1000.0) >= (hackType.getCheck().getMaxCancelledViolations(dataType, hash) - 1); // -1 so we don't ceil the number
        } else {
            return false;
        }
    }

    public void setLevel(int hash, int amount) {
        timeReset();
        int previousLevel = level;

        // Violations
        if (amount < Check.maxViolationsPerCycle) {
            this.level = amount;
            increaseCancelledLevel(hash); // Increase it in case a player has reached the max level and is still cheating
        }
        this.lastCancelledLevel = 0;
        timeReset(); // Always after changing the level

        // Always last
        if (previousLevel != level) {
            PlayerInfo.refresh(player.getName());
        }
    }

    public int increaseCancelledLevel(int hash) {
        this.lastCancelledLevel = hash;
        Long time = cancelledLevel.get(hash);

        if (time == null) {
            cancelledLevel.put(hash, System.currentTimeMillis() + 1_000L);
            return 1;
        } else {
            long current = System.currentTimeMillis();

            if (time < current) {
                this.cancelledLevel.put(hash, current + 1_000L);
                return 1;
            } else {
                time += 1_000L;
                this.cancelledLevel.put(hash, time);
                return AlgebraUtils.integerCeil(time / 1000.0);
            }
        }
    }

    public void setMaxLevel(String message) {
        maxLevel.put(this, message);
    }

    public void removeMaxLevel(String message) {
        String cached = maxLevel.get(this);

        if (cached != null && cached.equals(message)) {
            maxLevel.remove(this);
        }
    }

    // Separator

    public void reset() {
        reset(false);
    }

    private void reset(boolean local) {
        ViolationResetEvent event;

        if (SpartanBukkit.isSynchronised()) {
            Player player = this.player.getPlayer();

            if (player != null
                    && player.isOnline()) {
                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    event = new ViolationResetEvent(player, this.getHackType());
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
            maxLevel.remove(this);
            this.level = 0;
            this.lastCancelledLevel = 0;
        } else if ((System.currentTimeMillis() - this.cycleExpiration) > Check.violationCycleSeconds) { // Forceful reset to keep potential developer API usage within hardcoded thresholds
            maxLevel.remove(this);
            this.level = 0;
            this.lastCancelledLevel = 0;
        }
        this.cycleExpiration = System.currentTimeMillis() + Check.violationCycleSeconds;

        // Always last
        if (player != null) {
            player.getProfile().resetLiveEvidence(hackType);

            if (local) {
                PlayerInfo.refresh(player.getName());
            }
        }
    }

    private void timeReset() {
        long time = System.currentTimeMillis();

        if (this.cycleExpiration < time) {
            reset(true);
        }
    }

    // Separator

    public long getLastViolationTime(boolean maxIfNull) {
        return maxIfNull && this.lastViolation == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - this.lastViolation;
    }

    public void setLastViolationTime(long time) {
        this.lastViolation = time;
        this.player.setLastViolation(this);
    }

    // Separator

    public Enums.HackType getHackType() {
        return hackType;
    }
}
