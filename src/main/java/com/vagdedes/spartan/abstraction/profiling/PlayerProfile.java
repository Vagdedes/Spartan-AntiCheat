package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerProfile {

    private static final Set<Enums.HackType> emptySet = new HashSet<>(0);
    public final String name;
    private final MiningHistory[] miningHistory;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;
    private final CheckExecutor[] executors;

    // Separator

    public PlayerProfile(String name) {
        this.name = name;
        this.skull = null;
        this.offlinePlayer = null;
        this.executors = new CheckExecutor[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
        this.registerExecutors(emptySet);
    }

    public PlayerProfile(SpartanProtocol protocol) {
        this.name = protocol.player.getName();
        this.offlinePlayer = protocol.player; // Attention
        this.skull = null;
        this.executors = new CheckExecutor[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
        this.registerExecutors(emptySet);
    }

    // Separator

    public void updateOfflinePlayer(SpartanProtocol protocol) {
        this.offlinePlayer = protocol.player;
    }

    public void updateOnlinePlayer(SpartanProtocol protocol) {
        if (protocol == null) {
            for (CheckExecutor executor : executors) {
                executor.player = null;

                for (DetectionExecutor detectionExecutor : executor.getDetections()) {
                    detectionExecutor.player = null;
                }
            }
        } else {
            this.updateOfflinePlayer(protocol);

            for (CheckExecutor executor : executors) {
                executor.player = protocol.spartanPlayer;

                for (DetectionExecutor detectionExecutor : executor.getDetections()) {
                    detectionExecutor.player = protocol.spartanPlayer;
                }
            }
        }
    }

    // Separator

    public CheckExecutor getExecutor(Enums.HackType hackType) {
        return executors[hackType.ordinal()];
    }

    public CheckExecutor[] getExecutors() {
        return executors;
    }

    public void registerExecutors(Set<Enums.HackType> registry) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            try {
                CheckExecutor executor = (CheckExecutor) hackType.executor
                        .getConstructor(hackType.getClass(), SpartanPlayer.class)
                        .newInstance(hackType, this.getSpartanPlayer());

                if (registry.contains(hackType)
                        || this.executors[hackType.ordinal()] == null) {
                    this.executors[hackType.ordinal()] = executor;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Separator

    public ItemStack getSkull() {
        if (this.skull == null) {
            if (this.offlinePlayer == null) {
                SpartanProtocol protocol = SpartanBukkit.getProtocol(name);

                if (protocol == null) {
                    return InventoryUtils.getSkull(null, name, false);
                } else {
                    this.updateOfflinePlayer(SpartanBukkit.getProtocol(protocol.player));
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
                this.updateOfflinePlayer(protocol);
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

    public MiningHistory getMiningHistory(MiningHistory.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    public boolean hasData(Enums.HackType hackType) {
        for (DetectionExecutor detectionExecutor : this.getExecutor(hackType).getDetections()) {
            if (detectionExecutor.hasDataToCompare()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasData(Check.DataType dataType) {
        for (CheckExecutor checkExecutor : this.getExecutors()) {
            for (DetectionExecutor detectionExecutor : checkExecutor.getDetections()) {
                if (detectionExecutor.hasDataToCompare()) {
                    return detectionExecutor.player != null
                            && detectionExecutor.player.dataType == dataType;
                }
            }
        }
        return false;
    }

    // Separator

    public Collection<Enums.HackType> getEvidenceList(double threshold) {
        List<Enums.HackType> set = new ArrayList<>();

        for (CheckExecutor executor : executors) {
            double probability = executor.getExtremeProbability();

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.add(executor.hackType);
            }
        }
        return set;
    }

    public Set<Map.Entry<Enums.HackType, Double>> getEvidenceEntries(double threshold) {
        Map<Enums.HackType, Double> set = new HashMap<>();

        for (CheckExecutor executor : executors) {
            double probability = executor.getExtremeProbability();

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.put(executor.hackType, probability);
            }
        }
        return set.entrySet();
    }

}
