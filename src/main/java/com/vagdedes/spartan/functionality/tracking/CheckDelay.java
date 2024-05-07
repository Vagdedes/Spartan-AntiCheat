package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckDelay {

    private static final Enums.HackType[] mostlyAffected;
    private static long cooldown = 0L, delay = 0L;
    private static final Cooldowns cooldowns = new Cooldowns(null);

    static {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        List<Enums.HackType> checksAffectedByCombatArray = new ArrayList<>(hackTypes.length);

        for (Enums.HackType hackType : hackTypes) {
            switch (hackType.category) {
                case MOVEMENT:
                case COMBAT:
                case EXPLOITS:
                    checksAffectedByCombatArray.add(hackType);
                    break;
                default:
                    break;
            }
        }
        mostlyAffected = checksAffectedByCombatArray.toArray(new Enums.HackType[0]);
    }

    public static void cancel(UUID uuid, int ticks) {
        cooldowns.add(uuid.toString(), ticks);
    }

    public static void cancel(int ticks, int secondsDelay) {
        long ms = System.currentTimeMillis();

        if (delay <= ms) {
            if (secondsDelay > 0) {
                delay = ms + (secondsDelay * 1_000L);
            }
            cooldown = ms + (ticks * TPS.tickTime);
        }
    }

    public static boolean hasCooldown(SpartanPlayer player) {
        return cooldown >= System.currentTimeMillis()
                || !cooldowns.canDo(player.uuid.toString());
    }

    public static void evadeCommonFalsePositives(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, Enums.HackType[] hackTypes, int ticks) {
        for (Enums.HackType hackType : hackTypes) {
            player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
        }
    }

    public static void evadeStandardCombatFPs(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, int ticks) {
        evadeCommonFalsePositives(player, compatibilityType, mostlyAffected, ticks);
    }
}
