package com.vagdedes.spartan.compatibility.manual.vanilla;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;

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
            Attribute attribute = Attribute.valueOf(attributeString);

            if (attribute != null) {
                PlayerInventory inventory = p.getInventory();

                if (inventory != null) {
                    int modifiersCount = 0;
                    double amount = 0.0;

                    for (ItemStack itemStack : new ItemStack[]{
                            inventory.getHelmet(),
                            inventory.getChestplate(),
                            inventory.getLeggings(),
                            inventory.getBoots(),
                            inventory.getItemInHand(),
                            MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? inventory.getItemInOffHand() : null
                    }) {
                        if (itemStack != null && itemStack.hasItemMeta()) {
                            ItemMeta meta = itemStack.getItemMeta();

                            if (meta != null && meta.hasAttributeModifiers()) {
                                Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);

                                if (modifiers != null && !modifiers.isEmpty()) {
                                    for (AttributeModifier modifier : modifiers) {
                                        modifiersCount++;
                                        amount = Math.max(amount, modifier.getAmount());
                                    }
                                }
                            }
                        }
                    }

                    if (modifiersCount > 0) {
                        return amount;
                    }
                }
            }
        }
        return 0.0;
    }
}
