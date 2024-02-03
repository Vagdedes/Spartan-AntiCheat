package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;

public class DetectionTick {

    private long futureTime, tick;

    public DetectionTick() {
        this.futureTime = 0L;
    }

    public boolean canSkip(PlayerProfile profile, int ticksCooldown, SpartanPlayer player) {
        boolean canSkip;
        long currentTime = System.currentTimeMillis();

        if (currentTime > futureTime) {
            this.futureTime = currentTime + (ticksCooldown * TPS.tickTime);
            this.tick = TPS.getTick(player);
            canSkip = true;
        } else {
            canSkip = this.tick == TPS.getTick(player);
        }

        if (canSkip
                && !MaximumCheckedPlayers.isActive()
                && !profile.isSuspectedOrHacker()
                && !player.hasNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance)) {
            for (LiveViolation liveViolation : player.getViolations()) {
                if (liveViolation.hasMaxCancelledLevel()
                        || liveViolation.getLastViolationTime(true) <= Check.recentViolationSeconds) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
