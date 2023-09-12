package me.vagdedes.spartan.compatibility.manual.abilities;

import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityProgressEvent;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectKorra implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityStart(AbilityStartEvent e) {
        if (Compatibility.CompatibilityType.ProjectKorra.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getPlayer(e.getAbility().getPlayer()), 60);
        }
    }

    @EventHandler
    private void AbilityProgress(AbilityProgressEvent e) {
        if (Compatibility.CompatibilityType.ProjectKorra.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getPlayer(e.getAbility().getPlayer()), 40);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityDamageEvent(AbilityDamageEntityEvent e) {
        if (Compatibility.CompatibilityType.ProjectKorra.isFunctional()) {
            evadeCombatFPs(SpartanBukkit.getPlayer(e.getEntity().getUniqueId()), 60);
        }
    }

    private static void evadeCombatFPs(SpartanPlayer player, int ticks) {
        if (player != null) {
            Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.ProjectKorra;
            CheckProtection.evadeStandardCombatFPs(player, compatibilityType, ticks);
            CheckProtection.evadeCommonFalsePositives(player, compatibilityType,
                    new Enums.HackType[]{
                            Enums.HackType.Criticals,
                            Enums.HackType.NoSwing,
                    }, ticks);
        }
    }
}
