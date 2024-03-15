package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.UltimateStatistics;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TestServer;
import me.vagdedes.spartan.system.Enums;

import java.util.LinkedHashMap;
import java.util.Map;

public class CancelViolation {

    private static final String masterKey = "cancel-violation";
    private static final Map<Integer, Integer> memory = new LinkedHashMap<>(Enums.HackType.values().length * Enums.DataType.values().length);
    private static final int cooldown = 3;

    static void clear() {
        memory.clear();
    }

    static void clear(Enums.HackType hackType) {
        for (Enums.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(true)) {
            memory.remove(getStorageKey(hackType, dataType));
        }
    }

    static void refresh(Enums.HackType[] hackTypes) {
        for (Enums.HackType hackType : hackTypes) {
            Check check = hackType.getCheck();

            for (Enums.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(true)) {
                memory.put(
                        getStorageKey(hackType, dataType),
                        check.cancelViolation + (check.cancelViolation * check.getProblematicDetections())
                );
            }
        }
    }

    // Separator

    public static boolean isForced(SpartanPlayer player, Enums.HackType hackType) {
        return !player.getCooldowns().canDo(masterKey + hackType.ordinal())
                || UltimateStatistics.isSuspected(player.uuid);
    }

    public static void force(SpartanPlayer player, Enums.HackType hackType) {
        player.getCooldowns().add(masterKey + hackType.ordinal(), cooldown);
    }

    public static int get(Enums.HackType hackType, Enums.DataType dataType) {
        return TestServer.isIdentified() ? Check.minimumDefaultCancelViolation :
                memory.getOrDefault(getStorageKey(hackType, dataType), hackType.getCheck().cancelViolation);
    }

    // Separator

    private static int getStorageKey(Enums.HackType hackType, Enums.DataType dataType) {
        return (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + dataType.hashCode();
    }
}
