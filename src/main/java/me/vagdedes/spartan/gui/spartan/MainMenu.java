package me.vagdedes.spartan.gui.spartan;

import com.vagdedes.filegui.api.FileGUIAPI;
import me.vagdedes.spartan.abstraction.InventoryMenu;
import me.vagdedes.spartan.compatibility.necessary.FileGUI;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import me.vagdedes.spartan.gui.SpartanMenu;
import me.vagdedes.spartan.gui.configuration.ManageConfiguration;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.gui.helpers.PlayerStateLists;
import me.vagdedes.spartan.handlers.connection.DiscordMemberCount;
import me.vagdedes.spartan.handlers.connection.IDs;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.profiling.PlayerReport;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.Permission;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainMenu extends InventoryMenu {

    private static int ecosystemItem = 1;
    private static final ResearchEngine.DataType oppositeObject = SpartanEdition.getOppositeVersion();
    private static final String
            opposite = oppositeObject.toString(),
            offer = "§6Get Plugins for FREE§8: §e§nhttps://www.vagdedes.com/account/viewOffer",
            oppositeVersion = "Spartan: " + opposite + " Edition",
            opposite_1_0_Version = "Spartan 1.0: " + opposite + " Edition",
            opposite_2_0_Version = "Spartan 2.0: " + opposite + " Edition";

    public MainMenu() {
        super(getMenuName(), 54, new Permission[]{Permission.MANAGE, Permission.INFO});
    }

    public static void refresh() {
        if (Config.settings.getBoolean("Important.refresh_inventory_menu")) {
            Runnable runnable = () -> {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    String menuName = getMenuName();

                    for (SpartanPlayer p : players) {
                        SpartanBukkit.runTask(p, () -> {
                            Player n = p.getPlayer();

                            if (n != null) {
                                String title = n.getOpenInventory().getTitle();

                                if (title.startsWith(menuName)) {
                                    SpartanMenu.manageConfiguration.clear();
                                    DiscordMemberCount.ignore();
                                    SpartanMenu.mainMenu.open(p);
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

    public boolean notify(SpartanPlayer player) {
        for (Permission permission : permissions) {
            if (Permissions.has(player, permission)) {
                return AntiCheatUpdates.messageWarnings(player);
            }
        }
        return false;
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        List<String> lore = new ArrayList<>(20);
        UUID uuid = player.getUniqueId();
        int page = PlayerStateLists.getPage(uuid), previousPageSlot = 18, nextPageSlot = 26;
        setTitle(player, getMenuName() + page);

        // Ecosystem
        if (Config.settings.getBoolean(Settings.showEcosystemOption)) {
            ItemStack itemStack = new ItemStack(Material.GOLD_BLOCK);
            itemStack.setAmount(AlgebraUtils.randomInteger(1, 64));
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            ItemMeta meta = itemStack.getItemMeta();
            String name;
            boolean enabled;

            switch (ecosystemItem) {
                case 1:
                    InventoryUtils.prepareDescription(lore, "Compatible Plugin" + (SpartanBukkit.canAdvertise ? " Offer" : ""));
                    name = Compatibility.CompatibilityType.UltimateStatistics.name();
                    lore.add("§7Get Ultimate Stats to improve");
                    lore.add("§7Spartan's legitimate/hacker");
                    lore.add("§7detection.");
                    ecosystemItem = 2;
                    enabled = PluginUtils.exists(name.toLowerCase());
                    break;
                case 2:
                    InventoryUtils.prepareDescription(lore, "Compatible Plugin" + (SpartanBukkit.canAdvertise ? " Offer" : ""));
                    name = Compatibility.CompatibilityType.AntiAltAccount.name();
                    lore.add("§7Get Anti Alt Account to prevent");
                    lore.add("§7global hackers from joining your");
                    lore.add("§7server.");
                    ecosystemItem = 3;
                    enabled = Compatibility.CompatibilityType.AntiAltAccount.isFunctional();
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
                    name = Compatibility.CompatibilityType.FileGUI.name();
                    lore.add("§7Get File GUI to replace Spartan's");
                    lore.add("§7simple configuration menu with");
                    lore.add("§7an advanced configuration menu.");
                    ecosystemItem = SpartanBukkit.canAdvertise ? 4 : 1;
                    enabled = Compatibility.CompatibilityType.FileGUI.isFunctional();
                    break;
            }
            lore.add("");

            if (SpartanBukkit.canAdvertise
                    && !name.equals(oppositeVersion)) {
                lore.add("§aGet " + name + " and receive");

                switch (ecosystemItem) {
                    case 2:
                        lore.add("§a" + Compatibility.CompatibilityType.AntiAltAccount.name() + " & " + Compatibility.CompatibilityType.FileGUI.name());
                        break;
                    case 3:
                        lore.add("§a" + Compatibility.CompatibilityType.UltimateStatistics.name() + " & " + Compatibility.CompatibilityType.FileGUI.name());
                        break;
                    default:
                        lore.add("§a" + Compatibility.CompatibilityType.UltimateStatistics.name() + " & " + Compatibility.CompatibilityType.AntiAltAccount.name());
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
            inventory.setItem(49, itemStack);
        }

        // AntiCheat Updates
        add("§a" + AntiCheatUpdates.name,
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
        add("§aLive Customer Support", lore, supportItem, 47);

        // Configuration
        InventoryUtils.prepareDescription(lore, "Plugin Management");
        Runtime runtime = Runtime.getRuntime();
        List<String> warnings = AntiCheatUpdates.getWarnings(true);

        if (!warnings.isEmpty()) {
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
        double tps = TPS.get(player, false);
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
        add("§aConfiguration", lore, new ItemStack(MaterialUtils.get("crafting_table")), 51);

        // Compatibilities
        InventoryUtils.prepareDescription(lore, "Local Functionality");
        List<Compatibility.CompatibilityType> activeCompatibilities = Config.compatibility.getActiveCompatibilities();
        int activeCompatibilitiesSize = activeCompatibilities.size();

        lore.add("§7Identified§8:§a " + activeCompatibilitiesSize);
        lore.add("§7Total§8:§a " + Config.compatibility.getInactiveCompatibilities().size());

        if (activeCompatibilitiesSize > 0) {
            lore.add("");
            lore.add("§7Compatibilities§8:");

            for (Compatibility.CompatibilityType compatibility : activeCompatibilities) {
                lore.add("§a" + compatibility.toString());
            }
        }
        add("§aCompatibilities", lore, new ItemStack(MaterialUtils.get("enchanting_table")), 52);

        // Player List
        PlayerStateLists.fill(uuid, inventory);

        if (page > 1) {
            add("§cPage " + (page - 1), null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 14),
                    previousPageSlot);
        } else {
            add(PlayerStateLists.inactiveColour + "No Previous Page", null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 14),
                    previousPageSlot);
        }
        if (page < Integer.MAX_VALUE) {
            add("§aPage " + (page + 1), null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5),
                    nextPageSlot);
        } else {
            add(PlayerStateLists.inactiveColour + "No Next Page", null,
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.LIME_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), 1, (short) 5),
                    nextPageSlot);
        }
        return true;
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String name = itemStack.getItemMeta().getDisplayName(),
                item = (name.startsWith("§") ? name.substring(2) : name);

        if (item.equals("Live Customer Support")) {
            player.sendInventoryCloseMessage("");
            player.sendMessage("§6Discord Invite URL§8: §e§n" + DiscordMemberCount.discordURL);
            player.sendMessage("§6Spartan Tutorial§8: §e§nhttps://bit.ly/3Itw7Fd");
            player.sendMessage("");

        } else if (item.equals("Compatibilities")) {
            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                if (Compatibility.CompatibilityType.FileGUI.isFunctional()) {
                    Player n = player.getPlayer();

                    if (n != null && n.isOnline() && n.hasPermission(FileGUI.permission)) {
                        FileGUIAPI.openMenu(n, Config.compatibility.getFile().getPath(), 1);
                    } else {
                        SpartanMenu.manageConfiguration.openChild(player, ManageConfiguration.compatibilityFileName);
                    }
                } else {
                    SpartanMenu.manageConfiguration.openChild(player, ManageConfiguration.compatibilityFileName);
                }
            }

        } else if (item.equals("Configuration")) {
            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                if (clickType == ClickType.LEFT) {
                    SpartanMenu.manageChecks.open(player);
                } else if (clickType == ClickType.RIGHT) {
                    SpartanMenu.manageConfiguration.open(player);
                } else if (clickType.isShiftClick()) {
                    if (!Permissions.has(player, Permission.RELOAD)) {
                        player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
                        return true;
                    } else {
                        Config.reload(player);
                        player.sendInventoryCloseMessage(null);
                    }
                }
            }

        } else if (item.equals(Compatibility.CompatibilityType.UltimateStatistics.name())
                || item.equals(Compatibility.CompatibilityType.AntiAltAccount.name())
                || item.equals(Compatibility.CompatibilityType.FileGUI.name())
                || item.equals(oppositeVersion)) {
            sendOffer(player);
        } else if (item.startsWith("Page")) {
            String[] split = item.split(" ");

            if (split.length == 2) {
                String number = split[1];

                if (AlgebraUtils.validInteger(number)) {
                    UUID uuid = player.getUniqueId();
                    int itemPage = Integer.parseInt(number), currentPage = PlayerStateLists.getPage(uuid);

                    if (itemPage < currentPage) {
                        if (PlayerStateLists.previousPage(uuid)) {
                            DiscordMemberCount.ignore();
                            open(player);
                        }
                    } else if (itemPage > currentPage) {
                        if (PlayerStateLists.nextPage(uuid)) {
                            DiscordMemberCount.ignore();
                            open(player);
                        }
                    }
                }
            }
        } else if (!name.startsWith(PlayerStateLists.inactiveColour)
                && !item.equals(AntiCheatUpdates.name)) {
            List<String> lore = itemStack.getItemMeta().getLore();

            if (lore != null && lore.size() > 1 && lore.get(0).equals(PlayerStateLists.punishedPlayers)) {
                StringBuilder builder = new StringBuilder();

                for (String line : lore) {
                    if (!line.isEmpty()) {
                        builder.append(StringUtils.getClearColorString(line));
                    }
                }
                String reason = builder.toString();

                if (!reason.isEmpty()) {
                    List<PlayerReport> reports = ResearchEngine.getPlayerProfile(item).getPunishmentHistory().getReports();

                    if (!reports.isEmpty()) {
                        for (PlayerReport playerReport : reports) {
                            if (!playerReport.isDismissed() && playerReport.getReason().equals(reason)) {
                                playerReport.dismiss(name, player, true);
                                open(player);
                                break;
                            }
                        }
                    }
                }
            } else {
                SpartanMenu.playerInfo.open(player, false, item);
            }
        }
        return true;
    }

    private void sendOffer(SpartanPlayer player) {
        player.sendInventoryCloseMessage("");

        if (SpartanBukkit.canAdvertise) {
            player.sendImportantMessage("§6" + opposite_1_0_Version + "§8: §e§nhttps://www.vagdedes.com/account/viewProduct/?id=" + SpartanEdition.get1_0_ProductID(oppositeObject));
            player.sendImportantMessage("§6" + opposite_2_0_Version + "§8: §e§nhttps://www.vagdedes.com/account/viewProduct/?id=" + SpartanEdition.get2_0_ProductID(oppositeObject));
            player.sendMessage(offer);
        } else if (IDs.isBuiltByBit()) {
            player.sendMessage("§6" + Compatibility.CompatibilityType.UltimateStatistics.name() + "§8: §e§nhttps://builtbybit.com/resources/12576/");
            player.sendMessage("§6" + Compatibility.CompatibilityType.AntiAltAccount.name() + "§8: §e§nhttps://builtbybit.com/resources/20142/");
            player.sendMessage("§6" + Compatibility.CompatibilityType.FileGUI.name() + "§8: §e§nhttps://builtbybit.com/resources/13185/");
        } else if (IDs.isPolymart()) {
            player.sendMessage("§6" + Compatibility.CompatibilityType.UltimateStatistics.name() + "§8: §e§nhttps://polymart.org/resource/982/");
            player.sendMessage("§6" + Compatibility.CompatibilityType.AntiAltAccount.name() + "§8: §e§nhttps://polymart.org/resource/1096/");
            player.sendMessage("§6" + Compatibility.CompatibilityType.FileGUI.name() + "§8: §e§nhttps://polymart.org/resource/984/");
        } else {
            player.sendMessage("§6" + Compatibility.CompatibilityType.UltimateStatistics.name() + "§8: §e§nhttps://www.spigotmc.org/resources/60868/");
            player.sendMessage("§6" + Compatibility.CompatibilityType.AntiAltAccount.name() + "§8: §e§nhttps://www.spigotmc.org/resources/73105/");
            player.sendMessage("§6" + Compatibility.CompatibilityType.FileGUI.name() + "§8: §e§nhttps://www.spigotmc.org/resources/73893/");
        }
        player.sendMessage("");
    }
}
