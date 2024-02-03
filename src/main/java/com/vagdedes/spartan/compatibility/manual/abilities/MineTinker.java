package com.vagdedes.spartan.compatibility.manual.abilities;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import de.flo56958.minetinker.events.MTPlayerInteractEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MineTinker implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Enter(MTPlayerInteractEvent e) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.MineTinker;

        if (compatibilityType.isFunctional()) {
            CheckProtection.evadeCommonFalsePositives(e.getPlayer(), compatibilityType,
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
