package com.vagdedes.spartan.compatibility.manual.building;

import com.bgsoftware.wildtools.api.events.ToolUseEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class WildTools implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(ToolUseEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.WILD_TOOLS;

        if (compatibilityType.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    p,
                    compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.FastBreak,
                            Enums.HackType.BlockReach,
                            Enums.HackType.GhostHand,
                    },
                    40
            );
        }
    }
}
