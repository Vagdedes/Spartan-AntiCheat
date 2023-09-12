package me.vagdedes.spartan.gui.configuration;

import com.vagdedes.filegui.api.FileGUIAPI;
import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.compatibility.necessary.FileGUI;
import me.vagdedes.spartan.configuration.*;
import me.vagdedes.spartan.features.configuration.ConfigurationDiagnostics;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.gui.spartan.SpartanMenu;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ManageConfiguration {

    private static final String menu = "Configurations";
    public static final String
            compatibilityFileName = "compatibility.yml",
            checksFileName = "checks.yml";
    public static final String[] configs = new String[]{
            "config.yml", "settings.yml", checksFileName, compatibilityFileName,
            "messages.yml", "sql.yml"
    };
    private static final Map<UUID, Integer> map = new LinkedHashMap<>(Config.getMaxPlayers());
    private static final String documentationURL = "https://docs.google.com/document/d/e/2PACX-1vSu-WfjoyG8ipSI4tw3CqgmYh8gGDriSgD8gZTQ8HqU4k8jq9eYE8gzW3oiuKf6qzuvH7GTxssnMO_5/pub";

    public static void clear() {
        map.clear();
    }

    public static void save(SpartanPlayer p, boolean leave) {
        UUID uuid = p.getUniqueId();
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

    public static void open(SpartanPlayer p) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            p.sendInventoryCloseMessage(ChatColor.RED + "This menu is not available to versions prior to 1.9.");
            return;
        }
        if (!Permissions.has(p, Enums.Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Messages.get("no_permission"));
            return;
        }
        if (FileGUI.isEnabled()) {
            Player n = p.getPlayer();

            if (n != null && n.isOnline() && n.hasPermission(FileGUI.permission)) {
                FileGUIAPI.openMenu(n, Register.plugin.getDataFolder().getPath(), 1);
                return;
            }
        }
        int size = 27;
        Inventory inv = p.createInventory(size, menu);
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
            InventoryUtils.add(inv, "§aConfiguration Diagnostics", lore, new ItemStack(MaterialUtils.get("redstone_torch")), size - 2);
            documentationItem = size - 3;
        } else {
            documentationItem = size - 2;
        }

        InventoryUtils.add(inv, "§aDocumentation", null, new ItemStack(Material.PAPER), documentationItem);

        InventoryUtils.add(inv, "§4Back", AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), size - 1);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    for (String config : configs) {
                        String name = file.getName();

                        if (name.equals(config)) {
                            InventoryUtils.add(inv, "§c" + name.replace(".yml", ""), null,
                                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.WRITABLE_BOOK : Material.getMaterial("BOOK_AND_QUILL")), -1);
                        }
                    }
                }
            }
        }
        p.openInventory(inv);
    }

    public static void openChild(SpartanPlayer p, String s) {
        openChild(p, s, -1, null);
    }

    private static void openChild(SpartanPlayer p, String s, int slot, List<String> list) {
        if (!Permissions.has(p, Enums.Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Messages.get("no_permission"));
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
        Inventory inv = p.createInventory(54, menu + ": " + s);
        InventoryUtils.add(inv, "§4Back", null, new ItemStack(Material.ARROW), 53);
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
                        InventoryUtils.add(inv, "§c" + key, list, new ItemStack(Material.PAPER), -1);
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
                        InventoryUtils.add(inv, "§c" + key, lore, new ItemStack(Material.PAPER), -1);
                    }
                    counter++;

                    if (counter == 53) {
                        break;
                    }
                }
            }
        }
        p.openInventory(inv);
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title, ClickType click, int slot) {
        if (!title.startsWith(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Back")) {
            if (title.equals(menu)) {
                SpartanMenu.open(p);
            } else {
                open(p);
            }
        } else if (item.equals("Configuration Diagnostics")) {
            if (!Permissions.has(p, Enums.Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
                return true;
            }
            ConfigurationDiagnostics.execute(p);
            p.sendInventoryCloseMessage(null);
        } else if (item.equals("Documentation")) {
            p.sendImportantMessage("§7Click to learn more about the configuration§8: \n§a§n" + documentationURL);
        } else {
            if (title.equals(menu)) {
                openChild(p, item);
            } else {
                modify(p, i, title, click, slot);
            }
        }
        return true;
    }

    private static void modify(SpartanPlayer p, ItemStack item, String title, ClickType click, int slot) {
        ItemMeta meta = item.getItemMeta();
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
                        if (click.isLeftClick()) {
                            value = true;
                            ConfigUtils.set(file, path, value);
                            list.set(2, "§7Value§8:§c " + value);
                        } else if (click.isRightClick()) {
                            value = false;
                            ConfigUtils.set(file, path, value);
                            list.set(2, "§7Value§8:§c " + value);
                        } else {
                            refresh = false;
                        }
                        break;
                    case "Decimal":
                        double dbl = Double.parseDouble(value.toString());

                        if (click.isLeftClick()) {
                            dbl += 0.1;
                            ConfigUtils.set(file, path, dbl);
                            list.set(2, "§7Value§8:§c " + dbl);
                        } else if (click.isRightClick()) {
                            dbl -= 0.1;
                            ConfigUtils.set(file, path, dbl);
                            list.set(2, "§7Value§8:§c " + dbl);
                        } else {
                            refresh = false;
                        }
                        break;
                    case "Number":
                        int num = Integer.parseInt(value.toString());

                        if (click.isLeftClick()) {
                            num += 1;
                            ConfigUtils.set(file, path, num);
                            list.set(2, "§7Value§8:§c " + num);
                        } else if (click.isRightClick()) {
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
                    map.put(p.getUniqueId(), 1);
                    openChild(p, config, slot, list);
                }
            }
        }
    }

    public static void reload(boolean resetChecks) {
        if (!resetChecks) { // Do not run because their cache will be cleared by the Config method
            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearConfigurationCache();
            }
        }
        Settings.clear();
        Messages.clear();
        Compatibility.clear();
        SQLFeature.refreshConfiguration();

        // Always last
        Config.refreshVariables(resetChecks);
    }
}
