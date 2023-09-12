package me.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class FishingHook {

    public static void run(SpartanPlayer p, SpartanPlayer t) {
        if (!p.equals(t)) {
            Damage.addCooldown(p, 20);
            p.getHandlers().add(Handlers.HandlerType.FishingHook, 40);
        }
    }

    public static boolean hasCooldown(SpartanPlayer p) {
        return p.getHandlers().has(Handlers.HandlerType.FishingHook)
                && p.getBlocksOffGround(Velocity.blocksOffGround + 1, true, true) <= Velocity.blocksOffGround;
    }
}
