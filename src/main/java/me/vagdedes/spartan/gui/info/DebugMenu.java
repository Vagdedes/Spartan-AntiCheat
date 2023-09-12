package me.vagdedes.spartan.gui.info;

import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.moderation.Debug;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DebugMenu {

    private static final String menu = "§0Debug: ".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);

    static void open(SpartanPlayer p, SpartanPlayer t) {
        Inventory inv = p.createInventory(27, menu + t.getName());

        // Separator
        ItemStack i = new ItemStack(Material.IRON_SWORD);

        if (Debug.has(p, t, Enums.Debug.COMBAT)) {
            i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        InventoryUtils.add(inv, "§7" + Enums.Debug.COMBAT.getString(), null, i, 10);

        // Separator
        i = new ItemStack(MaterialUtils.get("gold_boots"));

        if (Debug.has(p, t, Enums.Debug.MOVEMENT)) {
            i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        InventoryUtils.add(inv, "§7" + Enums.Debug.MOVEMENT.getString(), null, i, 12);

        // Separator
        i = new ItemStack(Material.COMPASS);

        if (Debug.has(p, t, Enums.Debug.MISC)) {
            i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        InventoryUtils.add(inv, "§7" + Enums.Debug.MISC.getString(), null, i, 14);

        // Separator
        InventoryUtils.add(inv, "§cDisable", null, new ItemStack(Material.ARROW), 16);
        p.openInventory(inv);
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title) {
        if (!title.startsWith(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        SpartanPlayer t = SpartanBukkit.getPlayer(title.substring(menu.length()));

        if (t == null) {
            p.sendInventoryCloseMessage(Messages.get("player_not_found_message"));
        } else if (item.equals("Disable")) {
            Debug.remove(p, t);
            p.sendInventoryCloseMessage(null);
        } else {
            if (!Permissions.has(p, Permission.INFO) && !Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            } else {
                for (Enums.Debug debug : Enums.Debug.values()) {
                    if (debug.getString().equals(item)) {
                        Debug.add(p, t, debug);
                        break;
                    }
                }
                p.sendInventoryCloseMessage(null);
            }
        }
        return true;
    }
}
