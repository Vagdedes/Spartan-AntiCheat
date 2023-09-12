package me.vagdedes.spartan.compatibility.manual.abilities;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomMob;
import dev.lone.itemsadder.api.CustomStack;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanInventory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class ItemsAdder {

    private static final boolean classExists = ReflectionUtils.classExists("me.libraryaddict.disguise.DisguiseAPI");

    public static boolean is(SpartanPlayer player) {
        if (classExists && Compatibility.CompatibilityType.ItemsAdder.isFunctional()) {
            SpartanInventory inventory = player.getInventory();

            for (ItemStack armor : inventory.getArmorContents()) {
                if (armor != null && is(armor)) {
                    return true;
                }
            }
            return is(inventory.getItemInHand()) || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && is(inventory.getItemInOffHand());
        }
        return false;
    }

    public static boolean is(Block block) {
        return classExists && Compatibility.CompatibilityType.ItemsAdder.isFunctional()
                && CustomBlock.byAlreadyPlaced(block) != null;
    }

    private static boolean is(ItemStack itemStack) {
        return classExists && Compatibility.CompatibilityType.ItemsAdder.isFunctional()
                && CustomStack.getInstance(itemStack.getType().toString()) != null;
    }

    public static boolean is(Entity entity) {
        return classExists && Compatibility.CompatibilityType.ItemsAdder.isFunctional()
                && CustomMob.byAlreadySpawned(entity) != null;
    }
}

