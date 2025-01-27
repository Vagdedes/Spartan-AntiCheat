package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import lombok.Getter;
import lombok.Setter;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerProfile {

    public static final String
            activeFor = " was active for: ";

    public final String name;
    private final MiningHistory[] miningHistory;
    private ItemStack skull;
    private OfflinePlayer offlinePlayer;
    @Getter
    private final CheckRunner[] runners;
    @Getter
    @Setter
    private Check.DataType lastDataType;
    @Getter
    private Check.DetectionType lastDetectionType;
    @Getter
    private final ProfileContinuity continuity;

    // Separator

    public PlayerProfile(String name) {
        this.name = name;
        this.skull = null;
        this.runners = new CheckRunner[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];
        this.lastDataType = Check.DataType.JAVA;
        this.lastDetectionType = PluginBase.packetsEnabled()
                ? Check.DetectionType.PACKETS
                : Check.DetectionType.BUKKIT;
        this.continuity = new ProfileContinuity(this);

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(this, ore);
        }

        // Separator

        PlayerProtocol protocol = PluginBase.getProtocol(name);

        if (protocol != null) {
            this.offlinePlayer = protocol.bukkit();
            this.registerRunners(protocol);
        } else {
            this.offlinePlayer = null;
            this.registerRunners(null);
        }
    }

    public PlayerProfile(PlayerProtocol protocol) {
        this.name = protocol.bukkit().getName();
        this.offlinePlayer = protocol.bukkit(); // Attention
        this.skull = null;
        this.runners = new CheckRunner[Enums.HackType.values().length];
        this.miningHistory = new MiningHistory[MiningHistory.MiningOre.values().length];
        this.lastDataType = protocol.bukkitExtra.dataType;
        this.lastDetectionType = protocol.packetsEnabled()
                ? Check.DetectionType.PACKETS
                : Check.DetectionType.BUKKIT;
        this.continuity = new ProfileContinuity(this);

        for (MiningHistory.MiningOre ore : MiningHistory.MiningOre.values()) {
            this.miningHistory[ore.ordinal()] = new MiningHistory(this, ore);
        }
        this.registerRunners(protocol);
    }

    // Separator

    public void update(PlayerProtocol protocol) {
        this.offlinePlayer = protocol.bukkit();
        this.lastDataType = protocol.bukkitExtra.dataType;
        this.lastDetectionType = protocol.bukkitExtra.detectionType;
        this.registerRunners(protocol);
    }

    PlayerProtocol protocol() {
        return this.runners[0].protocol;
    }

    public boolean isOnline() {
        PlayerProtocol protocol = this.protocol();
        return protocol != null && PluginBase.isOnline(protocol);
    }

    public CheckRunner getRunner(Enums.HackType hackType) {
        return this.runners[hackType.ordinal()];
    }

    public void executeRunners(Object cancelled, Object object) {
        for (CheckRunner runner : this.getRunners()) {
            runner.handle(cancelled, object);
        }
    }

    private void registerRunners(PlayerProtocol protocol) {
        for (Enums.HackType hackType : Enums.HackType.values()) {
            try {
                CheckRunner
                        oldRunner = this.runners[hackType.ordinal()],
                        runner = (CheckRunner) hackType.executor
                                .getConstructor(hackType.getClass(), PlayerProtocol.class)
                                .newInstance(hackType, protocol);
                this.runners[hackType.ordinal()] = runner;

                if (oldRunner != null) {
                    for (CheckDetection detection : oldRunner.getDetections()) {
                        for (Check.DataType dataType : Check.DataType.values()) {
                            CheckDetection newDetection = runner.getDetection(detection.name);

                            if (newDetection != null) {
                                newDetection.setProbability(
                                        dataType,
                                        detection.getProbability(dataType)
                                );
                            }
                        }
                    }
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
            double probability = executor.getExtremeProbability(
                    this.lastDataType,
                    this.lastDetectionType
            );

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.add(executor.hackType);
            }
        }
        return set;
    }

    public Set<Map.Entry<Enums.HackType, Double>> getEvidenceEntries(double threshold) {
        Map<Enums.HackType, Double> set = new HashMap<>();

        for (CheckRunner executor : this.getRunners()) {
            double probability = executor.getExtremeProbability(
                    this.lastDataType,
                    this.lastDetectionType
            );

            if (PlayerEvidence.surpassedProbability(probability, threshold)) {
                set.put(executor.hackType, probability);
            }
        }
        return set.entrySet();
    }

}
