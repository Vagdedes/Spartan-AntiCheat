package com.vagdedes.spartan.compatibility.manual.abilities;

import be.anybody.api.advancedabilities.ability.event.AbilityCallEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.simple.CheckDelay;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AdvancedAbilities implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AbilityEvent(AbilityCallEvent e) {
        if (Compatibility.CompatibilityType.AdvancedAbilities.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            CheckDelay.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.AdvancedAbilities, 60);
        }
    }
}
