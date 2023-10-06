package me.vagdedes.spartan.gui.info;

import me.vagdedes.spartan.abstraction.InventoryMenu;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.functionality.moderation.Debug;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class DebugMenu extends InventoryMenu {

    private static final String menu = "§0Debug: ".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);

    public DebugMenu() {
        super(menu, 27, new Permission[]{Permission.INFO, Permission.MANAGE});
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        SpartanPlayer target = (SpartanPlayer) object;
        setTitle(player, menu + target.getName());

        // Separator
        ItemStack i = new ItemStack(Material.IRON_SWORD);

        if (Debug.has(player, target, Enums.Debug.COMBAT)) {
            i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        add("§7" + Enums.Debug.COMBAT.toString(), null, i, 10);

        // Separator
        i = new ItemStack(MaterialUtils.get("gold_boots"));

        if (Debug.has(player, target, Enums.Debug.MOVEMENT)) {
            i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        add("§7" + Enums.Debug.MOVEMENT.toString(), null, i, 12);

        // Separator
        i = new ItemStack(Material.COMPASS);

        if (Debug.has(player, target, Enums.Debug.MISC)) {
            i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        add("§7" + Enums.Debug.MISC.toString(), null, i, 14);

        // Separator
        add("§cDisable", null, new ItemStack(Material.ARROW), 16);
        return true;
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        SpartanPlayer t = SpartanBukkit.getPlayer(title.substring(menu.length()));

        if (t == null) {
            player.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
        } else if (item.equals("Disable")) {
            Debug.remove(player, t);
            player.sendInventoryCloseMessage(null);
        } else {
            if (!Permissions.has(player, Permission.INFO) && !Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                for (Enums.Debug debug : Enums.Debug.values()) {
                    if (debug.toString().equals(item)) {
                        Debug.add(player, t, debug);
                        break;
                    }
                }
                player.sendInventoryCloseMessage(null);
            }
        }
        return true;
    }
}
