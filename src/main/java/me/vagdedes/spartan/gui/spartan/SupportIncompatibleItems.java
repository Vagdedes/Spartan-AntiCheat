package me.vagdedes.spartan.gui.spartan;

import me.vagdedes.spartan.compatibility.manual.essential.MinigameMaker;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.objects.features.IncompatibleItem;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SupportIncompatibleItems {

    private static final String menu = "§0Support Incompatible Items".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);
    private static final int menuSize = 54;

    public static void open(SpartanPlayer p) {
        if (!Permissions.has(p, Enums.Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            return;
        }
        Inventory inv = p.createInventory(menuSize, menu);
        List<String> lore = new ArrayList<>(20);
        int counter = 0;

        for (IncompatibleItem incompatibleItem : MinigameMaker.getIncompatibleItems()) {
            counter++;
            String name = incompatibleItem.getName();

            if (name == null) {
                name = "No Name Required";
            }

            lore.clear();
            lore.add("");
            lore.add("§7Left Click to §aincrease §7time.");
            lore.add("§7Right Click to §cdecrease §7time.");
            lore.add("§7Shift Click to §4delete §7the item.");
            lore.add("");
            lore.add("§7Unique ID§8:§e " + incompatibleItem.hashCode());
            lore.add("§7Event§8:§e " + incompatibleItem.getEventType().toString().replace("_", "-"));
            lore.add("§7Material§8:§e " + BlockUtils.materialToString(incompatibleItem.getMaterial()));
            lore.add("§7Name§8:§e " + name);
            lore.add("§7Time§8:§e " + incompatibleItem.getSeconds() + " second(s)");
            lore.add("");
            lore.add("§7Checks§8:");

            for (Enums.HackType hackType : incompatibleItem.getHackTypes()) {
                lore.add("§e" + hackType.toString().replace("_", "-"));
            }
            InventoryUtils.add(inv, "§6" + name, lore, new ItemStack(incompatibleItem.getMaterial()), -1);

            if (counter == menuSize) {
                break;
            }
        }

        InventoryUtils.add(inv, "§2Add Item", null, new ItemStack(MaterialUtils.get("redstone_torch")), 52);

        InventoryUtils.add(inv, "§4Back", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), 53);

        p.openInventory(inv);
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title, ClickType clickType) {
        ItemMeta meta = i.getItemMeta();

        if (meta == null || meta.getDisplayName() == null || !title.equals(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();

        if (i.getType() == Material.ARROW && item.equals("§4Back")) {
            SpartanMenu.open(p);
        } else if (i.getType() == MaterialUtils.get("redstone_torch") && item.equals("§2Add Item")) {
            Player n = p.getPlayer();

            if (n != null && n.isOnline()) {
                Bukkit.dispatchCommand(n, "spartan add-incompatible-item");
                n.closeInventory();
            }
        } else {
            List<String> lore = meta.getLore();

            if (lore != null) {
                for (String line : lore) {
                    line = StringUtils.getClearColorString(line);
                    String match = "Unique ID: ";

                    if (line.startsWith(match)) {
                        line = line.substring(match.length());

                        if (AlgebraUtils.validInteger(line)) {
                            IncompatibleItem incompatibleItem = MinigameMaker.getByID(Integer.valueOf(line));

                            if (incompatibleItem != null) {
                                if (clickType.isLeftClick()) {
                                    MinigameMaker.updateSeconds(incompatibleItem, incompatibleItem.getSeconds() + 1);
                                } else if (clickType.isRightClick()) {
                                    MinigameMaker.updateSeconds(incompatibleItem, incompatibleItem.getSeconds() - 1);
                                } else if (clickType.isShiftClick()) {
                                    MinigameMaker.removeItem(incompatibleItem);
                                }
                            }
                            open(p);
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }
}
