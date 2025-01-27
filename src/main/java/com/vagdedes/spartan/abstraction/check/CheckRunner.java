package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.moderation.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;
import org.bukkit.event.Cancellable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckRunner extends CheckProcess {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);

    final long creation;
    private final Collection<CheckCancellation> disableCauses, silentCauses;
    private boolean cancelled;
    private final Map<String, CheckDetection> detections;
    private final boolean[] supportsDataType, supportsDetectionType;

    public CheckRunner(Enums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.creation = System.currentTimeMillis();
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

        if (this.hackType.getCheck().isEnabled(dataType, null)) {
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
        }
        return num;
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

    // Handle

    public final void handle(Object cancelled, Object object) {
        if (this.protocol != null) {
            boolean result;

            if (cancelled == null) {
                if (object instanceof Cancellable) {
                    result = ((Cancellable) object).isCancelled();
                } else {
                    result = false;
                }
            } else if (cancelled instanceof Boolean) {
                result = (Boolean) cancelled;
            } else {
                if (cancelled instanceof Cancellable) {
                    result = ((Cancellable) cancelled).isCancelled();
                } else {
                    result = false;
                }
            }
            this.cancelled = result;
            this.handleInternal(result, object);
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
                && this.supportsDataType[this.protocol.bukkitExtra.dataType.ordinal()]
                && this.supportsDetectionType[this.protocol.bukkitExtra.detectionType.ordinal()]
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
            PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        this.silentCauses.add(new CheckCancellation(reason, pointer, ticks));

        if (this.protocol != null) {
            PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    public final void removeDisableCause() {
        this.disableCauses.clear();

        if (this.protocol != null) {
            PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    public final void removeSilentCause() {
        this.silentCauses.clear();

        if (this.protocol != null) {
            PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
        }
    }

    // Prevention

    public final boolean prevent() {
        if (!this.detections.isEmpty()) {
            for (CheckDetection detection : this.getDetections()) {
                if (detection.prevent()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Notification

    final int getNotificationTicksCooldown(PlayerProtocol detected) {
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
