package me.vagdedes.spartan.compatibility.manual.damage;

import com.frash23.smashhit.AsyncPreDamageEvent;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.interfaces.listeners.EventsHandler6;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SmashHit implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void DealDamage(AsyncPreDamageEvent e) {
        if (Compatibility.CompatibilityType.SmashHit.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getDamager().getUniqueId());

            if (p == null) {
                return;
            }
            // Detections
            EventsHandler6.runDealDamage(e, p, e.getEntity(), e.getDamage(), EntityDamageEvent.DamageCause.ENTITY_ATTACK, e.isCancelled());

            if (p.getViolations(Enums.HackType.KillAura).process()
                    || p.getViolations(Enums.HackType.Criticals).process()
                    || p.getViolations(Enums.HackType.NoSwing).process()
                    || p.getViolations(Enums.HackType.FastClicks).process()
                    || p.getViolations(Enums.HackType.Velocity).process()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void ReceiveDamage(AsyncPreDamageEvent e) {
        if (Compatibility.CompatibilityType.SmashHit.isFunctional()) {
            Player damager = e.getDamager();
            Entity entity = e.getEntity();

            if (damager != null && entity != null) {
                boolean cancelled = e.isCancelled();
                EntityDamageEvent.DamageCause cause = EntityDamageEvent.DamageCause.ENTITY_ATTACK;

                // Protections
                if (Damage.runDealAndReceiveDamage(damager, entity, cause, cancelled)) {
                    e.setCancelled(true);
                } else {
                    EventsHandler6.runReceiveDamage(damager, entity, cause, cancelled);
                }
            }
        }
    }
}
