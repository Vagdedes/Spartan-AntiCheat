package com.vagdedes.spartan.compatibility.manual.abilities;

import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityProgressEvent;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CheckDelay;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectKorra implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityStart(AbilityStartEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getPlayer(e.getAbility().getPlayer()), 60);
        }
    }

    @EventHandler
    private void AbilityProgress(AbilityProgressEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getPlayer(e.getAbility().getPlayer()), 40);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityDamageEvent(AbilityDamageEntityEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getPlayer(e.getEntity().getUniqueId()), 60);
        }
    }

    private static void evadeCombatFPs(SpartanPlayer player, int ticks) {
        if (player != null) {
            Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.PROJECT_KORRA;
            CheckDelay.evadeStandardCombatFPs(player, compatibilityType, ticks);
            CheckDelay.evadeCommonFalsePositives(player, compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.Criticals,
                            Enums.HackType.NoSwing,
                    }, ticks);
        }
    }
}
