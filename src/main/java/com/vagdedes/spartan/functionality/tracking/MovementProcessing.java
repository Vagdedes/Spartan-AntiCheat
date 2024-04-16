package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class MovementProcessing {

    public static void run(SpartanPlayer p,
                           double dis, double hor, double ver, double box,
                           float fall) {
        // NMS Distance Caching
        p.movement.setNmsDistance(dis, hor, ver, box, fall);

        // Jump/Fall Identifier
        if (Math.abs(PlayerUtils.jumping[1] - ver) < PlayerUtils.getJumpingPrecision(p)) { // Last Jump
            p.movement.setLastJump();
        } else if (p.movement.isFalling(ver)) { // Last Fall
            p.movement.setLastFall();
        }

        // Extra Packets
        String key = "player-data=extra-packets";
        double difference = p.movement.getCustomDistance() - dis;
        p.getDecimals().add(key, difference, AlgebraUtils.integerRound(TPS.maximum));

        if (p.getBuffer().increase(key, 1) >= TPS.maximum) {
            p.getBuffer().remove(key);

            if (p.getDecimals().get(key, 0.0, Decimals.CALCULATE_AVERAGE) >= 0.01) {
                p.movement.setExtraPackets(p.movement.getExtraPackets() + 1);
            } else {
                p.movement.setExtraPackets(0);
            }
        }
    }
}
