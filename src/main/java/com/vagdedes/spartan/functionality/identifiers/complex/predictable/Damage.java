package com.vagdedes.spartan.functionality.identifiers.complex.predictable;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayerDamage;
import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.event.entity.EntityDamageEvent;

public class Damage { // Use for damage causes that still have an effect even after not being the last one

    public static boolean hasExplosion(SpartanPlayer player) {
        for (EntityDamageEvent.DamageCause cause : new EntityDamageEvent.DamageCause[]{
                EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
                EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        }) {
            if (player.getDamageReceived(cause).ticksPassed() <= TPS.tickTime) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBigKnockBack(SpartanPlayer player) {
        for (SpartanPlayerDamage damage : player.getReceivedDamages()) {
            if (damage.hasBigKnockBack()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSignificant(SpartanPlayer player) {
        for (SpartanPlayerDamage damage : player.getReceivedDamages()) {
            if (damage.isSignificant()) {
                return true;
            }
        }
        return false;
    }
}
