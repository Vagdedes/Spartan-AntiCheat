package me.vagdedes.spartan.gui.spartan;

import com.vagdedes.filegui.api.FileGUIAPI;
import me.vagdedes.spartan.compatibility.necessary.AntiAltAccount;
import me.vagdedes.spartan.compatibility.necessary.FileGUI;
import me.vagdedes.spartan.compatibility.necessary.UltimateStatistics;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.gui.configuration.ManageChecks;
import me.vagdedes.spartan.gui.configuration.ManageConfiguration;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.gui.helpers.PlayerStateLists;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.handlers.connection.DiscordMemberCount;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.profiling.PlayerReport;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.system.IDs;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpartanMenu {

    private static int ecosystemItem = 1;
    private static final String
            opposite = SpartanEdition.getOppositeVersion().toString(),
            offer = "§6Get Plugins for FREE§8: §e§nhttps://vagdedes.com/account/viewOffer",
            oppositeVersion = "Spartan: " + opposite + " Edition";

    public static void refresh() {
        if (Settings.getBoolean("Important.refresh_inventory_menu")) {
            Runnable runnable = () -> {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (players.size() > 0) {
                    String menuName = getMenuName();

                    for (SpartanPlayer p : players) {
                        SpartanBukkit.runTask(p, () -> {
                            Player n = p.getPlayer();

                            if (n != null) {
                                String title = n.getOpenInventory().getTitle();

                                if (title.startsWith(menuName)) {
                                    ManageConfiguration.clear();
                                    DiscordMemberCount.ignore();
                                    open(p);
                                }
                            }
                        });
                    }
                }
            };

            if (SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.transferTask(runnable);
            }
        }
    }

    private static String getMenuName() {
        String name;
        ResearchEngine.DataType missingDetection = SpartanEdition.getMissingDetection();

        if (missingDetection != null) {
            switch (missingDetection) {
                case Java:
                    name = "Spartan for " + ResearchEngine.DataType.Bedrock;
                    break;
                case Bedrock:
                    name = "Spartan " + ResearchEngine.DataType.Java + " Edition";
                    break;
                default:
                    name = "Spartan AntiCheat";
                    break;
            }
        } else {
            name = "Spartan AntiCheat";
        }
        return ("§0" + name + " | Page ").substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);
    }

    public static boolean notify(SpartanPlayer p) {
        return hasPermission(p) && AntiCheatUpdates.messageWarnings(p);
    }

    public static boolean hasPermission(SpartanPlayer p) {
        return Permissions.has(p, Permission.MANAGE) || Permissions.has(p, Permission.INFO);
    }

    public static boolean open(SpartanPlayer p) {
        return open(p, true);
    }

    public static boolean open(SpartanPlayer p, boolean permissionMessage) {
        if (!hasPermission(p)) {
            if (permissionMessage) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            }
            return false;
        }
        List<String> lore = new ArrayList<>(20);
        UUID uuid = p.getUniqueId();
        int page = PlayerStateLists.getPage(uuid), previousPageSlot = 18, nextPageSlot = 26;
        Inventory inv = p.createInventory(54, getMenuName() + page);

        // Ecosystem
        if (Settings.getBoolean(Settings.showEcosystemOption)) {
            ItemStack itemStack = new ItemStack(Material.GOLD_BLOCK);
            itemStack.setAmount(AlgebraUtils.randomInteger(1, 64));
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            ItemMeta meta = itemStack.getItemMeta();
            String name;
            boolean enabled;

            switch (ecosystemItem) {
                case 1:
                    InventoryUtils.prepareDescription(lore, "Compatible Plugin" + (SpartanBukkit.canAdvertise ? " Offer" : ""));
                    name = UltimateStatistics.name;
                    lore.add("§7Get Ultimate Stats to improve");
                    lore.add("§7Spartan's legitimate/hacker");
                    lore.add("§7detection.");
                    ecosystemItem = 2;
                    enabled = PluginUtils.exists(name.toLowerCase());
                    break;
                case 2:
                    InventoryUtils.prepareDescription(lore, "Compatible Plugin" + (SpartanBukkit.canAdvertise ? " Offer" : ""));
                    name = AntiAltAccount.name;
                    lore.add("§7Get Anti Alt Account to prevent");
                    lore.add("§7global hackers from joining your");
                    lore.add("§7server.");
                    ecosystemItem = 3;
                    enabled = AntiAltAccount.isEnabled();
                    break;
                case 4:
                    InventoryUtils.prepareDescription(lore, opposite + " Detections");
                    name = oppositeVersion;
                    lore.add("§7Get the full version of the");
                    lore.add("§7Spartan AntiCheat and enjoy");
                    lore.add("§7" + SpartanEdition.getVersion() + " & " + opposite + " detections.");
                    ecosystemItem = 1;
                    enabled = SpartanEdition.getMissingDetection() == null;
                    break;
                default:
                    InventoryUtils.prepareDescription(lore, "Compatible Plugin" + (SpartanBukkit.canAdvertise ? " Offer" : ""));
                    name = FileGUI.name;
                    lore.add("§7Get File GUI to replace Spartan's");
                    lore.add("§7simple configuration menu with");
                    lore.add("§7an advanced configuration menu.");
                    ecosystemItem = SpartanBukkit.canAdvertise ? 4 : 1;
                    enabled = FileGUI.isEnabled();
                    break;
            }
            lore.add("");

            if (SpartanBukkit.canAdvertise && !name.equals(oppositeVersion)) {
                lore.add("§aGet " + name + " and receive");

                switch (ecosystemItem) {
                    case 2:
                        lore.add("§a" + AntiAltAccount.name + " & " + FileGUI.name);
                        break;
                    case 3:
                        lore.add("§a" + UltimateStatistics.name + " & " + FileGUI.name);
                        break;
                    default:
                        lore.add("§a" + UltimateStatistics.name + " & " + AntiAltAccount.name);
                        break;
                }
                lore.add("§acompletely for FREE");
                lore.add("");
            }

            if (enabled) {
                lore.add("§2Enabled");
            } else {
                lore.add("§2Click to access.");
            }
            meta.setLore(lore);
            meta.setDisplayName("§3" + name);
            itemStack.setItemMeta(meta);
            inv.setItem(49, itemStack);
        }

        // AntiCheat Updates
        InventoryUtils.add(inv, "§a" + AntiCheatUpdates.name,
                AntiCheatUpdates.getInformation(true),
                new ItemStack(Material.CHEST), 46);

        // Live Customer Support
        InventoryUtils.prepareDescription(lore, "Official Discord Server");
        int discordMemberCount = DiscordMemberCount.get();

        if (discordMemberCount > 0) {
            lore.add("§7Click this item to §ajoin " + discordMemberCount + " online Discord " + (discordMemberCount == 1 ? "member" : "members") + "§7.");
        } else {
            lore.add("§7Click this item to §ajoin the Discord server§7.");
        }
        ItemStack supportItem = new ItemStack(Material.EMERALD, Math.max(Math.min(discordMemberCount, 64), 1));
        supportItem.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        InventoryUtils.add(inv, "§aLive Customer Support", lore, supportItem, 47);

        // Configuration
        InventoryUtils.prepareDescription(lore, "Plugin Management");
        Runtime runtime = Runtime.getRuntime();
        List<String> warnings = AntiCheatUpdates.getWarnings(true);

        if (warnings.size() > 0) {
            lore.add("§4Warning§8:");

            for (String warning : warnings) {
                lore.add("§c" + warning);
            }
            lore.add("");
        }
        lore.add("§7Server Type§8: §a" + (TestServer.isIdentified() ? "Testing (" + TestServer.getIdentification() + ")" : SpartanBukkit.isProductionServer() ? "Production" : "Vanilla"));
        if (!MultiVersion.unknownFork || !MultiVersion.other) {
            lore.add("§7Server Information§8: §a" + (MultiVersion.unknownFork ? "Version" : MultiVersion.fork())
                    + (MultiVersion.other ? "" : " " + MultiVersion.versionString()));
        }
        double tps = TPS.get(p, false);
        lore.add("§7" + (MultiVersion.folia ? "Region " : "") + "TPS (Ticks Per Second)§8: §a" + AlgebraUtils.cut(tps, 2)
                + " - " + (tps >= TPS.excellent ? "Excellent" : tps >= TPS.good ? "Good" : tps >= TPS.minimum ? "Mediocre" : "Unstable"));
        long maxMemory = runtime.maxMemory();
        lore.add("§7Server Memory Usage§8: §a" + AlgebraUtils.cut(((maxMemory - runtime.freeMemory()) / ((double) maxMemory)) * 100.0, 2) + "%");
        lore.add("§7Research Engine§8: §a" +
                (ResearchEngine.isCaching() ? "Calculating Data..." :
                        ResearchEngine.getProgress().getLogs() + " Logs §8/ §a" + ResearchEngine.getFights().size() + " Fights") + (ResearchEngine.isFull() ? " §c(Maxed Out)" : ""));

        if (SpartanBukkit.canAdvertise) {
            boolean preview = IDs.isPreview();
            lore.add("§7Detections Available§8: "
                    + (!preview && SpartanEdition.hasDetectionsPurchased(ResearchEngine.DataType.Java) ? "§a" : "§c") + ResearchEngine.DataType.Java
                    + " §8/ "
                    + (!preview && SpartanEdition.hasDetectionsPurchased(ResearchEngine.DataType.Bedrock) ? "§a" : "§c") + ResearchEngine.DataType.Bedrock);
        }
        lore.add("");
        lore.add("§7Left click to §amanage checks§7.");
        lore.add("§7Right click to §emanage configurations§7.");
        lore.add("§7Shift click to §creload the plugin's memory contents§7.");
        InventoryUtils.add(inv, "§aConfiguration", lore, new ItemStack(MaterialUtils.get("crafting_table")), 51);

        // Compatibilities
        InventoryUtils.prepareDescription(lore, "Local Functionality");
        List<Compatibility.CompatibilityType> activeCompatibilities = Compatibility.getActiveCompatibilities();
        int activeCompatibilitiesSize = activeCompatibilities.size();

        lore.add("§7Identified§8:§a " + activeCompatibilitiesSize);
        lore.add("§7Total§8:§a " + Compatibility.getInactiveCompatibilities().size());

        if (activeCompatibilitiesSize > 0) {
            lore.add("");
            lore.add("§7Compatibilities§8:");

            for (Compatibility.CompatibilityType compatibility : activeCompatibilities) {
                lore.add("§a" + compatibility.toString());
            }
        }
        InventoryUtils.add(inv, "§aCompatibilities", lore, new ItemStack(MaterialUtils.get("enchanting_table")), 52);

        // Player List
        PlayerStateLists.fill(uuid, inv);

        if (page > 1) {
            InventoryUtils.add(inv, "§cPage " + (page - 1), null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 14),
                    previousPageSlot);
        } else {
            InventoryUtils.add(inv, PlayerStateLists.inactiveColour + "No Previous Page", null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 14),
                    previousPageSlot);
        }
        if (page < Integer.MAX_VALUE) {
            InventoryUtils.add(inv, "§aPage " + (page + 1), null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5),
                    nextPageSlot);
        } else {
            InventoryUtils.add(inv, PlayerStateLists.inactiveColour + "No Next Page", null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5),
                    nextPageSlot);
        }
        p.openInventory(inv);
        return true;
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title, ClickType clickType) {
        if (!title.startsWith(getMenuName())) {
            return false;
        }
        String name = i.getItemMeta().getDisplayName(),
                item = (name.startsWith("§") ? name.substring(2) : name);

        if (item.equals("Live Customer Support")) {
            p.sendInventoryCloseMessage("");
            p.sendMessage("§6Discord Invite URL§8: §e§n" + DiscordMemberCount.discordURL);
            p.sendMessage("§6Spartan Tutorial§8: §e§nhttps://bit.ly/3Itw7Fd");
            p.sendMessage("");

        } else if (item.equals("Compatibilities")) {
            if (!Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            } else {
                if (FileGUI.isEnabled()) {
                    Player n = p.getPlayer();

                    if (n != null && n.isOnline() && n.hasPermission(FileGUI.permission)) {
                        FileGUIAPI.openMenu(n, Compatibility.getFile().getPath(), 1);
                    } else {
                        ManageConfiguration.openChild(p, ManageConfiguration.compatibilityFileName);
                    }
                } else {
                    ManageConfiguration.openChild(p, ManageConfiguration.compatibilityFileName);
                }
            }

        } else if (item.equals("Configuration")) {
            if (!Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            } else {
                if (clickType == ClickType.LEFT) {
                    ManageChecks.open(p);
                } else if (clickType == ClickType.RIGHT) {
                    ManageConfiguration.open(p);
                } else if (clickType.isShiftClick()) {
                    if (!Permissions.has(p, Permission.RELOAD)) {
                        p.sendInventoryCloseMessage(Messages.get("no_permission"));
                        return true;
                    } else {
                        Config.reload(p);
                        p.sendInventoryCloseMessage(null);
                    }
                }
            }

        } else if (item.equals(UltimateStatistics.name)
                || item.equals(AntiAltAccount.name)
                || item.equals(FileGUI.name)
                || item.equals(oppositeVersion)) {
            sendOffer(p);
        } else if (item.startsWith("Page")) {
            String[] split = item.split(" ");

            if (split.length == 2) {
                String number = split[1];

                if (AlgebraUtils.validInteger(number)) {
                    UUID uuid = p.getUniqueId();
                    int itemPage = Integer.parseInt(number), currentPage = PlayerStateLists.getPage(uuid);

                    if (itemPage < currentPage) {
                        if (PlayerStateLists.previousPage(uuid)) {
                            DiscordMemberCount.ignore();
                            open(p);
                        }
                    } else if (itemPage > currentPage) {
                        if (PlayerStateLists.nextPage(uuid)) {
                            DiscordMemberCount.ignore();
                            open(p);
                        }
                    }
                }
            }
        } else if (!name.startsWith(PlayerStateLists.inactiveColour)
                && !item.equals(AntiCheatUpdates.name)) {
            List<String> lore = i.getItemMeta().getLore();

            if (lore != null && lore.size() > 1 && lore.get(0).equals(PlayerStateLists.punishedPlayers)) {
                StringBuilder builder = new StringBuilder();

                for (String line : lore) {
                    if (line.length() > 0) {
                        builder.append(StringUtils.getClearColorString(line));
                    }
                }
                String reason = builder.toString();

                if (reason.length() > 0) {
                    List<PlayerReport> reports = ResearchEngine.getPlayerProfile(item).getPunishmentHistory().getReports();

                    if (reports.size() > 0) {
                        for (PlayerReport playerReport : reports) {
                            if (!playerReport.isDismissed() && playerReport.getReason().equals(reason)) {
                                playerReport.dismiss(name, p, true);
                                open(p);
                                break;
                            }
                        }
                    }
                }
            } else {
                PlayerInfo.open(p, item, true);
            }
        }
        return true;
    }

    private static void sendOffer(SpartanPlayer p) {
        p.sendInventoryCloseMessage("");

        if (SpartanBukkit.canAdvertise) {
            ResearchEngine.DataType opposite = SpartanEdition.getOppositeVersion();

            switch (opposite) {
                case Java:
                case Bedrock:
                    p.sendImportantMessage("§6" + oppositeVersion + "§8: §e§nhttps://vagdedes.com/account/viewProduct/?id=" + SpartanEdition.getProductID(opposite));
                    break;
                default:
                    break;
            }
            p.sendMessage(offer);
        } else if (IDs.isBuiltByBit()) {
            p.sendMessage("§6" + UltimateStatistics.name + "§8: §e§nhttps://builtbybit.com/resources/12576/");
            p.sendMessage("§6" + AntiAltAccount.name + "§8: §e§nhttps://builtbybit.com/resources/20142/");
            p.sendMessage("§6" + FileGUI.name + "§8: §e§nhttps://builtbybit.com/resources/13185/");
        } else if (IDs.isPolymart()) {
            p.sendMessage("§6" + UltimateStatistics.name + "§8: §e§nhttps://polymart.org/resource/982/");
            p.sendMessage("§6" + AntiAltAccount.name + "§8: §e§nhttps://polymart.org/resource/1096/");
            p.sendMessage("§6" + FileGUI.name + "§8: §e§nhttps://polymart.org/resource/984/");
        } else {
            p.sendMessage("§6" + UltimateStatistics.name + "§8: §e§nhttps://www.spigotmc.org/resources/60868/");
            p.sendMessage("§6" + AntiAltAccount.name + "§8: §e§nhttps://www.spigotmc.org/resources/73105/");
            p.sendMessage("§6" + FileGUI.name + "§8: §e§nhttps://www.spigotmc.org/resources/73893/");
        }
        p.sendMessage("");
    }
}
