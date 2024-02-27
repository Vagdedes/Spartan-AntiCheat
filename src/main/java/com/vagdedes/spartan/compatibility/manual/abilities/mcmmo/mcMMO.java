package com.vagdedes.spartan.compatibility.manual.abilities.mcmmo;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;

import java.util.UUID;

public class mcMMO {

    public static boolean hasAbility(SpartanPlayer p) {
        return Compatibility.CompatibilityType.mcMMO.isFunctional() && BackgroundMcMMO.hasAbility(p)
                || hasAxeAbility(p)
                || hasHandAbility(p)
                || hasTamingAbility(p);
    }

    private static boolean hasAxeAbility(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.mcMMO.isFunctional()) {
            Material m = p.getItemInHand().getType();
            return PlayerData.isAxeItem(m)
                    && p.getEnemiesNumber(CombatUtils.maxHitDistance, true) > 0;
        }
        return false;
    }

    private static boolean hasHandAbility(SpartanPlayer p) {
        return Compatibility.CompatibilityType.mcMMO.isFunctional() && p.getItemInHand() == null;
    }

    public static boolean hasTreeFeller(SpartanPlayer p) {
        return Compatibility.CompatibilityType.mcMMO.isFunctional() && BackgroundMcMMO.hasTreeFeller(p);
    }

    private static boolean hasTamingAbility(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.mcMMO.isFunctional()) {
            UUID uuid = p.uuid;

            for (Entity entity : p.getNearbyEntities(MoveUtils.chunk, MoveUtils.chunk, MoveUtils.chunk)) {
                if (entity instanceof Wolf) {
                    AnimalTamer owner = ((Wolf) entity).getOwner();

                    if (owner != null && owner.getUniqueId().equals(uuid)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
