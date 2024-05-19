package com.vagdedes.spartan.listeners.protocol;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import com.vagdedes.spartan.listeners.protocol.modules.RotationData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolStorage {

    static final Map<UUID, AbilitiesContainer> playerAbilitiesContainer = new ConcurrentHashMap<>();
    static final Map<UUID, SpartanLocation>
            positionManager = new ConcurrentHashMap<>(),
            lastTeleport = new ConcurrentHashMap<>();
    static final Map<UUID, RotationData> lastRotation = new ConcurrentHashMap<>();
    static final Map<UUID, Boolean>
            groundManager = new ConcurrentHashMap<>(),
            spawnStatus = new ConcurrentHashMap<>(),
            canCheck = new ConcurrentHashMap<>();

    public static boolean canCheck(UUID uuid) {
        return canCheck.getOrDefault(uuid, false);
    }

    public static AbilitiesContainer getAbilities(UUID uuid) {
        return playerAbilitiesContainer.computeIfAbsent(
                uuid,
                k -> new AbilitiesContainer(false, false, false)
        );
    }

    public static RotationData getRotation(UUID uuid) {
        return lastRotation.computeIfAbsent(
                uuid,
                k -> new RotationData(0, 0)
        );
    }

    public static boolean getSpawnStatus(UUID uuid) {
        return spawnStatus.getOrDefault(uuid, false);
    }

    public static SpartanLocation getLocation(UUID uuid, SpartanLocation defaultLocation) {
        return ProtocolStorage.positionManager.computeIfAbsent(
                uuid,
                k -> defaultLocation
        );
    }

    public static SpartanLocation getLocation(UUID uuid) {
        return ProtocolStorage.positionManager.computeIfAbsent(
                uuid,
                k -> new SpartanLocation()
        );
    }

    public static Boolean isOnGround(SpartanPlayer player) {
        return ProtocolStorage.groundManager.getOrDefault(player.uuid, false);
    }
}
