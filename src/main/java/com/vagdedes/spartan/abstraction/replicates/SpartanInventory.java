package com.vagdedes.spartan.abstraction.replicates;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SpartanInventory {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    public final ItemStack itemInHand;
    public final ItemStack itemInOffHand;

    SpartanInventory(ItemStack[] contents, ItemStack[] armor, ItemStack itemInHand, ItemStack itemOffHand) {
        this.contents = contents;
        this.armor = armor;

        if (itemInHand == null) {
            this.itemInHand = new ItemStack(Material.AIR);
        } else {
            this.itemInHand = itemInHand;
        }
        if (itemOffHand == null) {
            this.itemInOffHand = new ItemStack(Material.AIR);
        } else {
            this.itemInOffHand = itemOffHand;
        }
    }

    SpartanInventory() {
        this(new ItemStack[36], new ItemStack[4], new ItemStack(Material.AIR), new ItemStack(Material.AIR));
    }

    SpartanInventory(ItemStack[] contents) {
        this(contents, new ItemStack[4], new ItemStack(Material.AIR), new ItemStack(Material.AIR));
    }

    public boolean contains(Material material) {
        for (ItemStack itemStack : contents) {
            if (itemStack != null && itemStack.getType() == material) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getItem(int slot) {
        return contents[slot];
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmorContents() {
        return armor;
    }

    public int getSize() {
        return contents.length;
    }

    public ItemStack getHelmet() {
        return armor[0];
    }

    public ItemStack getChestplate() {
        return armor[1];
    }

    public ItemStack getLeggings() {
        return armor[2];
    }

    public ItemStack getBoots() {
        return armor[3];
    }
}
