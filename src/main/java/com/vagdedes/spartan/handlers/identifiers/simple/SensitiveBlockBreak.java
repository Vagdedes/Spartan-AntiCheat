package com.vagdedes.spartan.handlers.identifiers.simple;

import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;

public class SensitiveBlockBreak {

    public static void run(SpartanPlayer p, boolean cancelled, SpartanBlock b) {
        if (BlockUtils.isSensitive(p, b.getType())) {
            p.getHandlers().add(Handlers.HandlerType.SensitiveBlockBreak, cancelled ? 20 : 40);
        }
    }
}
