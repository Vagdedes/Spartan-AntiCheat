package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckRunner extends CheckProcess {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);

    final long creation;
    private final Collection<CheckCancellation> disableCauses, silentCauses;
    CheckPrevention prevention;
    private boolean cancelled;
    private final Map<String, CheckDetection> detections;
    private final boolean[] supportsDataType, supportsDetectionType;

    public CheckRunner(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.creation = System.currentTimeMillis();
        this.prevention = new CheckPrevention();
        this.detections = new ConcurrentHashMap<>(2);
        this.disableCauses = Collections.synchronizedList(new ArrayList<>(1));
        this.silentCauses = Collections.synchronizedList(new ArrayList<>(1));
        this.supportsDataType = new boolean[Check.DataType.values().length];
        this.supportsDetectionType = new boolean[Check.DetectionType.values().length];
    }

    // Probability

    public final boolean hasSufficientData(
            Check.DataType dataType,
            Check.DetectionType detectionType,
            double ratio
    ) {
        if (this.detections.isEmpty()) {
            return false;
        } else if (ratio > 0.0) {
            int count = 0,
                    total = 0;

            for (CheckDetection detectionExecutor : new TreeMap<>(this.detections).values()) {
                if (detectionExecutor instanceof ProbabilityDetection
                        && detectionExecutor.supportsDataType(dataType)
                        && detectionExecutor.supportsDetectionType(detectionType)) {
                    total++;

                    if (detectionExecutor.hasSufficientData(dataType)) {
                        count++;

                        if (count / (double) total >= ratio) {
                            return true;
                        }
                    }
                }
            }
            return total == 0;
        } else {
            boolean found = false;

            for (CheckDetection detectionExecutor : this.getDetections()) {
                if (detectionExecutor instanceof ProbabilityDetection
                        && detectionExecutor.supportsDataType(dataType)
                        && detectionExecutor.supportsDetectionType(detectionType)) {
                    found = true;

                    if (detectionExecutor.hasSufficientData(dataType)) {
                        return true;
                    }
                }
            }
            return !found;
        }
    }

    public final double getExtremeProbability(Check.DataType dataType, Check.DetectionType detectionType) {
        double num = PlayerEvidence.emptyProbability;

        for (CheckDetection detectionExecutor : this.getDetections()) {
            if (detectionExecutor.supportsDataType(dataType)
                    && detectionExecutor.supportsDetectionType(detectionType)
                    && detectionExecutor.hasSufficientData(dataType)) {
                if (PlayerEvidence.POSITIVE) {
                    num = Math.max(num, detectionExecutor.getProbability(dataType));
                } else {
                    num = Math.min(num, detectionExecutor.getProbability(dataType));
                }
            }
        }
        return num;
    }

    public final long getRemainingCompletionTime(Check.DataType dataType, Check.DetectionType detectionType) {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (playerProfiles.isEmpty()) {
            return 0L;
        } else {
            double averageTime = 0.0,
                    averageCompletion = 0.0;
            int size = 0;

            for (PlayerProfile profile : playerProfiles) {
                for (CheckDetection detection : profile.getRunner(this.hackType).getDetections()) {
                    if (detection.supportsDataType(dataType)
                            && detection.supportsDetectionType(detectionType)) {
                        long firstTime = detection.getFirstTime(dataType);

                        if (firstTime != -1L) {
                            long lastTime = detection.getLastTime(dataType);

                            if (lastTime != -1L) {
                                double completion = detection.getDataCompletion(dataType);
                                averageCompletion += completion * completion;
                                lastTime -= firstTime;
                                averageTime += lastTime * lastTime;
                                size++;
                            }
                        }
                    }
                }
            }

            if (size == 0) {
                return 0L;
            }
            averageTime = Math.sqrt(averageTime / size);
            averageCompletion = Math.sqrt(averageCompletion / size);
            long time = (long) ((1.0 - averageCompletion) * averageTime);
            // Less than a second is quite irrelevant
            return Math.max(time, 1_000L);
        }
    }

    // Detections

    public final CheckDetection getDetection(String detection) {
        return this.detections.get(detection);
    }

    public final Collection<CheckDetection> getDetections() {
        return this.detections.values();
    }

    protected final CheckDetection addDetection(String name, CheckDetection detection) {
        CheckDetection result = this.detections.putIfAbsent(name, detection);

        for (Check.DataType dataType : Check.DataType.values()) {
            if (detection.supportsDataType(dataType)) {
                this.supportsDataType[dataType.ordinal()] = true;
            }
        }
        for (Check.DetectionType detectionType : Check.DetectionType.values()) {
            if (detection.supportsDetectionType(detectionType)) {
                this.supportsDetectionType[detectionType.ordinal()] = true;
            }
        }
        return result;
    }

    public final void removeDetection(CheckDetection detection) {
        this.detections.remove(detection.name);

        for (Check.DataType dataType : Check.DataType.values()) {
            this.supportsDataType[dataType.ordinal()] = false;
        }
        for (Check.DetectionType detectionType : Check.DetectionType.values()) {
            this.supportsDetectionType[detectionType.ordinal()] = false;
        }
        for (CheckDetection other : this.getDetections()) {
            for (Check.DataType dataType : Check.DataType.values()) {
                if (other.supportsDataType(dataType)) {
                    this.supportsDataType[dataType.ordinal()] = true;
                }
            }
            for (Check.DetectionType detectionType : Check.DetectionType.values()) {
                if (other.supportsDetectionType(detectionType)) {
                    this.supportsDetectionType[detectionType.ordinal()] = true;
                }
            }
        }
    }

    // Run

    public final void run(boolean cancelled) {
        if (this.protocol != null) {
            this.cancelled = cancelled;
            this.runInternal(cancelled);
        }
    }

    protected void runInternal(boolean cancelled) {

    }

    // Handle

    public final void handle(boolean cancelled, Object object) {
        if (this.protocol != null) {
            this.cancelled = cancelled;
            this.handleInternal(cancelled, object);
        }
    }

    protected void handleInternal(boolean cancelled, Object object) {

    }

    // Separator

    protected boolean canRun() {
        return true;
    }

    // Separator

    final boolean supportsDataType(Check.DataType dataType) {
        return this.supportsDataType[dataType.ordinal()];
    }

    final boolean supportsDetectionType(Check.DetectionType detectionType) {
        return this.supportsDetectionType[detectionType.ordinal()];
    }

    final boolean canFunction() {
        return this.protocol != null
                && !this.protocol.npc
                && this.supportsDataType[this.protocol.spartan.dataType.ordinal()]
                && this.supportsDetectionType[this.protocol.spartan.detectionType.ordinal()]
                && (System.currentTimeMillis() - this.creation) > TPS.maximum * TPS.tickTime
                && (!cancelled || hackType.getCheck().handleCancelledEvents)
                && (!v1_8 || this.protocol.bukkit().getGameMode() != GameMode.SPECTATOR)
                && Attributes.getAmount(this.protocol, Attributes.GENERIC_SCALE) == 0.0
                && !ProtocolLib.isTemporary(this.protocol.bukkit());
    }

    // Causes

    private CheckCancellation getLastCause(Collection<CheckCancellation> collection) {
        CheckCancellation lastCause = null;
        Iterator<CheckCancellation> iterator = collection.iterator();

        while (iterator.hasNext()) {
            CheckCancellation cause = iterator.next();

            if (cause.hasExpired()) {
                iterator.remove();
            } else {
                lastCause = cause;
                break;
            }
        }
        return lastCause;
    }

    public final CheckCancellation getDisableCause() {
        CheckCancellation disableCause = this.getLastCause(this.disableCauses);

        if (disableCause == null) {
            if (this.protocol != null) {
                return MythicMobs.is(this.protocol)
                        ? new CheckCancellation(Compatibility.CompatibilityType.MYTHIC_MOBS)
                        : ItemsAdder.is(this.protocol)
                        ? new CheckCancellation(Compatibility.CompatibilityType.ITEMS_ADDER)
                        : CustomEnchantsPlus.has(this.protocol)
                        ? new CheckCancellation(Compatibility.CompatibilityType.CUSTOM_ENCHANTS_PLUS)
                        : EcoEnchants.has(this.protocol)
                        ? new CheckCancellation(Compatibility.CompatibilityType.ECO_ENCHANTS)
                        : null;
            } else {
                return null;
            }
        } else {
            return disableCause;
        }
    }

    public final CheckCancellation getSilentCause() {
        return this.getLastCause(this.silentCauses);
    }

    public final void addDisableCause(String reason, String pointer, int ticks) {
        if (reason == null) {
            reason = this.hackType.getCheck().getName();
        }
        this.disableCauses.add(new CheckCancellation(reason, pointer, ticks));

        if (this.protocol != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        this.silentCauses.add(new CheckCancellation(reason, pointer, ticks));

        if (this.protocol != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    public final void removeDisableCause() {
        this.disableCauses.clear();

        if (this.protocol != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    public final void removeSilentCause() {
        this.silentCauses.clear();

        if (this.protocol != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    // Prevention

    public final boolean prevent() {
        if (this.prevention.complete()) {
            if (SpartanBukkit.isSynchronised()) {
                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    CheckCancelEvent checkCancelEvent = new CheckCancelEvent(this.protocol.bukkit(), hackType);
                    Bukkit.getPluginManager().callEvent(checkCancelEvent);

                    if (checkCancelEvent.isCancelled()) {
                        return false;
                    } else {
                        this.prevention.handle(this.protocol);
                        return true;
                    }
                } else {
                    this.prevention.handle(this.protocol);
                    return true;
                }
            } else {
                this.prevention.handle(this.protocol);
                return true;
            }
        } else {
            return false;
        }
    }

    // Notification

    final int getNotificationTicksCooldown(SpartanProtocol detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.protocol);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.equals(this.protocol)
                || detected.getWorld().equals(this.protocol.getWorld())
                && detected.getLocation().distance(this.protocol.getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return AlgebraUtils.integerRound(TPS.maximum);
        }
    }

}
