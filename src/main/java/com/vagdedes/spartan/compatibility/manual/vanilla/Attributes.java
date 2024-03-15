package com.vagdedes.spartan.compatibility.manual.vanilla;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Attributes {

    private static final String key = "item-attributes=compatibility";
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

    public static boolean has(SpartanPlayer p, String attributeString) {
        if (Compatibility.CompatibilityType.ItemAttributes.isFunctional()) {
            try {
                Attribute attribute = Attribute.valueOf(attributeString);
                Player n = p.getPlayer();

                if (n != null) {
                    AttributeInstance instance = n.getAttribute(attribute);

                    if (instance != null && instance.getModifiers().size() > 0) {
                        int modifiers = 0;

                        for (AttributeModifier modifier :instance.getModifiers()) {
                            if (modifier.getSlot() != null) {
                                modifiers++;
                            }
                        }

                        if (modifiers > 0) {
                            p.getCooldowns().add(key, 60);
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return !p.getCooldowns().canDo(key);
        }
        return false;
    }
}
