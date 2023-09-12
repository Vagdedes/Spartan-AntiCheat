package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;

import java.util.List;
import java.util.UUID;

public class DetectionTick {

    private long futureTime, tick;

    public DetectionTick() {
        this.futureTime = 0L;
    }

    public boolean canSkip(UUID uuid, PlayerProfile profile, int ticksCooldown, SpartanPlayer player) {
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
            List<LiveViolation> liveViolations = Check.getViolationsObjects(uuid);

            if (liveViolations.size() > 0) {
                for (LiveViolation liveViolation : liveViolations) {
                    if (liveViolation.hasMaxCancelledLevel()
                            || liveViolation.getLastViolation(true) <= Check.recentViolationSeconds) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
