package com.vagdedes.spartan.utils.minecraft.inventory;

import org.bukkit.enchantments.Enchantment;

public class EnchantmentUtils {

    public static final Enchantment
            ARROW_KNOCKBACK,
            DURABILITY,
            WATER_WORKER,
            DIG_SPEED;

    static {
        Enchantment enchantment = findEnchantment("ARROW_KNOCKBACK");

        if (enchantment == null) {
            enchantment = Enchantment.PUNCH;
        }
        ARROW_KNOCKBACK = enchantment;

        enchantment = findEnchantment("DURABILITY");

        if (enchantment == null) {
            enchantment = Enchantment.UNBREAKING;
        }
        DURABILITY = enchantment;

        enchantment = findEnchantment("WATER_WORKER");

        if (enchantment == null) {
            enchantment = Enchantment.AQUA_AFFINITY;
        }
        WATER_WORKER = enchantment;

        enchantment = findEnchantment("DIG_SPEED");

        if (enchantment == null) {
            enchantment = Enchantment.EFFICIENCY;
        }
        DIG_SPEED = enchantment;
    }

    private static Enchantment findEnchantment(String string) {
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.getName().equalsIgnoreCase(string)) {
                return enchantment;
            }
        }
        return null;
    }
}
