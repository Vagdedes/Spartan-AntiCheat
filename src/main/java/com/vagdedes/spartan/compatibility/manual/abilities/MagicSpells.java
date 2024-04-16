package com.vagdedes.spartan.compatibility.manual.abilities;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.simple.CheckDelay;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MagicSpells implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void SpellCast(SpellCastEvent e) {
        if (Compatibility.CompatibilityType.MagicSpells.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getCaster().getUniqueId());

            if (p != null) {
                CheckDelay.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.MagicSpells, 40);
            }
        }
    }

    @EventHandler
    private void SpellCasted(SpellCastedEvent e) {
        if (Compatibility.CompatibilityType.MagicSpells.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getCaster().getUniqueId());

            if (p != null) {
                CheckDelay.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.MagicSpells, 40);
            }
        }
    }

    /*@EventHandler
    private void Spell(SpellEvent e) {
    }*/
}
