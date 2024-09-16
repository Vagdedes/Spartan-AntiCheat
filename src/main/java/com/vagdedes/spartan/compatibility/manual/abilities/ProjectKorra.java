package com.vagdedes.spartan.compatibility.manual.abilities;

import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityProgressEvent;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectKorra implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityStart(AbilityStartEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getProtocol(e.getAbility().getPlayer()).spartanPlayer, 60);
        }
    }

    @EventHandler
    private void AbilityProgress(AbilityProgressEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getProtocol(e.getAbility().getPlayer()).spartanPlayer, 40);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityDamageEvent(AbilityDamageEntityEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            Entity entity = e.getEntity();

            if (entity instanceof Player) {
                evadeCombatFPs(
                        SpartanBukkit.getProtocol((Player) entity).spartanPlayer,
                        60
                );
            }
        }
    }

    private static void evadeCombatFPs(SpartanPlayer player, int ticks) {
        if (player != null) {
            Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.PROJECT_KORRA;
            Config.compatibility.evadeFalsePositives(
                    player,
                    compatibilityType,
                    new Enums.HackCategoryType[]{
                            Enums.HackCategoryType.MOVEMENT,
                            Enums.HackCategoryType.COMBAT
                    },
                    ticks
            );
            Config.compatibility.evadeFalsePositives(
                    player,
                    compatibilityType,
                    Enums.HackType.NoSwing,
                    ticks
            );
        }
    }

}
