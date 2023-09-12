package me.vagdedes.spartan.compatibility.manual.damage;

import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
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
