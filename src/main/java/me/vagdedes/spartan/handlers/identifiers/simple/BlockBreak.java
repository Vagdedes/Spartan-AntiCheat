package me.vagdedes.spartan.handlers.identifiers.simple;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;

public class BlockBreak {

    public static void run(SpartanPlayer p, boolean cancelled, SpartanBlock b) {
        if (BlockUtils.isSensitive(p, b.getType())) {
            p.getHandlers().add(Handlers.HandlerType.BlockBreak, cancelled ? 20 : 40);
        }
    }
}
