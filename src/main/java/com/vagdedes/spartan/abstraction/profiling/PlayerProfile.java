package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerProfile {

    public static final String
            onlineFor = " was online for: ",
            afkFor = " was AFK for: ";

    public final String name;
    private final MiningHistory[] miningHistory;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;
    private final CheckRunner[] runners;
    private Check.DataType lastDataType;

    // Separator

    public PlayerProfile(String name) {
        this.name = name;
        this.skull = null;
        this.runners = new CheckRunner[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];
        this.lastDataType = Check.DataType.JAVA;

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }

        // Separator

        SpartanProtocol protocol = SpartanBukkit.getProtocol(name);

        if (protocol != null) {
            this.offlinePlayer = protocol.bukkit;
            this.registerChecks(protocol);
        } else {
            this.offlinePlayer = null;
            this.registerChecks(null);
        }
    }

    public PlayerProfile(SpartanProtocol protocol) {
        this.name = protocol.bukkit.getName();
        this.offlinePlayer = protocol.bukkit; // Attention
        this.skull = null;
        this.runners = new CheckRunner[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];
        this.lastDataType = protocol.spartan.dataType;

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(ore);
        }
        this.registerChecks(protocol);
    }

    // Separator

    public void setOnlineFor(long time, long count, boolean log) {
        if (log) {
            AntiCheatLogs.rawLogInfo(
                    time,
                    this.name + onlineFor + count,
                    false,
                    true,
                    true
            );
        }
        for (CheckRunner runner : this.getRunners()) {
            runner.trackTime(lastDataType, time, count);
        }
    }

    public void setAFKFor(long time, long count, boolean log) {
        if (log) {
            AntiCheatLogs.rawLogInfo(
                    time,
                    this.name + afkFor + count,
                    false,
                    true,
                    true
            );
        }
        for (CheckRunner runner : this.getRunners()) {
            runner.trackTime(lastDataType, time, -count);
        }
    }

    // Separator

    public Check.DataType getLastDataType() {
        return lastDataType;
    }

    public void update(SpartanProtocol protocol) {
        this.offlinePlayer = protocol.bukkit;
        this.lastDataType = protocol.spartan.dataType;

        for (CheckRunner executor : this.getRunners()) {
            executor.setProtocol(protocol);

            for (CheckDetection detectionExecutor : executor.getDetections()) {
                detectionExecutor.setProtocol(protocol);
            }
        }
    }

    public SpartanProtocol protocol() {
        return this.runners[0].protocol();
    }

    public CheckRunner getRunner(Enums.HackType hackType) {
        int ordinal = hackType.ordinal();

        synchronized (this.runners) {
            return this.runners[ordinal];
        }
    }

    public CheckRunner[] getRunners() {
        synchronized (this.runners) {
            return this.runners;
        }
    }

    private void registerChecks(SpartanProtocol protocol) {
        synchronized (this.runners) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                try {
                    CheckRunner executor = (CheckRunner) hackType.executor
                            .getConstructor(hackType.getClass(), SpartanProtocol.class)
                            .newInstance(hackType, protocol);
                    this.runners[hackType.ordinal()] = executor;
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
                return this.skull = InventoryUtils.getSkull(offlinePlayer, name, false);
            }
        } else {
            return this.skull;
        }
    }

    // Separator

    public MiningHistory getMiningHistory(MiningHistory.MiningOre ore) {
        return miningHistory[ore.ordinal()];
    }

    // Separator

    public Collection<Enums.HackType> getEvidenceList(double threshold) {
        List<Enums.HackType> set = new ArrayList<>();

        for (CheckRunner executor : this.getRunners()) {
            double probability = executor.getExtremeProbability(this.lastDataType);

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.add(executor.hackType);
            }
        }
        return set;
    }

    public Set<Map.Entry<Enums.HackType, Double>> getEvidenceEntries(double threshold) {
        Map<Enums.HackType, Double> set = new HashMap<>();

        for (CheckRunner executor : this.getRunners()) {
            double probability = executor.getExtremeProbability(this.lastDataType);

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.put(executor.hackType, probability);
            }
        }
        return set.entrySet();
    }

}
