package com.vagdedes.spartan.utils.minecraft.server.inventory;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {

    public static void prepareDescription(List<String> array, String title) {
        array.clear();

        if (!title.isEmpty()) {
            array.add("§e" + title);
            array.add("");
        }
    }

    public static void add(Inventory inv, String name, List<String> lore, ItemStack item, int slot) {
        ItemMeta am = item.getItemMeta();
        am.setDisplayName(name);

        if (lore != null) {
            am.setLore(lore);
        }
        item.setItemMeta(am);

        if (slot != -1) {
            inv.setItem(slot, item);
        } else {
            inv.addItem(item);
        }
    }

    public static ItemStack get(String name, ArrayList<String> lore, ItemStack item) {
        ItemMeta am = item.getItemMeta();
        am.setDisplayName(name);

        if (lore != null) {
            am.setLore(lore);
        }
        item.setItemMeta(am);
        return item;
    }

    public static ItemStack getHead() {
        return new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                ? Material.PLAYER_HEAD
                : Material.getMaterial("SKULL_ITEM"), 1, (short) SkullType.PLAYER.ordinal());
    }

    public static ItemStack getSkull(OfflinePlayer offlinePlayer, String backupName, boolean create) {
        return BackgroundInventoryUtils.getSkull(offlinePlayer, backupName, create);
    }

}
