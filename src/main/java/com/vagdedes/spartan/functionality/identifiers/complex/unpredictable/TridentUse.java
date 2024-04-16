package com.vagdedes.spartan.functionality.identifiers.complex.unpredictable;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class TridentUse {

    private static final int riptideMaxSafeLevel = 3;

    public static void run(SpartanPlayer p) {
        for (ItemStack item : new ItemStack[]{p.getItemInHand(), p.getInventory().itemInOffHand}) {
            if (item.getType() == Material.TRIDENT) {
                int level = item.getEnchantmentLevel(Enchantment.RIPTIDE);

                if (level > 0) {
                    Handlers handlers = p.getHandlers();
                    int ticks = Math.min(level * 30, 150);

                    if (level > riptideMaxSafeLevel) {
                        p.getHandlers().add(Handlers.HandlerType.Velocity, ticks);
                    }
                    handlers.add(Handlers.HandlerType.Trident, ticks);
                    break;
                }
            }
        }
    }
}
