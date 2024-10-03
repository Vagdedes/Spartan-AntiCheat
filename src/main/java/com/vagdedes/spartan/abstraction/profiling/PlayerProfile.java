package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class PlayerProfile {

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
        this.evidence = new PlayerEvidence(this);
        this.skull = null;
        this.offlinePlayer = null;

        // Separator
        this.violationHistory = new ViolationHistory[Check.DataType.values().length][hackTypes.length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            for (Enums.HackType hackType : hackTypes) {
                this.violationHistory[dataType.ordinal()][hackType.ordinal()] = new ViolationHistory();
            }
        }
        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
    }

    public PlayerProfile(SpartanProtocol protocol) {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        this.name = protocol.player.getName();
        this.offlinePlayer = protocol.player; // Attention
        this.evidence = new PlayerEvidence(this);
        this.skull = null;

        this.violationHistory = new ViolationHistory[Check.DataType.values().length][hackTypes.length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            for (Enums.HackType hackType : hackTypes) {
                this.violationHistory[dataType.ordinal()][hackType.ordinal()] = new ViolationHistory();
            }
        }
        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
    }

    public void update(SpartanProtocol protocol) {
        this.offlinePlayer = protocol.player;
    }

    // Separator

    public ItemStack getSkull() {
        if (this.skull == null) {
            if (this.offlinePlayer == null) {
                SpartanProtocol protocol = SpartanBukkit.getProtocol(name);

                if (protocol == null) {
                    return InventoryUtils.getSkull(null, name, false);
                } else {
                    this.update(SpartanBukkit.getProtocol(protocol.player));
                    this.skull = InventoryUtils.getSkull(protocol.player, name, false);
                }
            } else {
                this.skull = InventoryUtils.getSkull(offlinePlayer, name, false);
            }
        }
        return this.skull;
    }

    SpartanPlayer getSpartanPlayer() {
        SpartanProtocol protocol;

        if (this.offlinePlayer == null) {
            protocol = SpartanBukkit.getProtocol(name);

            if (protocol != null) {
                this.update(protocol);
                return protocol.spartanPlayer;
            } else {
                return null;
            }
        } else {
            protocol = SpartanBukkit.getProtocol(this.offlinePlayer.getUniqueId());
            return protocol != null ? protocol.spartanPlayer : null;
        }
    }

    // Separator

    public ViolationHistory getViolationHistory(Check.DataType dataType, Enums.HackType hackType) {
        return violationHistory[dataType.ordinal()][hackType.ordinal()];
    }

    public MiningHistory getMiningHistory(MiningHistory.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    public boolean hasData(Enums.HackType hackType) {
        for (Check.DataType dataType : Check.DataType.values()) {
            if (!violationHistory[dataType.ordinal()][hackType.ordinal()].isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasData(Check.DataType dataType) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            if (!violationHistory[dataType.ordinal()][hackType.ordinal()].isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
