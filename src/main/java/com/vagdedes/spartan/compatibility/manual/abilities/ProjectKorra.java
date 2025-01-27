package com.vagdedes.spartan.compatibility.manual.abilities;

import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityProgressEvent;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.PluginBase;
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
            evadeCombatFPs(PluginBase.getProtocol(e.getAbility().getPlayer()), 60);
        }
    }

    @EventHandler
    private void AbilityProgress(AbilityProgressEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            evadeCombatFPs(PluginBase.getProtocol(e.getAbility().getPlayer()), 40);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void AbilityDamageEvent(AbilityDamageEntityEvent e) {
        if (Compatibility.CompatibilityType.PROJECT_KORRA.isFunctional()) {
            Entity entity = e.getEntity();

            if (entity instanceof Player) {
                evadeCombatFPs(
                        PluginBase.getProtocol((Player) entity),
                        60
                );
            }
        }
    }

    private static void evadeCombatFPs(PlayerProtocol protocol, int ticks) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.PROJECT_KORRA;
        Config.compatibility.evadeFalsePositives(
                protocol,
                compatibilityType,
                new Enums.HackCategoryType[]{
                        Enums.HackCategoryType.MOVEMENT,
                        Enums.HackCategoryType.COMBAT
                },
                ticks
        );
        Config.compatibility.evadeFalsePositives(
                protocol,
                compatibilityType,
                Enums.HackType.NoSwing,
                ticks
        );
    }

}
