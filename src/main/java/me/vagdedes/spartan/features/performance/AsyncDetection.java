package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;

public class AsyncDetection {

    public static void run(SpartanPlayer player, Enums.HackType hackType, Runnable runnable) {
        if (!MultiVersion.folia
                && (hackType.getCheck().isSilent(player.getWorld().getName(), player.getUniqueId())
                || TPS.getMillisecondsPassed(player) <= 40L)) {
            SpartanBukkit.detectionThread.executeIfFreeElseHere(runnable);
        } else {
            // If there are less than 10 milliseconds available and the check is not silent,
            // we definitely run the detection on the main thread because we run into the
            // danger of moving the possible prevention in the next tick
            runnable.run();
        }
    }
}
