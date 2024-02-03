package com.vagdedes.spartan.compatibility.manual.damage;

import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
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
        return Damage.getLastReceived(p) <= PlayerData.combatTimeRequirement
                && (Compatibility.CompatibilityType.NoHitDelay.isFunctional() && !p.getCooldowns().canDo("no-hit-delay=compatibility")
                || Attributes.has(p, Attributes.GENERIC_ATTACK_SPEED)
                || KnockbackMaster.isUsing(p));
    }
}
