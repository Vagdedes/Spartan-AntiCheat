package me.vagdedes.spartan.utils.server;

import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {

    public enum ArmorState {
        Full, Semi, Empty
    }

    public static void prepareDescription(List<String> array, String title) {
        array.clear();

        if (title.length() > 0) {
            array.add("§e" + title);
            array.add("");
        }
    }

    public static int createHashCode(ItemStack itemStack) {
        int hash = 1;
        hash = hash * 31 + itemStack.getType().hashCode();
        hash = hash * 31 + itemStack.getAmount();
        //hash = hash * 31 + (itemStack.getDurability() & '\uffff');
        hash = hash * 31 + (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ? itemStack.getItemMeta().getDisplayName().hashCode() : 0);
        return hash;
    }

    public static ArmorState getArmorState(SpartanPlayer p) {
        int counter = 0;

        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item != null) {
                counter++;
            }
        }
        return counter == 0 ? ArmorState.Empty : counter == 4 ? ArmorState.Full : ArmorState.Semi;
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
        return new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.PLAYER_HEAD : Material.getMaterial("SKULL_ITEM"));
    }

    public static ItemStack getSkull(OfflinePlayer offlinePlayer) {
        ItemStack skull = new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.PLAYER_HEAD : Material.getMaterial("SKULL_ITEM"), 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            meta.setOwningPlayer(offlinePlayer);
        } else {
            String name = offlinePlayer.getName();

            if (name != null) {
                meta.setOwner(name);
            }
        }
        skull.setItemMeta(meta);
        return skull;
    }
}
