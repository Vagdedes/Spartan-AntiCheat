package com.vagdedes.spartan.compatibility.manual.vanilla;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;

public class DragonPhases implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(EnderDragonChangePhaseEvent e) {
        if (Compatibility.CompatibilityType.DRAGON_PHASES.isFunctional()) {
            EnderDragon.Phase phase = e.getNewPhase();

            if (phase == EnderDragon.Phase.BREATH_ATTACK || phase == EnderDragon.Phase.ROAR_BEFORE_ATTACK || phase == EnderDragon.Phase.CHARGE_PLAYER
                    || phase == EnderDragon.Phase.FLY_TO_PORTAL || phase == EnderDragon.Phase.LAND_ON_PORTAL || phase == EnderDragon.Phase.LEAVE_PORTAL
                    || phase == EnderDragon.Phase.HOVER) {
                double distance = PlayerUtils.chunk / 2;

                for (Entity entity : e.getEntity().getNearbyEntities(distance, distance, distance)) {
                    if (entity instanceof Player) {
                        SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                        if (p != null) {
                            p.getTrackers().add(
                                    Trackers.TrackerType.ABSTRACT_VELOCITY,
                                    !p.isOnGround() ? 120 : 60
                            );
                        }
                    }
                }
            }
        }
    }
}
