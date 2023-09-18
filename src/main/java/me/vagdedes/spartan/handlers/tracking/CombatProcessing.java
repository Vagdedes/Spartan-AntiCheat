package me.vagdedes.spartan.handlers.tracking;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatProcessing {

    private static final int pastTicks = 5;
    private static final String
            baseKey = "combat=",
            cooldownKey = "=cooldown";
    private static final Map<UUID, Vector[]> directionalComparison = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));    public static final String
            yaw = "yaw",
            pitch = "pitch",
            yawDifference = yaw + "-difference",
            pitchDifference = pitch + "-difference";

    public static void remove(SpartanPlayer player) {
        directionalComparison.remove(player.getUniqueId());
    }    private static final String[]
            teleportationList = new String[]{yaw, pitch, yawDifference, pitchDifference};

    public static void runTeleport(SpartanPlayer p) {
        p.getDecimals().remove(teleportationList);
        remove(p);
    }

    public static void runMove(SpartanPlayer p, SpartanLocation to,
                               Cooldowns cooldowns, Decimals decimals) {
        if (p.canRunChecks(false)) {
            UUID uuid = p.getUniqueId();
            Vector[] directions = directionalComparison.get(uuid);
            float yawVar = to.getYaw(), pitchVar = to.getPitch();

            if (directions != null) {
                Vector yawDirection = CombatUtils.getDirection(yawVar, 0.0f);
                decimals.add(baseKey + yawDifference, Math.toDegrees(yawDirection.distance(directions[0])), pastTicks);
                cooldowns.add(baseKey + yawDifference + cooldownKey, pastTicks);
                Vector pitchDirection = CombatUtils.getDirection(0.0f, pitchVar);
                decimals.add(baseKey + pitchDifference, Math.toDegrees(pitchDirection.distance(directions[1])), pastTicks);
                cooldowns.add(baseKey + pitchDifference + cooldownKey, pastTicks);
                directionalComparison.put(uuid, new Vector[]{yawDirection, pitchDirection});
            } else {
                directionalComparison.put(uuid, new Vector[]{
                        CombatUtils.getDirection(yawVar, 0.0f),
                        CombatUtils.getDirection(0.0f, pitchVar)
                });
            }
            decimals.add(baseKey + yaw, yawVar, pastTicks);
            cooldowns.add(baseKey + yaw + cooldownKey, pastTicks);
            decimals.add(baseKey + pitch, pitchVar, pastTicks);
            cooldowns.add(baseKey + pitch + cooldownKey, pastTicks);
        }
    }

    // Methods

    public static double getDecimal(SpartanPlayer p, String s, double fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_MAX) :
                fallback;
    }

    public static int getInteger(SpartanPlayer p, String s, int fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                (int) p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_MAX) :
                fallback;
    }

    // Singles

    public static long getLong(SpartanPlayer p, String s, long fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                (long) p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_MAX) :
                fallback;
    }

    public static double getDecimalAverage(SpartanPlayer p, String s, double fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_AVERAGE) :
                fallback;
    }

    public static int getIntegerAverage(SpartanPlayer p, String s, int fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                (int) p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_AVERAGE) :
                fallback;
    }

    // Averages

    public static long getLongAverage(SpartanPlayer p, String s, long fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                (long) p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_AVERAGE) :
                fallback;
    }

    public static List<Double> getAllValues(SpartanPlayer p, String s) {
        return p.getDecimals().getOldestToNewestList(baseKey + s);
    }



    // List



}
