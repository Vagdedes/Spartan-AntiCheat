package com.vagdedes.spartan.gui.configuration;

import com.vagdedes.spartan.abstraction.InventoryMenu;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class ManageOptions extends InventoryMenu {

    private static final String menu = "§0Manage: ".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);

    public ManageOptions() {
        super(menu, 9, Permission.MANAGE);
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        HackType hackType = (HackType) object;
        Set<String[]> options = hackType.getCheck().getStoredOptions();
        int amount = options.size();

        if (amount > 0) {
            double division = (amount + 2) / 9.0;
            int size = Math.min(6,
                    (division - Math.floor(division) == 0.0 ?
                            (int) (division + 1) :
                            (int) Math.ceil(division))
            ) * 9;
            setInventory(player, menu + hackType, size);

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
                add("§" + (positive ? "a" : "c") + option[0], null, item, -1);
            }
        } else {
            setInventory(player, menu + hackType, 9);
        }
        add("§4Reset Stored Data", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.REDSTONE), size - 2);
        add("§cBack", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), size - 1);
        return true;
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Back")) {
            SpartanMenu.manageChecks.open(player);
        } else if (item.equals("Reset Stored Data")) {
            String[] split = title.split(": ");

            if (split.length == 2) {
                String hackTypeString = split[1];

                for (HackType hackType : Enums.HackType.values()) {
                    String name = hackType.getCheck().getName();

                    if (name.equals(hackTypeString)) { // Do not use name, this is configuration based
                        ResearchEngine.resetData(hackType);
                        String message = Config.messages.getColorfulString("check_stored_data_delete_message").replace("{check}", name);
                        player.sendMessage(message);
                        SpartanMenu.manageChecks.open(player);
                        break;
                    }
                }
            }
        } else {
            String[] split = item.split("\\.");

            if (split.length > 1) {
                String hackTypeString = split[0];

                for (HackType hackType : Enums.HackType.values()) {
                    if (hackType.toString().equals(hackTypeString)) { // Do not use name, this is configuration based
                        Check check = hackType.getCheck();
                        Object value = check.getOption(item, null, true);

                        if (value instanceof Boolean) {
                            check.setOption(item, !((boolean) value));
                        }
                        open(player, hackType); // Always last
                        break;
                    }
                }
            }
        }
        return true;
    }

}
