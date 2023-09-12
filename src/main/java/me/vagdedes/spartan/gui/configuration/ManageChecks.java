package me.vagdedes.spartan.gui.configuration;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.gui.spartan.SpartanMenu;
import me.vagdedes.spartan.handlers.stability.CancelViolation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ManageChecks {

    private static final String menu = "§0Manage Checks".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);

    public static void open(SpartanPlayer p) {
        if (!Permissions.has(p, Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Messages.get("no_permission"));
            return;
        }
        Inventory inv = p.createInventory(54, menu);

        for (HackType check : Enums.HackType.values()) {
            addCheck(p, inv, check);
        }
        InventoryUtils.add(inv, "§cDisable silent checking for all checks", null, new ItemStack(MaterialUtils.get("lead")), 46);
        InventoryUtils.add(inv, "§cDisable all checks", null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 14), 47);

        InventoryUtils.add(inv, "§4Back", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), 49);

        InventoryUtils.add(inv, "§aEnable all checks", null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5), 51);
        InventoryUtils.add(inv, "§aEnable silent checking for all checks", null, new ItemStack(Material.FEATHER), 52);
        p.openInventory(inv);
    }

    private static void addCheck(SpartanPlayer p, Inventory inv, HackType hackType) {
        Check check = hackType.getCheck();
        boolean enabled = check.isEnabled(null, null, null),
                silent = check.isSilent(null, null),
                bypassing = Permissions.isBypassing(p, hackType);
        String[] disabledDetections = CloudFeature.getDisabledDetections(hackType);
        int cancelViolation = check.getDefaultCancelViolation();
        int problematicDetections = check.getProblematicDetections();

        String enabledOption;
        String silentOption = null;
        String colour, secondColour;
        ItemStack item;

        if (check.canBeSilent()) {
            if (silent) {
                silentOption = "§7Right click to §cdisable §7silent checking.";
            } else {
                silentOption = "§7Right click to §aenable §7silent checking.";
            }
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

        for (String s : check.getDescription()) {
            lore.add("§e" + s);
        }

        // Separator
        if (disabledDetections.length > 0) {
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
        lore.add("");

        for (ResearchEngine.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(false)) {
            lore.add("§7§l" + dataType.toString() + " §r§7Cancel Violation§8:§c " + Math.max(cancelViolation, CancelViolation.get(hackType, dataType)));
        }
        if (problematicDetections > 0) {
            lore.add("§7Problematic Detections§8: " + problematicDetections);
        }

        // Separator
        if (Config.isLegacy()) {
            lore.add("");
            lore.add("§7Punishment Categories§8:");

            for (Enums.PunishmentCategory category : Enums.PunishmentCategory.values()) {
                int violations = Check.getCategoryPunishment(hackType, ResearchEngine.DataType.Universal, category);

                if (violations == 1) {
                    lore.add("§4" + category + " §c" + violations + " Violation");
                } else {
                    lore.add("§4" + category + " §c" + violations + " Violations");
                }
            }
        }

        // Separator
        lore.add("");
        lore.add((enabled ? "§a" : "§c") + "Enabled §8/ "
                + (silent ? "§a" : "§c") + "Silent §8/ "
                + (check.canPunish() ? "§a" : "§c") + "Punishments §8/ "
                + (bypassing ? "§a" : "§c") + "Bypassing");

        /*int violationDivisor = ViolationDivisor.get(p, null, HackType);
        int defaultViolationDivisor = DefaultConfiguration.getViolationDivisor(HackType);
        lore.add("§7Violation Divisor§8: §4" + violationDivisor + (violationDivisor != defaultViolationDivisor ? " (Default: " + defaultViolationDivisor + ")" : ""));

        if (Config.canBeSilent(HackType)) {
            int cancelViolation = CancelViolation.getPreferred(HackType, world);
            int defaultCancelDivisor = DefaultConfiguration.getCancelAfterViolation(HackType) + 1;
            lore.add("§7Cancel Violation§8: §4" + cancelViolation + (cancelViolation != defaultCancelDivisor ? " (Default: " + defaultViolationDivisor + ")" : ""));
        }*/

        if (Config.isLegacy()) {
            for (int i = 1; i <= Check.maxViolations; i++) {
                int counter = 0;

                for (String s : check.getLegacyCommands(i)) {
                    if (s != null) {
                        counter++;
                        String base = "§7" + i + "§8:§f ";

                        if (s.length() > 40) {
                            lore.add(base + s.substring(0, 40));
                        } else {
                            lore.add(base + s);
                        }

                        if (counter >= Check.maxCommands) {
                            break;
                        }
                    }
                }
            }
        } else {
            int counter = 0;

            for (String s : check.getCommands()) {
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
        }

        // Separator
        lore.add("");
        lore.add(enabledOption);

        if (silentOption != null) {
            lore.add(silentOption);
        }
        if (check.getStoredOptions().size() > 0) {
            lore.add("§7Shift click to §emanage options");
        }

        // Separator

        if (enabled && silent) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        InventoryUtils.add(inv, colour + check.getName() + " " + secondColour + check.getCheckType().getName() + " Check", lore, item, -1);
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title, ClickType click) {
        if (!title.equals(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (!Permissions.has(p, Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Messages.get("no_permission"));
            return true;
        }
        if (item.equals("Back")) {
            SpartanMenu.open(p);
        } else if (item.equals("Disable all checks")) {
            Config.disableChecks();
            open(p);
        } else if (item.equals("Enable all checks")) {
            Config.enableChecks();
            open(p);
        } else if (item.equals("Disable silent checking for all checks")) {
            Config.disableSilentChecking();
            open(p);
        } else if (item.equals("Enable silent checking for all checks")) {
            Config.enableSilentChecking();
            open(p);
        } else {
            item = item.split(" ")[0];

            if (click == ClickType.LEFT) {
                setEnable(p, item);
            } else if (click == ClickType.RIGHT) {
                setSilent(p, item);
            } else if (click.isShiftClick()) {
                manageOptions(p, item);
            }
        }
        return true;
    }

    private static void setEnable(SpartanPlayer p, String item) {
        Check check = Config.getCheckByName(item);

        if (check != null) {
            if (check.isEnabled(null, null, null)) {
                check.setEnabled(null, false);
            } else {
                check.setEnabled(null, true);
            }
        }
        open(p);
    }

    private static void setSilent(SpartanPlayer p, String item) {
        Check check = Config.getCheckByName(item);

        if (check != null) {
            if (check.isSilent(null, null)) {
                check.setSilent("false");
            } else {
                check.setSilent("true");
            }
        }
        open(p);
    }

    private static void manageOptions(SpartanPlayer p, String item) {
        Check check = Config.getCheckByName(item);

        if (check != null) {
            ManageOptions.open(p, check.getHackType());
        }
    }
}
