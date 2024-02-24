package com.vagdedes.spartan.handlers.identifiers.simple;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckProtection {

    private static final Enums.HackType[] checksAffectedByCombatOrMovement;
    private static long cooldown = 0L, delay = 0L;

    static {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        List<Enums.HackType> checksAffectedByCombatArray = new ArrayList<>(hackTypes.length);

        for (Enums.HackType hackType : hackTypes) {
            switch (hackType.getCheck().getCheckType()) {
                case MOVEMENT:
                case COMBAT:
                case EXPLOITS:
                    checksAffectedByCombatArray.add(hackType);
                    break;
                default:
                    break;
            }
        }
        checksAffectedByCombatOrMovement = checksAffectedByCombatArray.toArray(new Enums.HackType[0]);
    }
    public static void cancel(UUID uuid, int ticks) {
        SpartanBukkit.cooldowns.add(uuid + "=check=protection", ticks);
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
                || player.getHandlers().has(Handlers.HandlerType.GameMode)
                || !SpartanBukkit.cooldowns.canDo(player.getUniqueId() + "=check=protection");
    }

    public static void evadeCommonFalsePositives(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, Enums.HackType[] hackTypes, int ticks) {
        for (Enums.HackType hackType : hackTypes) {
            player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
        }
    }

    public static void evadeStandardCombatFPs(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, int ticks) {
        evadeCommonFalsePositives(player, compatibilityType, checksAffectedByCombatOrMovement, ticks);
    }
}
