package me.vagdedes.spartan.handlers.identifiers.simple;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class BlockPlace {

    public static void runPlace(SpartanPlayer p, boolean cancelled) {
        if (p.getTimer().get("block-place=repeat") <= 205L) {
            p.getHandlers().add(Handlers.HandlerType.BlockPlace, cancelled ? 20 : 40);
        }
        p.getTimer().set("block-place=repeat");
    }
}
