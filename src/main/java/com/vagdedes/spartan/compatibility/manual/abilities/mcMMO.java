package com.vagdedes.spartan.compatibility.manual.abilities;

import com.gmail.nossr50.api.AbilityAPI;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class mcMMO {

    public static boolean hasGeneralAbility(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
            Player n = p.getInstance();
            return n != null && AbilityAPI.isAnyAbilityEnabled(n)

                    || PlayerUtils.isAxeItem(p.getItemInHand().getType())
                    && !p.getNearbyEntities(
                    CombatUtils.maxHitDistance,
                    CombatUtils.maxHitDistance,
                    CombatUtils.maxHitDistance).isEmpty()

                    || hasTamingAbility(p);
        } else {
            return false;
        }
    }

    public static boolean hasTreeFeller(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.MC_MMO.isFunctional()) {
            Player n = p.getInstance();
            return n != null && AbilityAPI.treeFellerEnabled(n);
        } else {
            return false;
        }
    }

    private static boolean hasTamingAbility(SpartanPlayer p) {
        for (Entity entity : p.getNearbyEntities(PlayerUtils.chunk, PlayerUtils.chunk, PlayerUtils.chunk)) {
            if (entity instanceof Wolf) {
                AnimalTamer owner = ((Wolf) entity).getOwner();

                if (owner != null && owner.getUniqueId().equals(p.uuid)) {
                    return true;
                }
            }
        }
        return false;
    }
}
