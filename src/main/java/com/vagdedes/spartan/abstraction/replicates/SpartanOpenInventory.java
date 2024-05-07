package com.vagdedes.spartan.abstraction.replicates;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SpartanOpenInventory {

    public final ItemStack cursor;
    public final int slots;
    public final SpartanInventory topInventory, bottomInventory;

    SpartanOpenInventory(ItemStack cursor, int slots, ItemStack[] topInventoryContents, ItemStack[] bottomInventoryContents) {
        this.cursor = cursor;
        this.slots = slots;
        this.topInventory  = new SpartanInventory(topInventoryContents);
        this.bottomInventory  = new SpartanInventory(bottomInventoryContents);
    }

    SpartanOpenInventory() {
        this(new ItemStack(Material.AIR), 36, new ItemStack[]{}, new ItemStack[]{});
    }

    public boolean contains(Material material) {
        for (SpartanInventory inventory : new SpartanInventory[]{topInventory, bottomInventory}) {
            for (ItemStack itemStack : inventory.getContents()) {
                if (itemStack != null && itemStack.getType() == material) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUsingTheCursor() {
        return this.cursor.getType() != Material.AIR;
    }

    public boolean isUsingAnotherInventory() {
        return this.slots > 46;
    }
}
