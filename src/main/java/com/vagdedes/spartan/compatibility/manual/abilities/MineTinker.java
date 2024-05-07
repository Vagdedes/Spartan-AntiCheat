package com.vagdedes.spartan.compatibility.manual.abilities;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CheckDelay;
import de.flo56958.minetinker.events.MTPlayerInteractEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MineTinker implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Enter(MTPlayerInteractEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.MINE_TINKER;

        if (compatibilityType.isFunctional()) {
            CheckDelay.evadeCommonFalsePositives(p, compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.KillAura,
                            Enums.HackType.FastClicks,
                            Enums.HackType.HitReach,
                            Enums.HackType.FastPlace,
                            Enums.HackType.Speed
                    }, 40);
        }
    }
}
