package com.vagdedes.spartan.compatibility.manual.vanilla;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Attributes {

    public static final boolean classExists =
            ReflectionUtils.classExists(
                    "org.bukkit.attribute.Attribute"
            ) && ReflectionUtils.classExists(
                    "org.bukkit.attribute.AttributeInstance"
            ) && ReflectionUtils.classExists(
                    "org.bukkit.attribute.AttributeModifier"
            );
    public static final String
            GENERIC_MAX_HEALTH = "GENERIC_MAX_HEALTH",
            GENERIC_FOLLOW_RANGE = "GENERIC_FOLLOW_RANGE",
            GENERIC_KNOCKBACK_RESISTANCE = "GENERIC_KNOCKBACK_RESISTANCE",
            GENERIC_MOVEMENT_SPEED = "GENERIC_MOVEMENT_SPEED",
            GENERIC_FLYING_SPEED = "GENERIC_FLYING_SPEED",
            GENERIC_ATTACK_DAMAGE = "GENERIC_ATTACK_DAMAGE",
            GENERIC_ATTACK_KNOCKBACK = "GENERIC_ATTACK_KNOCKBACK",
            GENERIC_ATTACK_SPEED = "GENERIC_ATTACK_SPEED",
            GENERIC_ARMOR = "GENERIC_ARMOR",
            GENERIC_ARMOR_TOUGHNESS = "GENERIC_ARMOR_TOUGHNESS",
            GENERIC_LUCK = "GENERIC_LUCK",
            HORSE_JUMP_STRENGTH = "HORSE_JUMP_STRENGTH",
            ZOMBIE_SPAWN_REINFORCEMENTS = "ZOMBIE_SPAWN_REINFORCEMENTS";

    public static double getAmount(SpartanPlayer p, String attributeString) {
        if (classExists && Compatibility.CompatibilityType.ITEM_ATTRIBUTES.isFunctional()) {
            Player n = p.getInstance();

            if (n != null) {
                AttributeInstance instance = n.getAttribute(Attribute.valueOf(attributeString));

                if (instance != null && !instance.getModifiers().isEmpty()) {
                    int modifiers = 0;
                    double amount = 0.0;

                    for (AttributeModifier modifier : instance.getModifiers()) {
                        if (modifier.getSlot() != null) {
                            modifiers++;
                            amount = Math.max(amount, modifier.getAmount());
                        }
                    }

                    if (modifiers > 0) {
                        return amount;
                    }
                }
            }
        }
        return 0.0;
    }
}
