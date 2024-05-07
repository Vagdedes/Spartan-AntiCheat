package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayerDamage;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.event.entity.EntityDamageEvent;

public class CombatProcessing {

    public static boolean hasExplosionDamage(SpartanPlayer player) {
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

    public static boolean hasBigKnockBackDamage(SpartanPlayer player) {
        for (SpartanPlayerDamage damage : player.getReceivedDamages()) {
            if (damage.hasBigKnockBack()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSignificantDamage(SpartanPlayer player) {
        for (SpartanPlayerDamage damage : player.getReceivedDamages()) {
            if (damage.isSignificant()) {
                return true;
            }
        }
        return false;
    }

    // Separator

    public static boolean canCheck(SpartanPlayer player) {
        return !player.movement.isCrawling()
                && !player.movement.isFlying()
                && !player.getTrackers().has(Trackers.TrackerType.GAME_MODE)
                && !player.getTrackers().has(Trackers.TrackerType.ELYTRA_USE)
                && player.getVehicle() == null
                && !mcMMO.hasGeneralAbility(player)
                && !Attributes.has(player, Attributes.GENERIC_ARMOR);
    }
}
