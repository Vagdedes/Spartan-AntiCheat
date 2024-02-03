package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.compatibility.necessary.UltimateStatistics;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.LinkedHashMap;
import java.util.Map;

public class CancelViolation {

    private static final String masterKey = "cancel-violation";
    private static final Map<Integer, Integer> memory = new LinkedHashMap<>(Enums.HackType.values().length * ResearchEngine.DataType.values().length);
    private static final int cooldown = 3;

    static void clear() {
        memory.clear();
    }

    static void clear(Enums.HackType hackType) {
        for (ResearchEngine.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(true)) {
            memory.remove(ResearchEngine.getStorageKey(hackType, dataType));
        }
    }

    static void refresh(Enums.HackType[] hackTypes) {
        for (Enums.HackType hackType : hackTypes) {
            Check check = hackType.getCheck();

            for (ResearchEngine.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(true)) {
                int key = ResearchEngine.getStorageKey(hackType, dataType);

                if (check.hasCancelViolation()) {
                    memory.put(key, check.getCancelViolation());
                } else {
                    int defaultCancelViolation = check.getDefaultCancelViolation();
                    memory.put(key, defaultCancelViolation + (defaultCancelViolation * check.getProblematicDetections()));
                }
            }
        }
    }

    // Separator

    public static boolean isForced(SpartanPlayer player, Enums.HackType hackType,
                                   LiveViolation liveViolation, int hash) {
        return liveViolation.hasMaxCancelledLevel(hash)
                || !player.getCooldowns().canDo(masterKey + hackType.ordinal())
                || UltimateStatistics.isSuspected(player.getUniqueId());
    }

    public static void force(SpartanPlayer player, Enums.HackType hackType) {
        Check check = hackType.getCheck();

        if (check.isUsingCancelViolation(
                get(hackType, player.getDataType()),
                AlgebraUtils.integerRound(check.getDefaultCancelViolation() / 2.0)
        )) {
            player.getCooldowns().add(masterKey + hackType.ordinal(), cooldown);
        }
    }

    public static int get(Enums.HackType hackType, ResearchEngine.DataType dataType) {
        return TestServer.isIdentified() ? Check.minimumDefaultCancelViolation :
                memory.getOrDefault(ResearchEngine.getStorageKey(hackType, dataType), hackType.getCheck().getDefaultCancelViolation());
    }
}
