package me.vagdedes.spartan.objects.system.hackPrevention;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.notifications.DetectionNotifications;
import me.vagdedes.spartan.features.performance.FalsePositiveDetection;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.features.protections.Teleport;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.CancelCause;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HackPrevention {

    private Enums.HackType hackType;
    private String information;
    private SpartanLocation location;
    private int violation, teleportCooldown;
    private boolean groundTeleport;
    private double damage;
    private long time;

    private static final Map<UUID, List<HackPrevention>> hm = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final Map<UUID, Map<Enums.HackType, Long>> hc = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));

    public static void remove(SpartanPlayer p) {
        UUID uuid = p.getUniqueId();
        hm.remove(uuid);
        hc.remove(uuid);
    }

    public static void cancel(SpartanPlayer player, Enums.HackType hackType, int ticks) {
        if (player != null && ticks > 0) {
            if (ticks > 1200) {
                ticks = 1200;
            }
            if (hackType != null) {
                UUID uuid = player.getUniqueId();
                Map<Enums.HackType, Long> hm = hc.get(uuid);
                long ms = System.currentTimeMillis() + (ticks * 50L);

                if (hm != null) {
                    Long value = hm.get(hackType);

                    if (value == null || ms > value) {
                        hm.put(hackType, ms);
                    }
                } else {
                    hm = new ConcurrentHashMap<>(Enums.HackType.values().length);
                    hm.put(hackType, ms);
                    hc.put(uuid, hm);
                }
            }
        }
    }

    // Separator

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int teleportCooldown,
                          boolean groundTeleport, double damage, int violation, boolean cache) {
        if (violation != -1) { // Empty Object
            Check check = hackType.getCheck();
            Enums.CheckType checkType = check.getCheckType();

            if (checkType != Enums.CheckType.COMBAT) { // Moderate movement violations/preventions
                List<HackPrevention> preventions = hm.get(player.getUniqueId());

                if (preventions != null && preventions.size() > 0) {
                    for (HackPrevention prevention : preventions) {
                        Enums.HackType preventionHackType = prevention.getHackType();

                        if (preventionHackType == hackType // same check
                                || preventionHackType.getCheck().getCheckType() == checkType) { // same category
                            return;
                        }
                    }
                }
            }
            if (player.getBuffer().start("hack-prevention=object", 1) >= Check.sufficientViolations) {
                return;
            }

            // Object Data
            this.time = System.currentTimeMillis();
            this.hackType = hackType;
            this.information = verbose;
            this.location = location;
            this.teleportCooldown = teleportCooldown;
            this.groundTeleport = groundTeleport;
            this.damage = damage;

            if (violation == 1 && checkType == Enums.CheckType.MOVEMENT) {
                if (VehicleAccess.hasExitCooldown(player, hackType)) {
                    this.violation = 0;
                } else {
                    CancelCause silentCause = hackType.getCheck().getSilentCause(player.getUniqueId());

                    if (silentCause != null && silentCause.getReason().equals(Teleport.reason)) {
                        this.violation = 0;
                    } else {
                        this.violation = violation;
                    }
                }
            } else {
                this.violation = violation;
            }

            // Object Cache (Always Last)
            if (cache) {
                UUID uuid = player.getUniqueId();
                List<HackPrevention> hs = hm.get(uuid);

                if (hs != null) {
                    hs.add(this);
                } else {
                    hs = new CopyOnWriteArrayList<>();
                    hs.add(this);
                    hm.put(uuid, hs);
                }
            }
        } else {
            this.hackType = hackType;
        }
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int teleportCooldown, boolean groundTeleport, double damage, boolean violation) {
        this(player, hackType, verbose, location, teleportCooldown, groundTeleport, damage, violation ? 1 : 0, true);
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int teleportCooldown, boolean groundTeleport, double damage) {
        this(player, hackType, verbose, location, teleportCooldown, groundTeleport, damage, 1, true);
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int teleportCooldown, boolean groundTeleport) {
        this(player, hackType, verbose, location, teleportCooldown, groundTeleport, 0.0, 1, true);
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int teleportCooldown) {
        this(player, hackType, verbose, location, teleportCooldown, false, 0.0, 1, true);
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location) {
        this(player, hackType, verbose, location, 0, false, 0.0, 1, true);
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose) {
        this(player, hackType, verbose, null, 0, false, 0.0, 1, true);
    }

    // Special

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, int actionCooldown) {
        this(player, hackType, verbose, null, 0, false, 0.0, 1, true);
        cancel(player, hackType, actionCooldown);
    }

    public HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose, boolean violateAll) {
        this(player, hackType, verbose, null, 0, false, 0.0, violateAll ? 2 : 1, true);
    }

    // Empty

    private HackPrevention(Enums.HackType hackType) { // Empty object
        this(null, hackType, null, null, 0, false, 0.0, -1, false);
    }

    // Separator

    public Enums.HackType getHackType() {
        return hackType;
    }

    public String getInformation() {
        return information;
    }

    public SpartanLocation getLocation() {
        return location;
    }

    public int getTeleportCooldown() {
        return teleportCooldown;
    }

    public boolean hasGroundTeleport() {
        return groundTeleport;
    }

    public double getDamage() {
        return damage;
    }

    // Separator

    public static HackPrevention specifyCancel(SpartanPlayer player, Collection<Enums.HackType> hackTypes) {
        return specifyCancel(player, hackTypes.toArray(new Enums.HackType[0]));
    }

    public static HackPrevention specifyCancel(SpartanPlayer player, Enums.HackType[] hackTypes) {
        // Search
        UUID uuid = player.getUniqueId();
        List<HackPrevention> preventions = hm.get(uuid);
        HackPrevention hp = null;

        if (preventions != null && preventions.size() > 0) {
            for (HackPrevention prevention : preventions) {
                Enums.HackType preventionHackType = prevention.hackType;

                for (Enums.HackType hackType : hackTypes) {
                    if (hackType == preventionHackType) {
                        hp = prevention;
                        preventions.remove(prevention);
                        break;
                    }
                }
            }
        }

        if (hp == null) {
            Enums.HackType hackType = getCooldown(uuid, hackTypes);

            if (hackType != null) {
                return new HackPrevention(hackType);
            }
        } else if (!LagLeniencies.hasInconsistencies(player, null)
                && !player.getHandlers().has(Handlers.HandlerType.GameMode)) {
            Enums.HackType hackType = hp.hackType;
            String information = hp.information;

            if (!CloudFeature.isInformationCancelled(hackType, information)) {
                Check check = hackType.getCheck();
                CancelCause disableCause = check.getDisabledCause(uuid);
                ResearchEngine.DataType dataType = player.getDataType();

                if (disableCause == null || !disableCause.pointerMatches(information)) {
                    LiveViolation violationsObject = check.getViolations(uuid, dataType);
                    int violations = violationsObject.getLevel();
                    PlayerViolation playerViolation = new PlayerViolation(player.getName(), hackType, hp.time, information, violations + 1, TPS.get(player, false) < TPS.minimum);

                    if (!CheckProtection.hasCooldown(uuid, playerViolation)) {
                        Player realPlayer = player.getPlayer();
                        PlayerProfile playerProfile = player.getProfile();
                        boolean canPrevent, canPreventAlternative, falsePositive,
                                suspectedOrHacker = playerProfile.isSuspectedOrHacker(hackType);

                        if (suspectedOrHacker) {
                            CancelCause silentCause = check.getSilentCause(uuid);

                            if (silentCause == null || !silentCause.pointerMatches(information)) {
                                canPrevent = true;
                                canPreventAlternative = true;
                            } else {
                                canPrevent = false;
                                canPreventAlternative = false;
                            }
                        } else {
                            if (CancelViolation.isForced(player, hackType, violationsObject, playerViolation.getSimilarityIdentity())
                                    || bypassCancelViolation(dataType, hackType, violations, hp.damage)) {
                                CancelCause silentCause = check.getSilentCause(uuid);

                                if (silentCause == null || !silentCause.pointerMatches(information)) {
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
                                        && playerProfile.getLastInteraction().getLastViolation(true) <= Check.violationCycleSeconds
                                        && !SpartanBukkit.isProductionServer()
                                        && DetectionNotifications.isEnabled(player);
                            }
                        }
                        if (FalsePositiveDetection.canCorrect(playerViolation, violationsObject)) {
                            falsePositive = true;
                        } else {
                            falsePositive = false;
                            violations += 1;
                        }

                        violationsObject.setLastViolation(); // Always after false-positive check

                        // API Event
                        boolean enabledDeveloperAPI = Settings.getBoolean("Important.enable_developer_api");
                        PlayerViolationEvent playerViolationEvent;

                        if (enabledDeveloperAPI) {
                            playerViolationEvent = new PlayerViolationEvent(realPlayer, hackType, violations, information, falsePositive);
                            Register.manager.callEvent(playerViolationEvent);
                        } else {
                            playerViolationEvent = null;
                        }

                        if (playerViolationEvent == null || !playerViolationEvent.isCancelled()) {
                            if (falsePositive) {
                                violationsObject.removeMaxLevel(information);
                                violations = violationsObject.increaseCancelledLevel(playerViolation.getSimilarityIdentity());
                                Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), information, violations, false, true, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                            } else {
                                switch (hp.violation) {
                                    case 2:
                                        violationsObject.setMaxLevel(information); // Algorithm will redirect this to case '1'.
                                        break;
                                    case 1:
                                        playerProfile.getViolationHistory(hackType).increaseViolations(playerViolation);
                                        violationsObject.setLevel(playerViolation.getSimilarityIdentity(), violations);
                                        playerProfile.setLastInteraction(violationsObject); // Always after interacting with object
                                        Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), information, violations, true, false, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                                        Moderation.performPunishments(player, hackType, violations, dataType);
                                        break;
                                    default:
                                        playerProfile.getViolationHistory(hackType).increaseViolations(playerViolation);
                                        playerProfile.setLastInteraction(violationsObject); // Always after interacting with object
                                        Moderation.detection(uuid, player, playerProfile, hackType, check, playerViolation.getDetection(), information, violations, false, false, canPrevent || canPreventAlternative, suspectedOrHacker, dataType);
                                        break;
                                }

                                if (!check.isSilent(player.getWorld().getName(), uuid)
                                        && (canPrevent || canPreventAlternative)) {
                                    if (enabledDeveloperAPI) {
                                        CheckCancelEvent checkCancelEvent = new CheckCancelEvent(realPlayer, hackType);
                                        Register.manager.callEvent(checkCancelEvent);

                                        if (!checkCancelEvent.isCancelled()) {
                                            return hp;
                                        }
                                    } else {
                                        return hp;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean bypassCancelViolation(ResearchEngine.DataType dataType, Enums.HackType hackType, int violations, double damage) {
        switch (hackType) {
            case NoFall:
                if (damage <= -(MoveUtils.fallDamageBlocks * 3)) { // We want NoFall to enter default if not true
                    return true;
                }
            default:
                return violations >= CancelViolation.get(hackType, dataType);
        }
    }

    public static boolean hasCooldown(UUID uuid) {
        Map<Enums.HackType, Long> cooldowns = hc.get(uuid);

        if (cooldowns != null && cooldowns.size() > 0) {
            long ms = System.currentTimeMillis();

            for (Map.Entry<Enums.HackType, Long> entry : cooldowns.entrySet()) {
                if (entry.getValue() > ms) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Enums.HackType getCooldown(UUID uuid, Enums.HackType[] hackTypes) {
        Map<Enums.HackType, Long> cooldowns = hc.get(uuid);

        if (cooldowns != null && cooldowns.size() > 0) {
            long ms = System.currentTimeMillis();

            for (Map.Entry<Enums.HackType, Long> entry : cooldowns.entrySet()) {
                if (entry.getValue() > ms) {
                    Enums.HackType hackType = entry.getKey();

                    for (Enums.HackType toCompare : hackTypes) {
                        if (toCompare == hackType) {
                            return hackType;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Enums.HackType getCooldown(UUID uuid, Collection<Enums.HackType> hackTypes) {
        return getCooldown(uuid, hackTypes.toArray(new Enums.HackType[0]));
    }

    public static Enums.HackType getCooldown(SpartanPlayer player, Collection<Enums.HackType> hackTypes) {
        return getCooldown(player.getUniqueId(), hackTypes);
    }

    public static Enums.HackType getCooldown(SpartanPlayer player, Enums.HackType[] hackTypes) {
        return getCooldown(player.getUniqueId(), hackTypes);
    }

    public static Enums.HackType getCooldown(SpartanPlayer player, Enums.HackType hackType) {
        return getCooldown(player.getUniqueId(), new Enums.HackType[]{hackType});
    }

    public static boolean canCancel(SpartanPlayer player, Enums.HackType[] hackTypes) {
        return specifyCancel(player, hackTypes) != null;
    }

    public static boolean canCancel(SpartanPlayer player, Enums.HackType hackType) {
        return specifyCancel(player, new Enums.HackType[]{hackType}) != null;
    }

    public static boolean canCancel(SpartanPlayer player) {
        UUID uuid = player.getUniqueId();

        if (hc.containsKey(uuid)) {
            return true;
        }
        List<HackPrevention> positions = hm.get(uuid);
        return positions != null && !positions.isEmpty();
    }
}
