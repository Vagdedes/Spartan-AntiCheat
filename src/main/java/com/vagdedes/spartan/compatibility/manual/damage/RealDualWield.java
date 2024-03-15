package com.vagdedes.spartan.compatibility.manual.damage;

import com.evill4mer.RealDualWield.Api.PlayerOffhandAnimationEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.protections.CheckDelay;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RealDualWield implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerOffhandAnimationEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.RealDualWield;

        if (compatibilityType.isFunctional()) {
            CheckDelay.evadeCommonFalsePositives(p, compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.KillAura,
                            Enums.HackType.HitReach,
                            Enums.HackType.Criticals,
                    }, 5);
        }
    }
}
