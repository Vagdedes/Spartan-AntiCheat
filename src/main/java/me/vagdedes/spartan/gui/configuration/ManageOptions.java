package me.vagdedes.spartan.gui.configuration;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class ManageOptions {

    private static final String menu = "§0Manage: ".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);

    public static void open(SpartanPlayer p, HackType hackType) {
        Set<String[]> options = hackType.getCheck().getStoredOptions();
        int amount = options.size();

        if (amount > 0) {
            if (!Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
                return;
            }
            double division = (amount + 2) / 9.0;
            int size = Math.min(6,
                    (division - Math.floor(division) == 0.0 ?
                            (int) (division + 1) :
                            (int) Math.ceil(division))
            ) * 9;
            Inventory inv = p.createInventory(size, menu + hackType);

            for (String[] option : options) {
                String value = option[1].toLowerCase();
                int cases;

                if (value.equalsIgnoreCase("true")) {
                    cases = 1;
                } else if (value.equalsIgnoreCase("false")) {
                    cases = 2;
                } else {
                    continue;
                }

                ItemStack item;
                boolean positive = cases == 1;

                if (positive) {
                    item = new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_DYE : Material.getMaterial("INK_SACK"), 1, (short) 10);
                } else {
                    item = new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.GRAY_DYE : Material.getMaterial("INK_SACK"), 1, (short) 8);
                }
                InventoryUtils.add(inv, "§" + (positive ? "a" : "c") + option[0], null, item, -1);
            }
            InventoryUtils.add(inv, "§4Reset Stored Data", AntiCheatUpdates.getInformation(false),
                    new ItemStack(Material.REDSTONE), size - 2);
            InventoryUtils.add(inv, "§cBack", AntiCheatUpdates.getInformation(false),
                    new ItemStack(Material.ARROW), size - 1);
            p.openInventory(inv);
        }
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title) {
        if (!title.startsWith(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (!Permissions.has(p, Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            return true;
        }
        if (item.equals("Back")) {
            ManageChecks.open(p);
        } else if (item.equals("Reset Stored Data")) {
            String[] split = title.split(": ");

            if (split.length == 2) {
                String hackTypeString = split[1];

                for (HackType hackType : Enums.HackType.values()) {
                    String name = hackType.getCheck().getName();

                    if (name.equals(hackTypeString)) { // Do not use name, this is configuration based
                        ResearchEngine.resetData(hackType);
                        String message = Config.messages.getColorfulString("check_stored_data_delete_message").replace("{check}", name);
                        p.sendMessage(message);
                        ManageChecks.open(p);
                        break;
                    }
                }
            }
        } else {
            String[] split = item.split("\\.");

            if (split.length > 1) {
                String hackTypeString = split[0],
                        option = item.substring(hackTypeString.length() + 1);

                for (HackType hackType : Enums.HackType.values()) {
                    if (hackType.toString().equals(hackTypeString)) { // Do not use name, this is configuration based
                        Check check = hackType.getCheck();
                        Object value = check.getOption(option, null, true);

                        if (value instanceof Boolean) {
                            check.setOption(option, !((boolean) value));
                        }
                        open(p, hackType); // Always last
                        break;
                    }
                }
            }
        }
        return true;
    }
}
