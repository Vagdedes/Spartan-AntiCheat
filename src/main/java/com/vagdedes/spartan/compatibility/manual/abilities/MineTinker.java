package com.vagdedes.spartan.compatibility.manual.abilities;

import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import de.flo56958.minetinker.events.MTPlayerInteractEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MineTinker implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Enter(MTPlayerInteractEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartan;
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.MINE_TINKER;

        if (compatibilityType.isFunctional()) {
            Config.compatibility.evadeFalsePositives(
                    p,
                    compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.KillAura,
                            Enums.HackType.FastClicks,
                            Enums.HackType.HitReach,
                            Enums.HackType.FastPlace
                    },
                    40
            );
        }
    }
}
