package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;

public class CombatProcessing {

    public static boolean canCheck(SpartanPlayer player) {
        if (!player.movement.isLowEyeHeight() // Covers swimming & gliding
                && !player.movement.wasFlying()
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
        return !player.getInstance().getUniqueId().equals(entity.getUniqueId())
                && !MythicMobs.is(entity)
                && !ItemsAdder.is(entity);
    }

}
