package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.*;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckExecutor extends CheckDetection {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);
    public static final String
            violationLevelIdentifier = "Violations:",
            javaPlayerIdentifier = "Java:",
            detectionIdentifier = "Detection:";

    private final long creation;
    private CancelCause disableCause, silentCause;
    HackPrevention prevention;
    private boolean cancelled;
    final Map<String, DetectionExecutor> detections;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.creation = System.currentTimeMillis();
        this.prevention = new HackPrevention();
        this.detections = new ConcurrentHashMap<>(2);
    }

    // Probability

    public double getExtremeProbability() {
        double num = PlayerEvidence.emptyProbability;

        for (DetectionExecutor detectionExecutor : this.detections.values()) {
            if (PlayerEvidence.POSITIVE) {
                num = Math.max(num, detectionExecutor.getProbability());
            } else {
                num = Math.min(num, detectionExecutor.getProbability());
            }
        }
        return num;
    }

    // Detections

    public final DetectionExecutor getDetection(String detection) {
        return this.detections.get(detection);
    }

    public final DetectionExecutor getDetection() {
        return this.detections.values().iterator().next();
    }

    public final Collection<DetectionExecutor> getDetections() {
        return this.detections.values();
    }

    // Run

    public final void run(boolean cancelled) {
        if (this.player != null) {
            this.cancelled = cancelled;
            this.runInternal(cancelled);
        }
    }

    protected void runInternal(boolean cancelled) {

    }

    // Handle

    public final void handle(boolean cancelled, Object object) {
        if (this.player != null) {
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
        return this.player != null
                && (System.currentTimeMillis() - this.creation) > TPS.maximum * TPS.tickTime
                && (!cancelled || hackType.getCheck().handleCancelledEvents)
                && !player.protocol.isLoading()
                && (!v1_8 || player.getInstance().getGameMode() != GameMode.SPECTATOR)
                && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName())
                && Attributes.getAmount(player, Attributes.GENERIC_SCALE) == 0.0
                && canRun()
                && !Permissions.isBypassing(player, hackType);
    }

    // Causes

    public final CancelCause getDisableCause() {
        if (disableCause == null || disableCause.hasExpired()) {
            if (this.player != null) {
                return MythicMobs.is(player)
                        ? new CancelCause(Compatibility.CompatibilityType.MYTHIC_MOBS)
                        : ItemsAdder.is(player)
                        ? new CancelCause(Compatibility.CompatibilityType.ITEMS_ADDER)
                        : CustomEnchantsPlus.has(player)
                        ? new CancelCause(Compatibility.CompatibilityType.CUSTOM_ENCHANTS_PLUS)
                        : EcoEnchants.has(player)
                        ? new CancelCause(Compatibility.CompatibilityType.ECO_ENCHANTS)
                        : null;
            } else {
                return null;
            }
        } else {
            return disableCause;
        }
    }

    public final CancelCause getSilentCause() {
        return silentCause != null && silentCause.hasExpired() ? null : silentCause;
    }

    public final void addDisableCause(String reason, String pointer, int ticks) {
        if (reason == null) {
            reason = this.hackType.getCheck().getName();
        }
        if (disableCause != null) {
            disableCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            disableCause = new CancelCause(reason, pointer, ticks);
        }
        if (this.player != null) {
            InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
        }
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        if (silentCause != null) {
            silentCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            silentCause = new CancelCause(reason, pointer, ticks);
        }
        if (this.player != null) {
            InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
        }
    }

    public final void removeDisableCause() {
        this.disableCause = null;

        if (this.player != null) {
            InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
        }
    }

    public final void removeSilentCause() {
        this.silentCause = null;

        if (this.player != null) {
            InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
        }
    }

    // Prevention

    public final boolean prevent() {
        if (this.prevention.complete()) {
            if (SpartanBukkit.isSynchronised()) {
                CheckCancelEvent checkCancelEvent;

                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
                    Register.manager.callEvent(checkCancelEvent);
                } else {
                    checkCancelEvent = null;
                }

                if (checkCancelEvent == null
                        || !checkCancelEvent.isCancelled()) {
                    this.prevention.handle(player);
                    return true;
                } else {
                    return false;
                }
            } else {
                Thread thread = Thread.currentThread();
                Boolean[] cancelled = new Boolean[1];

                SpartanBukkit.transferTask(this.player, () -> {
                    CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
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
                    this.prevention.handle(player);
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

    final int getNotificationTicksCooldown(SpartanPlayer detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.player);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.getInstance().equals(this.player.getInstance())
                || detected.getWorld().equals(this.player.getWorld())
                && detected.movement.getLocation().distance(this.player.movement.getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return AlgebraUtils.integerCeil(TPS.maximum);
        }
    }

}
