package com.vagdedes.spartan.compatibility.manual.abilities.crackshot;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CheckDelay;
import me.DeeCaaD.CrackShotPlus.Events.WeaponSecondScopeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class CrackShotPlus implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WeaponScope(WeaponSecondScopeEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT_PLUS.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            if (!e.isCancelled()) {
                Buffer buffer = p.getBuffer();
                CheckDelay.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.CRACK_SHOT_PLUS, 20);

                if (e.isZoomIn()) {
                    buffer.set("crackshotplus=compatibility=scope", 1);
                } else {
                    buffer.remove("crackshotplus=compatibility=scope");
                }
            } else {
                p.getBuffer().remove("crackshotplus=compatibility=scope");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntityDamage(EntityDamageEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT_PLUS.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getEntity().getUniqueId());

            if (p != null && isUsingScope(p)) {
                CheckDelay.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.CRACK_SHOT_PLUS, 60);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void EntityByEntityDamage(EntityDamageByEntityEvent e) {
        if (Compatibility.CompatibilityType.CRACK_SHOT_PLUS.isFunctional()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getDamager().getUniqueId());

            if (p != null && isUsingScope(p)) {
                CheckDelay.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.CRACK_SHOT_PLUS, 30);
            }
        }
    }

    static boolean isUsingScope(SpartanPlayer p) {
        return Compatibility.CompatibilityType.CRACK_SHOT_PLUS.isFunctional()
                && p.getBuffer().get("crackshotplus=compatibility=scope") != 0;
    }
}

