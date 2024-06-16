package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayerDamage;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
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
        if (!player.movement.isCrawling()
                && !player.movement.isFlying()
                && !player.trackers.has(Trackers.TrackerType.FLYING)
                && !player.movement.isGliding()
                && player.getInstance().getVehicle() == null
                && !mcMMO.hasGeneralAbility(player)
                && Attributes.getAmount(player, Attributes.GENERIC_ARMOR) == 0.0) {
            GameMode gameMode = player.getInstance().getGameMode();
            return gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE
                    || gameMode == GameMode.CREATIVE;
        } else {
            return false;
        }
    }

    public static boolean canCheck(SpartanPlayer player, LivingEntity entity) {
        return !player.uuid.equals(entity.getUniqueId())
                && !MythicMobs.is(entity)
                && !ItemsAdder.is(entity);
    }

}
