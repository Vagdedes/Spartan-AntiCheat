package me.vagdedes.spartan.objects.system;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.ViolationResetEvent;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LiveViolation {

    private static final ConcurrentHashMap<LiveViolation, String> maxLevel = new ConcurrentHashMap<>();

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!maxLevel.isEmpty()) {
                    for (Map.Entry<LiveViolation, String> entry : maxLevel.entrySet()) {
                        LiveViolation violations = entry.getKey();
                        SpartanPlayer player = violations.getPlayer();

                        if (player != null) {
                            Enums.HackType hackType = violations.getHackType();

                            if (violations.getLevel() < Check.maxViolations) {
                                new HackPrevention(player, hackType, entry.getValue());
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

    private final UUID uuid;
    private final ResearchEngine.DataType dataType;
    private final Enums.HackType hackType;
    private long cycleExpiration, lastViolation;
    private final Map<Integer, Long> cancelledLevel;
    private int level, lastCancelledLevel;
    private final LoopLevelCache cancelledLevelCache, maxCancelledLevelCache;

    public LiveViolation(Enums.HackType hackType, ResearchEngine.DataType dataType) {
        this.uuid = SpartanBukkit.uuid;
        this.dataType = dataType;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = MultiVersion.folia ? new HashMap<>() : new ConcurrentHashMap<>();
        this.lastViolation = 0L;
        this.lastCancelledLevel = 0;
        this.cancelledLevelCache = new LoopLevelCache();
        this.maxCancelledLevelCache = new LoopLevelCache();
    }

    public LiveViolation(UUID uuid, ResearchEngine.DataType dataType, Enums.HackType hackType) {
        this.uuid = uuid;
        this.dataType = dataType;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = MultiVersion.folia ? new HashMap<>() : new ConcurrentHashMap<>();
        this.lastViolation = 0L;
        this.lastCancelledLevel = 0;
        this.cancelledLevelCache = new LoopLevelCache();
        this.maxCancelledLevelCache = new LoopLevelCache();
        reset(); // Do not make it local as it takes part in object initiation and is not important
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
                    ? Math.min(AlgebraUtils.integerCeil(time / 1000.0), Check.maxViolations)
                    : 0;
        }
    }

    public int getLastCancelledLevel() {
        return lastCancelledLevel;
    }

    public boolean hasLevel() {
        return getLevel() > 0;
    }

    public boolean hasMaxLevel() {
        return getLevel() == Check.maxViolations;
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
        if (amount < Check.maxViolations) {
            this.level = amount;
            increaseCancelledLevel(hash); // Increase it in case a player has reached the max level and is still cheating
        }
        this.lastCancelledLevel = 0;
        timeReset(); // Always after changing the level

        // Always last
        if (previousLevel != level) {
            SpartanPlayer player = getPlayer();

            if (player != null) {
                PlayerInfo.refresh(player.getName());
            }
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
            SpartanPlayer spartanPlayer = this.getPlayer();

            if (spartanPlayer != null) {
                Player player = spartanPlayer.getPlayer();

                if (player != null && player.isOnline()) {
                    if (Settings.getBoolean("Important.enable_developer_api")) {
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
        SpartanPlayer player = getPlayer();

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

    public long getLastViolation(boolean maxIfNull) {
        return maxIfNull && this.lastViolation == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - this.lastViolation;
    }

    public void setLastViolation() {
        this.lastViolation = System.currentTimeMillis();
    }

    // Separator

    public Enums.HackType getHackType() {
        return hackType;
    }

    public UUID getUUID() {
        return uuid;
    }

    public SpartanPlayer getPlayer() {
        return SpartanBukkit.getPlayer(this.uuid);
    }
}
