package com.vagdedes.spartan.functionality.identifiers.simple;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;

public class SensitiveBlockBreak {

    public static void run(SpartanPlayer p, boolean cancelled, SpartanBlock b) {
        if (BlockUtils.isSensitive(p, b.material)) {
            p.getHandlers().add(Handlers.HandlerType.SensitiveBlockBreak, cancelled ? 20 : 40);
        }
    }
}
