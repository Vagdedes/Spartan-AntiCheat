package com.vagdedes.spartan.handlers.tracking;

import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.objects.data.Decimals;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;

public class MovementProcessing {

    public static void run(SpartanPlayer p,
                           double dis, double hor, double ver, double box,
                           boolean crawling) {
        if (p.canRunChecks(false)) {
            // Damage
            Damage.runMove(p);

            // NMS Distance Caching
            p.setNmsDistance(dis, hor, ver, box);

            if (!crawling) {
                // Jump/Fall Identifier
                if (Math.abs(MoveUtils.jumping[1] - ver) < MoveUtils.getJumpingPrecision(p)) { // Last Jump
                    p.setLastJump();
                } else if (p.isFalling(ver)) { // Last Fall
                    p.setLastFall();
                }
            }

            // Extra Packets
            String key = "player-data=extra-packets";

            if (!p.isDetected(true)) {
                int maxTicks = 20;
                double difference = p.getCustomDistance() - dis;
                p.getDecimals().add(key, difference, maxTicks);

                if (p.getBuffer().increase(key, 1) >= maxTicks) {
                    p.getBuffer().remove(key);

                    if (p.getDecimals().get(key, Decimals.CALCULATE_AVERAGE) >= 0.01) {
                        p.setExtraPackets(p.getExtraPackets() + 1);
                    } else {
                        p.setExtraPackets(0);
                    }
                }
            } else { // Reset so it can again start from zero
                p.getBuffer().remove(key);
                p.getDecimals().remove(key);
            }
        }
    }
}
