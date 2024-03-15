package com.vagdedes.spartan.compatibility.manual.damage;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class NoHitDelay {

    private static final int time = 40;

    private static int getTicks() {
        return Compatibility.CompatibilityType.OldCombatMechanics.isFunctional() ? 18 : 20;
    }

    public static void runDealDamage(SpartanPlayer p, Entity entity) {
        if (Compatibility.CompatibilityType.NoHitDelay.isFunctional() && entity instanceof Player) {
            SpartanPlayer t = SpartanBukkit.getPlayer((Player) entity);

            if (t != null && t.getMaximumNoDamageTicks() < getTicks()) {
                p.getCooldowns().add("no-hit-delay=compatibility", time);
                t.getCooldowns().add("no-hit-delay=compatibility", time);
            }
        }
    }

    public static void runVelocity(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.NoHitDelay.isFunctional() && p.getMaximumNoDamageTicks() < getTicks()) {
            p.getCooldowns().add("no-hit-delay=compatibility", time);
        }
    }

    public static boolean hasCooldown(SpartanPlayer p) {
        return Damage.getLastReceived(p) <= CombatUtils.combatTimeRequirement
                && (Compatibility.CompatibilityType.NoHitDelay.isFunctional() && !p.getCooldowns().canDo("no-hit-delay=compatibility")
                || Attributes.has(p, Attributes.GENERIC_ATTACK_SPEED)
                || KnockbackMaster.isUsing(p));
    }
}
