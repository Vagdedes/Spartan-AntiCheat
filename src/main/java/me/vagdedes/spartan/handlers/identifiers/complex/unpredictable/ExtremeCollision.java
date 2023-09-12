package me.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

public class ExtremeCollision {

    public static void run(SpartanPlayer player) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            Handlers handlers = player.getHandlers();

            if (!handlers.isDisabled(Handlers.HandlerType.ExtremeCollision)) {
                handlers.disable(Handlers.HandlerType.ExtremeCollision, 2); // Disable for the next tick

                if (PlayerData.getNearbyCollisions(player) >= 0.9) {
                    handlers.add(Handlers.HandlerType.ExtremeCollision, 40);
                }
            }
        }
    }
}
