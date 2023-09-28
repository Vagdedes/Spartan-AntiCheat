package me.vagdedes.spartan.gui.configuration;

import com.vagdedes.filegui.api.FileGUIAPI;
import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.abstraction.InventoryMenu;
import me.vagdedes.spartan.compatibility.necessary.FileGUI;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.configuration.ConfigurationDiagnostics;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.gui.SpartanMenu;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ManageConfiguration extends InventoryMenu {

    private static final int menuSize = 27;
    private static final String menu = "Configurations";
    public static final String
            compatibilityFileName = "compatibility.yml",
            checksFileName = "checks.yml";
    public static final String[] configs = new String[]{
            "config.yml", "Config.settings.yml", checksFileName, compatibilityFileName,
            "messages.yml", "sql.yml"
    };
    private static final Map<UUID, Integer> map = new LinkedHashMap<>(Config.getMaxPlayers());
    private static final String documentationURL = "https://docs.google.com/document/d/e/2PACX-1vSu-WfjoyG8ipSI4tw3CqgmYh8gGDriSgD8gZTQ8HqU4k8jq9eYE8gzW3oiuKf6qzuvH7GTxssnMO_5/pub";

    public ManageConfiguration() {
        super(menu, menuSize, Enums.Permission.MANAGE);
    }

    public void clear() {
        map.clear();
    }

    public void save(SpartanPlayer player, boolean leave) {
        UUID uuid = player.getUniqueId();
        Integer number = map.get(uuid);

        if (number != null) {
            boolean reload = false;

            if (leave) {
                map.remove(uuid);
                reload = true;
            } else if (number > 0) {
                map.put(uuid, number - 1);
            } else {
                map.remove(uuid);
                reload = true;
            }

            if (reload) {
                Config.create();
            }
        }
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            player.sendInventoryCloseMessage(ChatColor.RED + "This menu is not available to versions prior to 1.9.");
            return false;
        } else if (Compatibility.CompatibilityType.FileGUI.isFunctional()) {
            Player n = player.getPlayer();

            if (n != null && n.isOnline() && n.hasPermission(FileGUI.permission)) {
                FileGUIAPI.openMenu(n, Register.plugin.getDataFolder().getPath(), 1);
                return false;
            }
        }
        File[] files = Register.plugin.getDataFolder().listFiles();
        int documentationItem;

        if (Config.hasCancelAfterViolationOption()) {
            List<String> lore = new ArrayList<>(20);
            lore.add("");
            lore.add("§7This feature is a semi-permanent solution to");
            lore.add("§7solve unwanted low-violation false positives.");
            lore.add("§7It will study your local or database logs, based");
            lore.add("§7on your configuration preferences, and will");
            lore.add("§7automatically adjust your config.yml configuration");
            lore.add("§7for a better and more stable checking performance.");
            lore.add("");
            lore.add("§cPlease do not use this feature in a server that");
            lore.add("§callows any sort of hacking module. It will possibly");
            lore.add("§capply false changes to the configuration.");
            InventoryUtils.add(inventory, "§aConfiguration Diagnostics", lore, new ItemStack(MaterialUtils.get("redstone_torch")), menuSize - 2);
            documentationItem = menuSize - 3;
        } else {
            documentationItem = menuSize - 2;
        }

        InventoryUtils.add(inventory, "§aDocumentation", null, new ItemStack(Material.PAPER), documentationItem);

        InventoryUtils.add(inventory, "§4Back", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), menuSize - 1);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    for (String config : configs) {
                        String name = file.getName();

                        if (name.equals(config)) {
                            InventoryUtils.add(inventory, "§c" + name.replace(".yml", ""), null,
                                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.WRITABLE_BOOK : Material.getMaterial("BOOK_AND_QUILL")), -1);
                        }
                    }
                }
            }
        }
        return true;
    }

    public void openChild(SpartanPlayer player, String s) {
        openChild(player, s, -1, null);
    }

    private void openChild(SpartanPlayer player, String s, int slot, List<String> list) {
        boolean hasPermission = false;

        for (Enums.Permission permission : permissions) {
            if (Permissions.has(player, permission)) {
                hasPermission = true;
                break;
            }
        }
        if (!hasPermission) {
            player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            return;
        }
        boolean access = false;

        for (String config : configs) {
            if (config.equals(s + ".yml")) {
                access = true;
                break;
            }
        }

        if (!access) {
            return;
        }
        setTitle(player, menu + ": " + s);
        InventoryUtils.add(inventory, "§4Back", null, new ItemStack(Material.ARROW), 53);
        int counter = 0;

        File file = new File(Register.plugin.getDataFolder() + "/" + s + ".yml");
        YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);

        if (file.exists()) {
            for (String key : filea.getKeys(true)) {
                Object obj = filea.get(key);
                String type = obj instanceof Boolean ? "Logical" :
                        obj instanceof Double ? "Decimal" :
                                obj instanceof Integer ? "Number" :
                                        obj instanceof String ? "Text" :
                                                null;

                if (obj != null && type != null) {
                    if (counter == slot) {
                        InventoryUtils.add(inventory, "§c" + key, list, new ItemStack(Material.PAPER), -1);
                    } else {
                        String value = obj.toString();
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("§7Type§8:§c " + type);
                        lore.add("§7Value§8:§c " + (value.equals("") ? "(Empty)" : (key.contains("password") ? "§k" : "") + value));
                        lore.add("");

                        if (type.equals("Logical")) {
                            lore.add("§7Left click to set to §aTrue");
                            lore.add("§7Right click to set to §cFalse");
                        } else if (type.equals("Decimal")) {
                            lore.add("§7Left click to §aincrease §7by §a0.1");
                            lore.add("§7Right click to §cdecrease §7by §c0.1");
                        } else if (type.equals("Number")) {
                            lore.add("§7Left click to §aincrease §7by §a1");
                            lore.add("§7Right click to §cdecrease §7by §c1");
                        } else {
                            lore.add("§7No modification available. Please use a file explorer.");
                        }
                        InventoryUtils.add(inventory, "§c" + key, lore, new ItemStack(Material.PAPER), -1);
                    }
                    counter++;

                    if (counter == 53) {
                        break;
                    }
                }
            }
        }
        player.openInventory(inventory);
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Back")) {
            if (title.equals(menu)) {
                SpartanMenu.mainMenu.open(player);
            } else {
                open(player);
            }
        } else if (item.equals("Configuration Diagnostics")) {
            if (!Permissions.has(player, Enums.Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
                return true;
            }
            ConfigurationDiagnostics.execute(player);
            player.sendInventoryCloseMessage(null);
        } else if (item.equals("Documentation")) {
            player.sendImportantMessage("§7Click to learn more about the configuration§8: \n§a§n" + documentationURL);
        } else {
            if (title.equals(menu)) {
                openChild(player, item);
            } else {
                modify(player, itemStack, title, clickType, slot);
            }
        }
        return true;
    }

    private void modify(SpartanPlayer player, ItemStack itemStack, String title, ClickType clickType, int slot) {
        ItemMeta meta = itemStack.getItemMeta();
        List<String> list = meta.getLore();

        if (list != null) {
            String config = title.substring(menu.length() + 2);
            File file = new File(Register.plugin.getDataFolder() + "/" + config + ".yml");

            if (file.exists()) {
                String path = StringUtils.getClearColorString(meta.getDisplayName().substring(2));
                String type = list.get(1).substring(12);
                Object value = list.get(2).substring(13);
                boolean refresh = true;

                switch (type) {
                    case "Logical":
                        if (clickType.isLeftClick()) {
                            value = true;
                            ConfigUtils.set(file, path, value);
                            list.set(2, "§7Value§8:§c " + value);
                        } else if (clickType.isRightClick()) {
                            value = false;
                            ConfigUtils.set(file, path, value);
                            list.set(2, "§7Value§8:§c " + value);
                        } else {
                            refresh = false;
                        }
                        break;
                    case "Decimal":
                        double dbl = Double.parseDouble(value.toString());

                        if (clickType.isLeftClick()) {
                            dbl += 0.1;
                            ConfigUtils.set(file, path, dbl);
                            list.set(2, "§7Value§8:§c " + dbl);
                        } else if (clickType.isRightClick()) {
                            dbl -= 0.1;
                            ConfigUtils.set(file, path, dbl);
                            list.set(2, "§7Value§8:§c " + dbl);
                        } else {
                            refresh = false;
                        }
                        break;
                    case "Number":
                        int num = Integer.parseInt(value.toString());

                        if (clickType.isLeftClick()) {
                            num += 1;
                            ConfigUtils.set(file, path, num);
                            list.set(2, "§7Value§8:§c " + num);
                        } else if (clickType.isRightClick()) {
                            num -= 1;
                            ConfigUtils.set(file, path, num);
                            list.set(2, "§7Value§8:§c " + num);
                        } else {
                            refresh = false;
                        }
                        break;
                    case "Text":
                    default:
                        refresh = false;
                        break;
                }
                if (refresh) {
                    // Cache
                    reload(true);

                    // Inventory
                    map.put(player.getUniqueId(), 1);
                    openChild(player, config, slot, list);
                }
            }
        }
    }

    public void reload(boolean resetChecks) {
        if (!resetChecks) { // Do not run because their cache will be cleared by the Config method
            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearConfigurationCache();
            }
        }
        Config.settings.clear();
        Config.messages.clear();
        Config.sql.refreshConfiguration();
        Config.compatibility.clear();

        // Always last
        Config.refreshVariables(resetChecks);
    }
}
