package me.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class TridentUse {

    private static final int riptideMaxSafeLevel = 3;

    public static void run(SpartanPlayer p) {
        for (ItemStack item : new ItemStack[]{p.getItemInHand(), p.getInventory().getItemInOffHand()}) {
            if (item.getType() == Material.TRIDENT) {
                int level = item.getEnchantmentLevel(Enchantment.RIPTIDE);

                if (level > 0) {
                    Handlers handlers = p.getHandlers();
                    int ticks = Math.min(level * 30, 150);

                    if (level > riptideMaxSafeLevel) {
                        Velocity.addCooldown(p, -ticks);
                    }
                    handlers.add(Handlers.HandlerType.Trident, ticks);
                    break;
                }
            }
        }
    }
}
