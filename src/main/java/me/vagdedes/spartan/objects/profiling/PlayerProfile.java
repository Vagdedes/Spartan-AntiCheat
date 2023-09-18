package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.checks.world.XRay;
import me.vagdedes.spartan.compatibility.necessary.bedrock.BedrockCompatibility;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.gui.spartan.SpartanMenu;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerProfile {

    private UUID uuid;
    private final String name;
    private final ViolationHistory[] violationHistory;
    private final MiningHistory[] miningHistory;
    private final PunishmentHistory punishmentHistory;
    private final PlayerEvidence evidence;
    private boolean
            bedrockPlayer,
            bedrockPlayerCheck,
            staff,
            tester;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;
    private final PlayerCombat playerCombat;
    private long lastPlayed;

    // Separator

    public PlayerProfile(String name) {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        boolean isNull = name == null;

        // Separator
        this.uuid = isNull ? SpartanBukkit.uuid : null;
        this.name = name;
        this.punishmentHistory = new PunishmentHistory();
        this.playerCombat = new PlayerCombat(isNull ? "" : name);
        this.evidence = new PlayerEvidence();
        this.skull = null;
        this.offlinePlayer = null;

        // Separator
        if (isNull) {
            this.staff = false;
            this.bedrockPlayer = false;
            this.bedrockPlayerCheck = true;
            this.lastPlayed = 0L;
        } else {
            SpartanPlayer player = this.getSpartanPlayer();

            if (player != null) {
                this.staff = Permissions.isStaff(player);
                this.bedrockPlayer = player.isBedrockPlayer();
                this.bedrockPlayerCheck = true;
                this.lastPlayed = player.getLastPlayed();
            } else {
                this.staff = false;
                this.bedrockPlayer = BedrockCompatibility.isPlayer(name);
                this.bedrockPlayerCheck = bedrockPlayer;
                this.lastPlayed = 0L;
            }
        }

        // Separator
        this.violationHistory = new ViolationHistory[Enums.hackTypeLength];
        this.miningHistory = new MiningHistory[Enums.miningOreLength];

        for (Enums.HackType hackType : hackTypes) {
            this.violationHistory[hackType.ordinal()] = new ViolationHistory(hackType, getDataType());
        }
        for (Enums.MiningOre ore : Enums.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore, 0, 1);
        }
    }

    public PlayerProfile(SpartanPlayer player) {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.offlinePlayer = player.getPlayer(); // Attention
        this.punishmentHistory = new PunishmentHistory();
        this.playerCombat = new PlayerCombat(name);
        this.evidence = new PlayerEvidence();
        this.skull = null;
        this.offlinePlayer = null;
        this.bedrockPlayer = player.isBedrockPlayer(); // Attention
        this.staff = Permissions.isStaff(player);
        this.bedrockPlayerCheck = true;
        this.lastPlayed = player.getLastPlayed();

        this.violationHistory = new ViolationHistory[Enums.hackTypeLength];
        this.miningHistory = new MiningHistory[Enums.miningOreLength];

        for (Enums.HackType hackType : hackTypes) {
            this.violationHistory[hackType.ordinal()] = new ViolationHistory(hackType, getDataType());
        }
        for (Enums.MiningOre ore : Enums.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore, 0, 1);
        }
    }

    public PlayerProfile() {
        this((String) null);
    }

    // Separator

    public UUID getUniqueId() {
        if (uuid == null) {
            SpartanPlayer player = getSpartanPlayer();

            if (player != null) {
                this.uuid = player.getUniqueId();

                if (!bedrockPlayerCheck && BedrockCompatibility.isPlayer(uuid, name)) {
                    this.bedrockPlayerCheck = true;
                    this.bedrockPlayer = true;
                }
            } else {
                OfflinePlayer offlinePlayer = getOfflinePlayer();

                if (offlinePlayer != null) {
                    this.uuid = offlinePlayer.getUniqueId();

                    if (!bedrockPlayerCheck && BedrockCompatibility.isPlayer(uuid, name)) {
                        this.bedrockPlayerCheck = true;
                        this.bedrockPlayer = true;
                    }
                }
            }
        }
        return uuid;
    }

    public String getName() {
        return name;
    }

    public ItemStack getSkull() {
        if (skull == null) {
            OfflinePlayer player = getOfflinePlayer();
            this.skull = player == null ? InventoryUtils.getHead() : InventoryUtils.getSkull(player);
        }
        return skull;
    }

    public ResearchEngine.DataType getDataType() {
        return isBedrockPlayer() ? ResearchEngine.DataType.Bedrock : ResearchEngine.DataType.Java;
    }

    public boolean isBedrockPlayer() {
        if (bedrockPlayer) {
            return true;
        }
        if (!bedrockPlayerCheck) {
            SpartanPlayer player = getSpartanPlayer();

            if (player != null) {
                bedrockPlayerCheck = true;

                if (player.isBedrockPlayer()) {
                    return bedrockPlayer = true;
                }
            }
        }
        return false;
    }

    public boolean wasStaff() {
        return staff;
    }

    public void setStaff(boolean bool) {
        this.staff = bool;
    }

    public boolean wasTesting() {
        if (tester) {
            return true;
        }
        SpartanPlayer player = getSpartanPlayer();
        return player != null && TestServer.isTester(player);
    }

    public void setTester(boolean bool) {
        this.tester = bool;
    }

    public OfflinePlayer getOfflinePlayer() {
        if (offlinePlayer == null && name != null) {
            if (this.uuid == null) {
                this.offlinePlayer = Bukkit.getOfflinePlayer(name);
                this.uuid = offlinePlayer.getUniqueId();
            } else {
                this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            }
            if (!bedrockPlayerCheck && !bedrockPlayer
                    && BedrockCompatibility.isPlayer(uuid, name)) {
                this.bedrockPlayerCheck = true;
                this.bedrockPlayer = true;
            }
        }
        return offlinePlayer;
    }

    public SpartanPlayer getSpartanPlayer() {
        return uuid != null ? SpartanBukkit.getPlayer(uuid) :
                name != null ? SpartanBukkit.getPlayer(name) :
                        null;
    }

    // Separator

    public boolean isOnline() {
        return getSpartanPlayer() != null;
    }

    public boolean wasRecentlyOnline() {
        return isOnline() || (System.currentTimeMillis() - lastPlayed) <= 60_000L;
    }

    public void setOnline(SpartanPlayer player) {
        this.lastPlayed = player.getLastPlayed();
    }

    // Separator

    public ViolationHistory[] getViolationHistory() {
        return violationHistory;
    }

    public ViolationHistory getViolationHistory(Enums.HackType hackType) {
        return violationHistory[hackType.ordinal()];
    }

    // Separator

    public void calculateHistoricalEvidence() {
        if (evidence.startCalculation(shouldCalculateEvidence())) {
            boolean hasStatistics = ViolationStatistics.has();

            for (ViolationHistory violationHistory : violationHistory) {
                Enums.HackType hackType = violationHistory.getHackType();

                if (hackType == Enums.HackType.XRay) {
                    Map<Enums.MiningOre, Integer> suspectedOres = new LinkedHashMap<>(Enums.miningOreLength);

                    for (MiningHistory miningHistory : getMiningHistory()) {
                        for (World.Environment environment : World.Environment.values()) {
                            double mines = miningHistory.getMines(environment);

                            if (mines > 0) {
                                int days = miningHistory.getDays();
                                mines = (mines + punishmentHistory.getReportCount(hackType, 0)) / ((double) days);
                                double globalAverage = Math.abs(ResearchEngine.getMiningHistoryAverage(miningHistory.getOre(), XRay.lenientStatisticalRatio)), // When below zero, it means data is not yet coming from player profiles but backup variables
                                        miningRatio = mines / globalAverage;

                                if (miningRatio > 1.0) { // We inform earlier in the inventory menu while the violation we want to be accurate
                                    suspectedOres.put(miningHistory.getOre(), AlgebraUtils.integerRound(miningRatio * 100.0));
                                }
                            }
                            break;
                        }
                    }

                    if (suspectedOres.size() > 0) {
                        String separator = "§l/§r";
                        evidence.add(hackType,
                                StringUtils.toString(suspectedOres.keySet().toArray(new Enums.MiningOre[0]), separator)
                                        + ": " + StringUtils.toString(suspectedOres.values().toArray(new Integer[0]), separator));
                    } else {
                        evidence.remove(hackType);
                    }
                } else if (hasStatistics) {
                    ViolationStatistics.GlobalWarmup globalStatistics = ViolationStatistics.get(hackType);

                    if (globalStatistics.has) {
                        List<PlayerViolation> data = violationHistory.getViolationsList();

                        if (data.size() > 0) {
                            Map<String, ViolationStatistics> comparedStatistics = new LinkedHashMap<>();

                            // Calculate compared profile data

                            for (PlayerViolation playerViolation : data) {
                                if (playerViolation.isDetectionEnabled()) {
                                    String date = playerViolation.getDate();
                                    ViolationStatistics statistics = comparedStatistics.get(date);

                                    if (statistics == null) {
                                        statistics = new ViolationStatistics();
                                        comparedStatistics.put(date, statistics);
                                    }
                                    statistics.count(playerViolation);
                                }
                            }

                            // Collide Compared and Global Data

                            int violationCount = violationHistory.getImportantViolations(true) + punishmentHistory.getReportCount(hackType, 0);
                            double averageViolations = violationCount / ((double) comparedStatistics.size()),
                                    violationRatio = averageViolations / hackType.getCheck().getDefaultCancelViolation(),
                                    thresholdSurpassing = violationRatio / Check.analysisMultiplier;

                            if (thresholdSurpassing >= 1.0) {
                                int count = 0,
                                        globalCount = globalStatistics.unrecordedProfiles;
                                double timeAverage = 0.0,
                                        globalTimeAverage = 0.0;
                                Collection<ViolationStatistics> comparedData = comparedStatistics.values();

                                for (Map.Entry<String, Double> entry : globalStatistics.loop) { // Dates
                                    ViolationStatistics.IndividualWarmup comparables = ViolationStatistics.getComparables(entry.getKey(), comparedData);

                                    if (comparables.has && !comparables.cached) {
                                        for (double comparedResults : comparables.loop) {
                                            count++;
                                            timeAverage += comparedResults;
                                        }
                                    }
                                    globalCount++;
                                    globalTimeAverage += entry.getValue();
                                }

                                // Decide based on Information

                                if (count > 0 && globalCount > 0) {
                                    timeAverage = (globalTimeAverage / globalCount) / (timeAverage / count); // Greater is less time between violations per day compared to others

                                    if (timeAverage > 1.5) {
                                        evidence.add(hackType,
                                                "suspicion: " + AlgebraUtils.integerRound(thresholdSurpassing * 100.0) + "%"
                                                        + "§l/§r" + AlgebraUtils.integerRound(timeAverage * 100.0) + "%"
                                        );
                                    } else {
                                        evidence.remove(hackType);
                                    }
                                } else {
                                    evidence.remove(hackType);
                                }
                            } else {
                                evidence.remove(hackType);
                            }
                        } else {
                            evidence.remove(hackType);
                        }
                    } else {
                        evidence.remove(hackType);
                    }
                } else {
                    evidence.remove(hackType);
                }
            }
            judgeEvidence();
        }
    }

    public boolean calculateLiveEvidence(SpartanPlayer player, Enums.HackType hackType, ResearchEngine.DataType dataType) {
        if (evidence.has(hackType)) {
            return true;
        }
        if (hackType != Enums.HackType.XRay) {
            int violationCount = player.getViolations(hackType).getLevel() - hackType.getCheck().getCancelViolation();

            if (violationCount > 0) {
                violationCount += punishmentHistory.getReportCount(hackType, 0);
                double violationRatio = (violationCount / ((double) CancelViolation.get(hackType, dataType))),
                        thresholdSurpassing = violationRatio / Check.analysisMultiplier;

                if (thresholdSurpassing >= 1.0) {
                    evidence.add(hackType,
                            "suspicion: " + AlgebraUtils.integerRound(thresholdSurpassing * 100.0) + "%"
                    );
                    judgeEvidence();
                    PlayerInfo.refresh(player.getName());
                    SpartanMenu.refresh();
                    return true;
                }
            }
        }
        return false;
    }

    public void resetLiveEvidence(Enums.HackType hackType) {
        if (!ViolationStatistics.has(hackType, 0)) {
            evidence.clear();
        }
    }

    public PlayerEvidence getEvidence() {
        return evidence;
    }

    private void judgeEvidence() {
        switch (evidence.judge()) {
            case Suspected:
                ResearchEngine.addSuspected(this);
                break;
            case Hacker:
                ResearchEngine.addHacker(this);
                break;
            default:
                ResearchEngine.addLegitimate(this);
                break;
        }
    }

    boolean shouldCalculateEvidence() {
        return (staff || evidence.noCalculations() || wasRecentlyOnline()) && getUsefulLogs() > 0;
    }

    public boolean isTrustWorthy(boolean history) {
        return !wasStaff()
                && !wasTesting()
                && (!history || punishmentHistory.getOverall(true) == 0);
    }

    // Separator

    public MiningHistory[] getMiningHistory() {
        return miningHistory;
    }

    public MiningHistory getOverallMiningHistory() {
        int mines = 0, days = 0;

        for (MiningHistory miningHistory : getMiningHistory()) {
            mines += miningHistory.getMines();
            days += miningHistory.getDays();
        }
        return new MiningHistory(null, mines, Math.max(days, 1));
    }

    public MiningHistory getMiningHistory(Enums.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    // Separator

    public boolean isLegitimate() {
        return evidence.has(PlayerEvidence.EvidenceType.Legitimate) != null;
    }

    public boolean isHacker() {
        return !evidence.has(PlayerEvidence.EvidenceType.Hacker).isEmpty();
    }

    public boolean isSuspected() {
        return !evidence.has(PlayerEvidence.EvidenceType.Suspected).isEmpty();
    }

    public boolean isSuspectedOrHacker() {
        return isSuspected() || isHacker();
    }

    public boolean isSuspectedOrHacker(Enums.HackType[] hackTypes) {
        Collection<Enums.HackType> evidence = this.evidence.has(PlayerEvidence.EvidenceType.Suspected);

        for (Enums.HackType hackType : hackTypes) {
            if (evidence.contains(hackType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSuspectedOrHacker(Enums.HackType hackType) {
        return evidence.has(PlayerEvidence.EvidenceType.Suspected).contains(hackType) || isHacker();
    }

    // Separator

    public int getUsefulLogs(ViolationHistory violationHistory) {
        return violationHistory.getAllViolations()
                + getOverallMiningHistory().getMines()
                + getPunishmentHistory().getOverall(true);
    }

    public int getUsefulLogs() {
        int sum = 0;

        for (ViolationHistory violationHistory : getViolationHistory()) {
            sum += getUsefulLogs(violationHistory);
        }
        return sum;
    }

    public PunishmentHistory getPunishmentHistory() {
        return punishmentHistory;
    }

    // Separator

    public PlayerCombat getCombat() {
        return playerCombat;
    }
}
