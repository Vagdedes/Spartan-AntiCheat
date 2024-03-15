package com.vagdedes.spartan.compatibility.manual.enchants;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanInventory;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import me.darrionat.CustomEnchantsAPI;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class CustomEnchantsPlus {

    public static boolean has(SpartanPlayer player) {
        if (Compatibility.CompatibilityType.CustomEnchantsPlus.isFunctional()) {
            SpartanInventory inventory = player.getInventory();

            for (ItemStack armor : inventory.getArmorContents()) {
                if (armor != null && has(armor)) {
                    return true;
                }
            }
            return has(inventory.itemInHand) || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && has(inventory.itemInOffHand);
        }
        return false;
    }

    private static boolean has(ItemStack item) {
        if (item.hasItemMeta()) {
            for (Enchantment enchantment : item.getItemMeta().getEnchants().keySet()) {
                if (CustomEnchantsAPI.isCustomEnchantment(enchantment)) {
                    return true;
                }
            }
        }
        return false;
    }
}
