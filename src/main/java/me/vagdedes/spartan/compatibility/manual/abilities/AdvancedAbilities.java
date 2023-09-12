package me.vagdedes.spartan.compatibility.manual.abilities;

import be.anybody.api.advancedabilities.ability.event.AbilityCallEvent;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AdvancedAbilities implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void AbilityEvent(AbilityCallEvent e) {
        if (Compatibility.CompatibilityType.AdvancedAbilities.isFunctional()) {
            CheckProtection.evadeStandardCombatFPs(e.getPlayer(), Compatibility.CompatibilityType.AdvancedAbilities, 60);
        }
    }
}
