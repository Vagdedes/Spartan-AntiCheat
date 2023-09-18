package me.vagdedes.spartan.compatibility.manual.enchants;

import com.willfp.ecoenchants.enchants.EcoEnchant;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanInventory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class EcoEnchants {

    public static boolean has(SpartanPlayer player) {
        if (Compatibility.CompatibilityType.EcoEnchants.isFunctional()) {
            SpartanInventory inventory = player.getInventory();

            for (ItemStack armor : inventory.getArmorContents()) {
                if (armor != null && has(armor)) {
                    return true;
                }
            }
            return has(inventory.getItemInHand()) || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && has(inventory.getItemInOffHand());
        }
        return false;
    }

    private static boolean has(ItemStack item) {
        if (item.hasItemMeta()) {
            for (Enchantment enchantment : item.getItemMeta().getEnchants().keySet()) {
                if (enchantment instanceof EcoEnchant) {
                    return true;
                }
            }
        }
        return false;
    }
}
