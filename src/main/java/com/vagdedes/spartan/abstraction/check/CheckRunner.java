package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
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
import com.vagdedes.spartan.functionality.server.*;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckRunner extends CheckProcess {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);

    private final long creation;
    private final Collection<CheckCancellation> disableCauses, silentCauses;
    CheckPrevention prevention;
    private boolean cancelled;
    protected final Map<String, CheckDetection> detections;

    public CheckRunner(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.creation = System.currentTimeMillis();
        this.prevention = new CheckPrevention();
        this.detections = new ConcurrentHashMap<>(2);
        this.disableCauses = Collections.synchronizedList(new ArrayList<>(1));
        this.silentCauses = Collections.synchronizedList(new ArrayList<>(1));
    }

    // Probability

    public final boolean hasSufficientData(Check.DataType dataType, double ratio) {
        if (ratio > 0.0) {
            int count = 0,
                    total = 0;

            for (CheckDetection detectionExecutor : this.detections.values()) {
                if (detectionExecutor instanceof ProbabilityDetection) {
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

            for (CheckDetection detectionExecutor : this.detections.values()) {
                if (detectionExecutor instanceof ProbabilityDetection) {
                    found = true;

                    if (detectionExecutor.hasSufficientData(dataType)) {
                        return true;
                    }
                }
            }
            return !found;
        }
    }

    public final double getExtremeProbability(Check.DataType dataType) {
        double num = PlayerEvidence.emptyProbability;

        for (CheckDetection detectionExecutor : this.detections.values()) {
            if (detectionExecutor.hasSufficientData(dataType)) {
                if (PlayerEvidence.POSITIVE) {
                    num = Math.max(num, detectionExecutor.getProbability(dataType));
                } else {
                    num = Math.min(num, detectionExecutor.getProbability(dataType));
                }
            }
        }
        return num;
    }

    public final long getRemainingCompletionTime(Check.DataType dataType) {
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (playerProfiles.isEmpty()) {
            return 0L;
        } else {
            double averageTime = 0.0,
                    averageCompletion = 0.0;
            int size = 0;

            for (PlayerProfile profile : playerProfiles) {
                for (CheckDetection detection : profile.getRunner(this.hackType).getDetections()) {
                    Long firstTime = detection.getFirstTime(dataType);

                    if (firstTime != null) {
                        Long lastTime = detection.getLastTime(dataType);

                        if (lastTime != null) {
                            double completion = detection.getDataCompletion(dataType);
                            averageCompletion += completion * completion;
                            lastTime -= firstTime;
                            averageTime += lastTime * lastTime;
                            size++;
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
            return time < 1_000L // Less than a second is quite irrelevant
                    ? 0L
                    : time;
        }
    }

    // Detections

    public final CheckDetection getDetection(String detection) {
        return detection == null
                ? this.getDetection()
                : this.detections.get(detection);
    }

    public final CheckDetection getDetection() {
        if (this.detections.isEmpty()) {
            new ImplementedProbabilityDetection(this, null, false);
        }
        return this.detections.values().iterator().next();
    }

    public final Collection<CheckDetection> getDetections() {
        return this.detections.values();
    }

    // Run

    public final void run(boolean cancelled) {
        if (this.protocol() != null) {
            this.cancelled = cancelled;
            this.runInternal(cancelled);
        }
    }

    protected void runInternal(boolean cancelled) {

    }

    // Handle

    public final void handle(boolean cancelled, Object object) {
        if (this.protocol() != null) {
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

    final boolean canFunction() {
        return this.protocol() != null
                && (System.currentTimeMillis() - this.creation) > TPS.maximum * TPS.tickTime
                && (!cancelled || hackType.getCheck().handleCancelledEvents)
                && (!v1_8 || this.protocol().bukkit.getGameMode() != GameMode.SPECTATOR)
                && hackType.getCheck().isEnabled(this.protocol().spartan.dataType, this.protocol().getWorld().getName())
                && Attributes.getAmount(this.protocol(), Attributes.GENERIC_SCALE) == 0.0
                && !ProtocolLib.isTemporary(this.protocol().bukkit)
                && canRun()
                && !Permissions.isBypassing(this.protocol().bukkit, hackType);
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
            if (this.protocol() != null) {
                return MythicMobs.is(this.protocol())
                        ? new CheckCancellation(Compatibility.CompatibilityType.MYTHIC_MOBS)
                        : ItemsAdder.is(this.protocol())
                        ? new CheckCancellation(Compatibility.CompatibilityType.ITEMS_ADDER)
                        : CustomEnchantsPlus.has(this.protocol())
                        ? new CheckCancellation(Compatibility.CompatibilityType.CUSTOM_ENCHANTS_PLUS)
                        : EcoEnchants.has(this.protocol())
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
        if (this.protocol() != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol().bukkit.getName());
        }
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        this.silentCauses.add(new CheckCancellation(reason, pointer, ticks));

        if (this.protocol() != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol().bukkit.getName());
        }
    }

    public final void removeDisableCause() {
        this.disableCauses.clear();

        if (this.protocol() != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol().bukkit.getName());
        }
    }

    public final void removeSilentCause() {
        this.silentCauses.clear();

        if (this.protocol() != null) {
            InteractiveInventory.playerInfo.refresh(this.protocol().bukkit.getName());
        }
    }

    // Prevention

    public final boolean prevent() {
        if (this.prevention.complete()) {
            if (SpartanBukkit.isSynchronised()) {
                CheckCancelEvent checkCancelEvent;

                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    checkCancelEvent = new CheckCancelEvent(this.protocol().bukkit, hackType);
                    Register.manager.callEvent(checkCancelEvent);
                } else {
                    checkCancelEvent = null;
                }

                if (checkCancelEvent == null
                        || !checkCancelEvent.isCancelled()) {
                    this.prevention.handle(this.protocol());
                    return true;
                } else {
                    return false;
                }
            } else {
                Thread thread = Thread.currentThread();
                Boolean[] cancelled = new Boolean[1];

                SpartanBukkit.transferTask(this.protocol(), () -> {
                    CheckCancelEvent checkCancelEvent = new CheckCancelEvent(this.protocol().bukkit, hackType);
                    Register.manager.callEvent(checkCancelEvent);
                    cancelled[0] = checkCancelEvent.isCancelled();

                    synchronized (thread) {
                        thread.notifyAll();
                    }
                });
                synchronized (thread) {
                    if (cancelled[0] == null) {
                        try {
                            thread.wait();
                        } catch (Exception ex) {
                        }
                    }
                }
                if (!cancelled[0]) {
                    this.prevention.handle(this.protocol());
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    // Notification

    final int getNotificationTicksCooldown(SpartanProtocol detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.protocol());

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.bukkit.equals(this.protocol().bukkit)
                || detected.getWorld().equals(this.protocol().getWorld())
                && detected.getLocation().distance(this.protocol().getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return AlgebraUtils.integerCeil(TPS.maximum);
        }
    }

}
