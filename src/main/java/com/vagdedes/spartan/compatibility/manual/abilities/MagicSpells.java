package com.vagdedes.spartan.compatibility.manual.abilities;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MagicSpells implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void SpellCast(SpellCastEvent e) {
        if (Compatibility.CompatibilityType.MAGIC_SPELLS.isFunctional()) {
            LivingEntity caster = e.getCaster();

            if (caster instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getProtocol((Player) caster).spartanPlayer;

                if (p != null) {
                    Config.compatibility.evadeFalsePositives(
                            p,
                            Compatibility.CompatibilityType.MAGIC_SPELLS,
                            new Enums.HackCategoryType[]{
                                    Enums.HackCategoryType.MOVEMENT,
                                    Enums.HackCategoryType.COMBAT
                            },
                            40
                    );
                }
            }
        }
    }

    @EventHandler
    private void SpellCasted(SpellCastedEvent e) {
        if (Compatibility.CompatibilityType.MAGIC_SPELLS.isFunctional()) {
            LivingEntity caster = e.getCaster();

            if (caster instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getProtocol((Player) caster).spartanPlayer;

                if (p != null) {
                    Config.compatibility.evadeFalsePositives(
                            p,
                            Compatibility.CompatibilityType.MAGIC_SPELLS,
                            new Enums.HackCategoryType[]{
                                    Enums.HackCategoryType.MOVEMENT,
                                    Enums.HackCategoryType.COMBAT
                            },
                            40
                    );
                }
            }
        }
    }

    /*@EventHandler
    private void Spell(SpellEvent e) {
    }*/
}
