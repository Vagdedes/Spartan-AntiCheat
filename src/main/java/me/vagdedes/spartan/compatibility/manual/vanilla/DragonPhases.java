package me.vagdedes.spartan.compatibility.manual.vanilla;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
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
        if (Compatibility.CompatibilityType.DragonPhases.isFunctional()) {
            EnderDragon.Phase phase = e.getNewPhase();

            if (phase == EnderDragon.Phase.BREATH_ATTACK || phase == EnderDragon.Phase.ROAR_BEFORE_ATTACK || phase == EnderDragon.Phase.CHARGE_PLAYER
                    || phase == EnderDragon.Phase.FLY_TO_PORTAL || phase == EnderDragon.Phase.LAND_ON_PORTAL || phase == EnderDragon.Phase.LEAVE_PORTAL
                    || phase == EnderDragon.Phase.HOVER) {
                double distance = MoveUtils.chunk / 2;

                for (Entity entity : e.getEntity().getNearbyEntities(distance, distance, distance)) {
                    if (entity instanceof Player) {
                        SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                        if (p !=null) {
                            Damage.extremeDamageHandling(p);
                        }
                    }
                }
            }
        }
    }
}
