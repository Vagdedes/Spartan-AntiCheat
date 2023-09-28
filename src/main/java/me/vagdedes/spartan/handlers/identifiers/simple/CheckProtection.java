package me.vagdedes.spartan.handlers.identifiers.simple;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckProtection {

    private static final Enums.HackType[] checksAffectedByCombatOrMovement;
    private static long cooldown = 0L;
    private static long delay = 0L;

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

    public static void cancel(UUID uuid, int ticks, boolean ignoreNumbers) {
        SpartanBukkit.cooldowns.add(uuid + "=check=protection", ticks);

        if (ignoreNumbers) {
            SpartanBukkit.cooldowns.add(uuid + "=check=protection=ignore-numbers", ticks);
        }
    }

    public static void cancel(int ticks, int secondsDelay) {
        long ms = System.currentTimeMillis();

        if (delay <= ms) {
            if (secondsDelay > 0) {
                delay = ms + (secondsDelay * 1_000L);
            }
            cooldown = ms + (ticks * 50L);
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                for (SpartanPlayer p : players) {
                    cancel(p.getUniqueId(), 5, false);
                }
            }
        }
    }

    public static boolean hasCooldown(UUID uuid, PlayerViolation playerViolation) {
        if (cooldown >= System.currentTimeMillis()
                || !SpartanBukkit.cooldowns.canDo(uuid + "=check=protection")) { // has cooldown
            if (SpartanBukkit.cooldowns.canDo(uuid + "=check=protection=ignore-numbers")) {
                List<Number> numbers = playerViolation.getNumbersList();

                if (!numbers.isEmpty()) {
                    for (Number number : numbers) {
                        if (number instanceof Double && number.doubleValue() >= 1.0) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static void evadeCommonFalsePositives(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, Enums.HackType[] hackTypes, int ticks) {
        for (Enums.HackType hackType : hackTypes) {
            hackType.getCheck().addDisabledUser(player.getUniqueId(), compatibilityType.toString(), ticks);
        }
    }

    public static void evadeCommonFalsePositives(Player player, Compatibility.CompatibilityType compatibilityType, Enums.HackType[] hackTypes, int ticks) {
        for (Enums.HackType hackType : hackTypes) {
            hackType.getCheck().addDisabledUser(player.getUniqueId(), compatibilityType.toString(), ticks);
        }
    }

    public static void evadeStandardCombatFPs(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, int ticks) {
        evadeCommonFalsePositives(player, compatibilityType, checksAffectedByCombatOrMovement, ticks);
    }

    public static void evadeStandardCombatFPs(Player player, Compatibility.CompatibilityType compatibilityType, int ticks) {
        evadeCommonFalsePositives(player, compatibilityType, checksAffectedByCombatOrMovement, ticks);
    }
}
