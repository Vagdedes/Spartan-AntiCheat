package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.compatibility.necessary.bedrock.BedrockCompatibility;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.gui.spartan.MainMenu;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

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
        this.violationHistory = new ViolationHistory[hackTypes.length];
        this.miningHistory = new MiningHistory[Enums.MiningOre.values().length];

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

        this.violationHistory = new ViolationHistory[hackTypes.length];
        this.miningHistory = new MiningHistory[Enums.MiningOre.values().length];

        for (Enums.HackType hackType : hackTypes) {
            this.violationHistory[hackType.ordinal()] = new ViolationHistory(hackType, getDataType());
        }
        for (Enums.MiningOre ore : Enums.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore, 0, 1);
        }
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
        return isOnline() || (System.currentTimeMillis() - lastPlayed) <= (ResearchEngine.cacheRefreshTicks * TPS.tickTime);
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

    public boolean calculateLiveEvidence(SpartanPlayer player, Enums.HackType hackType, ResearchEngine.DataType dataType) {
        synchronized (evidence.live) {
            if (evidence.live.containsKey(hackType) || evidence.historical.containsKey(hackType)) {
                return true;
            }
            Check check = hackType.getCheck();

            if (check.supportsLiveEvidence()) {
                int violationCount = player.getViolations(hackType).getLevel() - check.getCancelViolation();

                if (violationCount > 0) {
                    violationCount += punishmentHistory.getReportCount(hackType, 0);
                    double violationRatio = (violationCount / ((double) CancelViolation.get(hackType, dataType))),
                            thresholdSurpassing = violationRatio / Check.analysisMultiplier;

                    if (thresholdSurpassing >= 1.0) {
                        evidence.live.put(hackType,
                                "Average: " + AlgebraUtils.integerRound(thresholdSurpassing * 100.0) + "%"
                        );
                        evidence.judge();
                        SpartanMenu.playerInfo.refresh(player.getName());
                        MainMenu.refresh();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public void removeFromLiveEvidence(Enums.HackType hackType) {
        synchronized (evidence.live) {
            evidence.live.remove(hackType);
            evidence.judge();
        }
    }

    public void removeFromHistoricalEvidence(Enums.HackType hackType) {
        synchronized (evidence.live) {
            evidence.historical.remove(hackType);
            evidence.judge();
        }
    }

    public void removeFromAllEvidence(Enums.HackType hackType) {
        synchronized (evidence.live) {
            evidence.live.remove(hackType);
            evidence.historical.remove(hackType);
            evidence.judge();
        }
    }

    public PlayerEvidence getEvidence() {
        return evidence;
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
        return getEvidence().has(PlayerEvidence.EvidenceType.Legitimate);
    }

    public boolean isHacker() {
        return getEvidence().has(PlayerEvidence.EvidenceType.Hacker);
    }

    public boolean isSuspected() {
        return getEvidence().has(PlayerEvidence.EvidenceType.Suspected);
    }

    public boolean isSuspected(Enums.HackType[] hackTypes) {
        synchronized (evidence.live) {
            if (evidence.has(PlayerEvidence.EvidenceType.Suspected)) {
                for (Enums.HackType hackType : hackTypes) {
                    if (evidence.live.containsKey(hackType)
                            || evidence.historical.containsKey(hackType)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean isSuspected(Enums.HackType hackType) {
        synchronized (evidence.live) {
            return evidence.has(PlayerEvidence.EvidenceType.Suspected)
                    && (evidence.live.containsKey(hackType)
                    || evidence.historical.containsKey(hackType));
        }
    }

    public boolean isSuspectedOrHacker() {
        return isSuspected() || isHacker();
    }

    public boolean isSuspectedOrHacker(Enums.HackType[] hackTypes) {
        return isHacker() || isSuspected(hackTypes);
    }

    public boolean isSuspectedOrHacker(Enums.HackType hackType) {
        return isHacker() || isSuspected(hackType);
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
