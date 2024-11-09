package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerProfile {

    public final String name;
    private final MiningHistory[] miningHistory;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;
    private final CheckExecutor[] executors;
    private Check.DataType lastDataType;

    // Separator

    public PlayerProfile(String name) {
        this.name = name;
        this.skull = null;
        this.executors = new CheckExecutor[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];
        this.lastDataType = Check.DataType.JAVA;

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }

        // Separator

        SpartanProtocol protocol = SpartanBukkit.getProtocol(name);

        if (protocol != null) {
            this.offlinePlayer = protocol.bukkit;
            this.registerExecutors(protocol);
        } else {
            this.offlinePlayer = null;
            this.registerExecutors(null);
        }
    }

    public PlayerProfile(SpartanProtocol protocol) {
        this.name = protocol.bukkit.getName();
        this.offlinePlayer = protocol.bukkit; // Attention
        this.skull = null;
        this.executors = new CheckExecutor[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];
        this.lastDataType = protocol.spartan.dataType;

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
        this.registerExecutors(protocol);
    }

    // Separator

    public Check.DataType getLastDataType() {
        return lastDataType;
    }

    public void update(SpartanProtocol protocol) {
        this.offlinePlayer = protocol.bukkit;
        this.lastDataType = protocol.spartan.dataType;

        for (CheckExecutor executor : this.getExecutors()) {
            executor.setProtocol(protocol);

            for (DetectionExecutor detectionExecutor : executor.getDetections()) {
                detectionExecutor.setProtocol(protocol);
            }
        }
    }

    public SpartanProtocol protocol() {
        return this.executors[0].protocol();
    }

    public CheckExecutor getExecutor(Enums.HackType hackType) {
        int ordinal = hackType.ordinal();

        synchronized (this.executors) {
            return this.executors[ordinal];
        }
    }

    public CheckExecutor[] getExecutors() {
        synchronized (this.executors) {
            return this.executors;
        }
    }

    private void registerExecutors(SpartanProtocol protocol) {
        synchronized (this.executors) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                try {
                    CheckExecutor executor = (CheckExecutor) hackType.executor
                            .getConstructor(hackType.getClass(), SpartanProtocol.class)
                            .newInstance(hackType, protocol);
                    this.executors[hackType.ordinal()] = executor;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // Separator

    public ItemStack getSkull() {
        if (this.skull == null) {
            if (this.offlinePlayer == null) {
                return InventoryUtils.getSkull(null, name, false);
            } else {
                this.skull = InventoryUtils.getSkull(offlinePlayer, name, false);
            }
        }
        return this.skull;
    }

    // Separator

    public MiningHistory getMiningHistory(MiningHistory.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    // Separator

    public Collection<Enums.HackType> getEvidenceList(double threshold) {
        List<Enums.HackType> set = new ArrayList<>();

        for (CheckExecutor executor : this.getExecutors()) {
            double probability = executor.getExtremeProbability(this.lastDataType);

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.add(executor.hackType);
            }
        }
        return set;
    }

    public Set<Map.Entry<Enums.HackType, Double>> getEvidenceEntries(
            double threshold
    ) {
        Map<Enums.HackType, Double> set = new HashMap<>();

        for (CheckExecutor executor : this.getExecutors()) {
            double probability = executor.getExtremeProbability(this.lastDataType);

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.put(executor.hackType, probability);
            }
        }
        return set.entrySet();
    }

}
