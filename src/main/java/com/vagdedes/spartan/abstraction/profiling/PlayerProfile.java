package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.BedrockCompatibility;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.UUID;

public class PlayerProfile {

    private UUID uuid;
    public final String name;
    private final ViolationHistory[] violationHistory;
    private final MiningHistory[] miningHistory;
    public final PunishmentHistory punishmentHistory;
    public final PlayerEvidence evidence;
    private boolean bedrockPlayer, bedrockPlayerCheck;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;
    public final PlayerCombat playerCombat;

    // Separator

    public PlayerProfile() {
        this.uuid = null;
        this.name = Double.toString(new Random().nextDouble());
        this.punishmentHistory = null;
        this.playerCombat = null;
        this.evidence = null;
        this.skull = null;
        this.offlinePlayer = null;
        this.bedrockPlayer = false;
        this.bedrockPlayerCheck = true;
        this.violationHistory = null;
        this.miningHistory = null;
    }

    public PlayerProfile(String name) {
        Enums.HackType[] hackTypes = Enums.HackType.values();

        // Separator
        this.uuid = null;
        this.name = name;
        this.punishmentHistory = new PunishmentHistory();
        this.playerCombat = new PlayerCombat(this);
        this.evidence = new PlayerEvidence();
        this.skull = null;
        this.offlinePlayer = null;

        // Separator
        SpartanPlayer player = this.getSpartanPlayer();

        if (player != null) {
            this.bedrockPlayer = player.bedrockPlayer;
            this.bedrockPlayerCheck = true;
        } else {
            this.bedrockPlayer = BedrockCompatibility.isPlayer(name);
            this.bedrockPlayerCheck = bedrockPlayer;
        }

        // Separator
        this.violationHistory = new ViolationHistory[hackTypes.length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (Enums.HackType hackType : hackTypes) {
            this.violationHistory[hackType.ordinal()] = new ViolationHistory();
        }
        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore, 0);
        }
    }

    public PlayerProfile(SpartanPlayer player) {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        this.uuid = player.uuid;
        this.name = player.name;
        this.offlinePlayer = player.getInstance(); // Attention
        this.punishmentHistory = new PunishmentHistory();
        this.playerCombat = new PlayerCombat(this);
        this.evidence = new PlayerEvidence();
        this.skull = null;
        this.offlinePlayer = null;
        this.bedrockPlayer = player.bedrockPlayer; // Attention
        this.bedrockPlayerCheck = true;

        this.violationHistory = new ViolationHistory[hackTypes.length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (Enums.HackType hackType : hackTypes) {
            this.violationHistory[hackType.ordinal()] = new ViolationHistory();
        }
        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore, 0);
        }
    }

    // Separator

    public String getName() {
        return name;
    }

    public ItemStack getSkull(boolean force) {
        if (skull == null) {
            if (force) {
                OfflinePlayer player = getOfflinePlayer();

                if (player == null) {
                    return InventoryUtils.getHead();
                } else {
                    this.skull = InventoryUtils.getSkull(player, name, false);
                }
            } else {
                SpartanPlayer spartanPlayer = getSpartanPlayer();

                if (spartanPlayer == null) {
                    return InventoryUtils.getSkull(null, name, false);
                } else {
                    Player player = spartanPlayer.getInstance();

                    if (player == null) {
                        return InventoryUtils.getSkull(null, name, false);
                    } else {
                        this.skull = InventoryUtils.getSkull(player, name, false);
                    }
                }
            }
        }
        return skull;
    }

    public Enums.DataType getDataType() {
        return isBedrockPlayer() ? Enums.DataType.BEDROCK : Enums.DataType.JAVA;
    }

    public boolean isBedrockPlayer() {
        if (bedrockPlayer) {
            return true;
        }
        if (!bedrockPlayerCheck) {
            SpartanPlayer player = getSpartanPlayer();

            if (player != null) {
                bedrockPlayerCheck = true;

                if (player.bedrockPlayer) {
                    return bedrockPlayer = true;
                }
            }
        }
        return false;
    }

    public OfflinePlayer getOfflinePlayer() {
        if (offlinePlayer == null) {
            if (this.uuid == null) {
                SpartanPlayer player = getSpartanPlayer();

                if (player != null) {
                    this.uuid = player.uuid;
                    this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                } else {
                    this.offlinePlayer = Bukkit.getOfflinePlayer(name);
                    this.uuid = offlinePlayer.getUniqueId();
                }
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
        SpartanProtocol protocol;

        if (uuid != null) {
            protocol = SpartanBukkit.getProtocol(uuid);
        } else {
            protocol = SpartanBukkit.getProtocol(name);
        }
        return protocol != null ? protocol.spartanPlayer : null;
    }

    public boolean isOnline() {
        return getSpartanPlayer() != null;
    }

    // Separator

    public ViolationHistory[] getViolationHistory() {
        return violationHistory;
    }

    public ViolationHistory getViolationHistory(Enums.HackType hackType) {
        return violationHistory[hackType.ordinal()];
    }

    // Separator

    public MiningHistory[] getMiningHistory() {
        return miningHistory;
    }

    public MiningHistory getOverallMiningHistory() {
        int mines = 0, days = 0;

        for (MiningHistory miningHistory : getMiningHistory()) {
            mines += miningHistory.getMines();
            days = Math.max(miningHistory.getDays(), days);
        }
        return new MiningHistory(null, mines);
    }

    public MiningHistory getMiningHistory(MiningHistory.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    // Separator

    public boolean isLegitimate() {
        return evidence.has(PlayerEvidence.EvidenceType.LEGITIMATE);
    }

}
