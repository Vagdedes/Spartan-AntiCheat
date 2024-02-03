package com.vagdedes.spartan.gui.configuration;

import com.vagdedes.spartan.abstraction.InventoryMenu;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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

        add("§4Back", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), 49);

        add("§aEnable all checks", null, new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5), 51);
        add("§aEnable silent checking for all checks", null, new ItemStack(Material.FEATHER), 52);
        return true;
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Back")) {
            SpartanMenu.mainMenu.open(player);
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
                setEnable(player, item);
            } else if (clickType == ClickType.RIGHT) {
                setSilent(player, item);
            } else if (clickType.isShiftClick()) {
                manageOptions(player, item);
            }
        }
        return true;
    }

    private void addCheck(SpartanPlayer player, HackType hackType) {
        Check check = hackType.getCheck();
        boolean enabled = check.isEnabled(null, null, null),
                silent = check.isSilent(null, null),
                bypassing = Permissions.isBypassing(player, hackType);
        String[] disabledDetections = CloudFeature.getShownDisabledDetections(hackType);
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
            for (int i = 1; i <= Check.maxViolationsPerCycle; i++) {
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
        if (!check.getStoredOptions().isEmpty()) {
            lore.add("§7Shift click to §emanage options");
        }

        // Separator

        if (enabled && silent) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        add(colour + check.getName() + " " + secondColour + check.getCheckType().toString() + " Check", lore, item, -1);
    }

    private void setEnable(SpartanPlayer player, String item) {
        Check check = Config.getCheckByName(item);

        if (check != null) {
            if (check.isEnabled(null, null, null)) {
                check.setEnabled(null, false);
            } else {
                check.setEnabled(null, true);
            }
        }
        open(player);
    }

    private void setSilent(SpartanPlayer player, String item) {
        Check check = Config.getCheckByName(item);

        if (check != null) {
            if (check.isSilent(null, null)) {
                check.setSilent("false");
            } else {
                check.setSilent("true");
            }
        }
        open(player);
    }

    private void manageOptions(SpartanPlayer player, String item) {
        Check check = Config.getCheckByName(item);

        if (check != null) {
            SpartanMenu.manageOptions.open(player, check.getHackType());
        }
    }
}
