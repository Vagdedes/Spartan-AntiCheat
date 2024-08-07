package com.vagdedes.spartan.abstraction.inventory.implementation;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.utils.minecraft.inventory.EnchantmentUtils;
import com.vagdedes.spartan.utils.minecraft.world.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ManageChecks extends InventoryMenu {

    public ManageChecks() {
        super("§0Manage Checks".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0), 54, Permission.MANAGE);
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        for (HackType check : Enums.HackType.values()) {
            addCheck(player, check);
        }
        add("§cDisable silent checking for all checks", null, new ItemStack(MaterialUtils.get("lead")), 46);
        add("§cDisable all checks", null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 14), 47);

        add("§4Back", null, new ItemStack(Material.ARROW), 49);

        add("§aEnable all checks", null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5), 51);
        add("§aEnable silent checking for all checks", null, new ItemStack(Material.FEATHER), 52);
        return true;
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Back")) {
            InteractiveInventory.mainMenu.open(player);
        } else if (item.equals("Disable all checks")) {
            Config.disableChecks();
            open(player);
        } else if (item.equals("Enable all checks")) {
            Config.enableChecks();
            open(player);
        } else if (item.equals("Disable silent checking for all checks")) {
            Config.disableSilentChecking();
            open(player);
        } else if (item.equals("Enable silent checking for all checks")) {
            Config.enableSilentChecking();
            open(player);
        } else {
            item = item.split(" ")[0];

            if (clickType == ClickType.LEFT) {
                Check check = Config.getCheckByName(item);

                if (check != null) {
                    check.setEnabled(null, !check.isEnabled(null, null, null));
                }
                open(player);
            } else if (clickType == ClickType.RIGHT) {
                Check check = Config.getCheckByName(item);

                if (check != null) {
                    check.setSilent(null, !check.isSilent(null, null));
                }
                open(player);
            } else if (clickType.isShiftClick()) {
                Check check = Config.getCheckByName(item);

                if (check != null) {
                    ResearchEngine.resetData(check.hackType);
                    player.sendInventoryCloseMessage(
                            Config.messages.getColorfulString("check_stored_data_delete_message")
                                    .replace("{check}", check.getName())
                    );

                }
            }
        }
        return true;
    }

    private void addCheck(SpartanPlayer player, HackType hackType) {
        Check check = hackType.getCheck();
        boolean enabled = check.isEnabled(null, null, null),
                silent = check.isSilent(null, null),
                bypassing = Permissions.isBypassing(player, hackType);
        String[] disabledDetections = CloudBase.getShownDisabledDetections(hackType);
        String enabledOption, silentOption, colour, secondColour;
        ItemStack item;

        if (silent) {
            silentOption = "§7Right click to §cdisable §7silent checking.";
        } else {
            silentOption = "§7Right click to §aenable §7silent checking.";
        }

        if (enabled) {
            item = new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_DYE : Material.getMaterial("INK_SACK"), 1, (short) 10);
            colour = "§2";
            secondColour = "§a";
            enabledOption = "§7Left click to §cdisable §7check.";
        } else {
            item = new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.GRAY_DYE : Material.getMaterial("INK_SACK"), 1, (short) 8);
            colour = "§4";
            secondColour = "§c";
            enabledOption = "§7Left click to §aenable §7check.";
        }

        List<String> lore = new ArrayList<>(30);

        for (String s : hackType.description) {
            lore.add("§e" + s);
        }

        // Separator
        if (disabledDetections != null) {
            List<String> extra = new ArrayList<>();
            int normalPlaceholder = 0;
            extra.add("");
            extra.add("§7Forcefully Disabled Detections§8:");

            for (String detection : disabledDetections) {
                if (!detection.startsWith(" ")) {
                    normalPlaceholder++;
                    extra.add("§4" + detection);
                }
            }
            if (normalPlaceholder > 0) {
                lore.addAll(extra);
            }
        }

        // Separator
        lore.add("");
        lore.add((enabled ? "§a" : "§c") + "Enabled §8/ "
                + (silent ? "§a" : "§c") + "Silent §8/ "
                + (check.canPunish(null) ? "§a" : "§c") + "Punishments §8/ "
                + (bypassing ? "§a" : "§c") + "Bypassing");
        int counter = 0;

        for (String s : Config.settings.getPunishmentCommands()) {
            if (s != null) {
                counter++;
                String base = "§7" + counter + "§8:§f ";

                if (s.length() > 40) {
                    lore.add(base + s.substring(0, 40));
                } else {
                    lore.add(base + s);
                }
            }
        }

        // Separator
        lore.add("");
        lore.add(enabledOption);

        if (silentOption != null) {
            lore.add(silentOption);
        }
        lore.add("§7Shift click to §edelete §7the check's data.");

        // Separator

        if (enabled && silent) {
            item.addUnsafeEnchantment(EnchantmentUtils.DURABILITY, 1);
        }
        add(colour + check.getName() + " " + secondColour + hackType.category.toString() + " Check", lore, item, -1);
    }
}
