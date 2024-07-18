package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerProfile {

    private UUID uuid;
    public final String name;
    private final ViolationHistory[][] violationHistory;
    private final MiningHistory[] miningHistory;
    public final PlayerEvidence evidence;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;

    // Separator

    public PlayerProfile(String name) {
        Enums.HackType[] hackTypes = Enums.HackType.values();

        // Separator
        this.name = name;
        this.evidence = new PlayerEvidence();
        this.skull = null;
        this.offlinePlayer = null;

        // Separator
        this.violationHistory = new ViolationHistory[ResearchEngine.usableDataTypes.length][hackTypes.length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (Enums.DataType dataType : ResearchEngine.usableDataTypes) {
            for (Enums.HackType hackType : hackTypes) {
                this.violationHistory[dataType.ordinal()][hackType.ordinal()] = new ViolationHistory();
            }
        }
        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
    }

    public PlayerProfile(SpartanPlayer player) {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        this.uuid = player.uuid;
        this.name = player.name;
        this.offlinePlayer = player.getInstance(); // Attention
        this.evidence = new PlayerEvidence();
        this.skull = null;
        this.offlinePlayer = null;

        this.violationHistory = new ViolationHistory[ResearchEngine.usableDataTypes.length][hackTypes.length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (Enums.DataType dataType : ResearchEngine.usableDataTypes) {
            for (Enums.HackType hackType : hackTypes) {
                this.violationHistory[dataType.ordinal()][hackType.ordinal()] = new ViolationHistory();
            }
        }
        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
    }

    public void update(SpartanPlayer player) {
        this.uuid = player.uuid;
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

    // Separator

    public ViolationHistory getViolationHistory(Enums.DataType dataType, Enums.HackType hackType) {
        return violationHistory[dataType.ordinal()][hackType.ordinal()];
    }

    public MiningHistory getMiningHistory(MiningHistory.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    public boolean hasData(Enums.DataType dataType) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (!violationHistory[dataType.ordinal()][hackType.ordinal()].isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
