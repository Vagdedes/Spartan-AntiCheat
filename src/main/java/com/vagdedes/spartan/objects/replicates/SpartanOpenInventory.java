package com.vagdedes.spartan.objects.replicates;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SpartanOpenInventory {

    private final ItemStack cursor;
    private final int slots;
    private final SpartanInventory topInventory, bottomInventory;

    SpartanOpenInventory(ItemStack cursor, int slots, ItemStack[] topInventoryContents, ItemStack[] bottomInventoryContents) {
        this.cursor = cursor;
        this.slots = slots;
        this.topInventory  = new SpartanInventory(topInventoryContents);
        this.bottomInventory  = new SpartanInventory(bottomInventoryContents);
    }

    SpartanOpenInventory() {
        this(new ItemStack(Material.AIR), 36, new ItemStack[]{}, new ItemStack[]{});
    }

    public ItemStack getCursor() {
        return cursor;
    }

    public int countSlots() {
        return slots;
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

    public SpartanInventory getTopInventory() {
        return topInventory;
    }

    public SpartanInventory getBottomInventory() {
        return bottomInventory;
    }
}
