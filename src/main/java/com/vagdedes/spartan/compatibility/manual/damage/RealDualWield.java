package com.vagdedes.spartan.compatibility.manual.damage;

import com.evill4mer.RealDualWield.Api.PlayerOffhandAnimationEvent;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RealDualWield implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerOffhandAnimationEvent e) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.RealDualWield;

        if (compatibilityType.isFunctional()) {
            CheckProtection.evadeCommonFalsePositives(e.getPlayer(), compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.KillAura,
                            Enums.HackType.HitReach,
                            Enums.HackType.Criticals,
                    }, 5);
        }
    }
}
