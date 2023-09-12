package me.vagdedes.spartan.handlers.identifiers.simple;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class GameModeProtection {

    public static void run(SpartanPlayer p) {
        if (!p.isOnGround() || !p.isOnGroundCustom() || p.getCustomDistance() > 0.0 || p.getNmsDistance() > 0.0) {
            p.getHandlers().add(Handlers.HandlerType.GameMode, 60);
        }
    }
}
