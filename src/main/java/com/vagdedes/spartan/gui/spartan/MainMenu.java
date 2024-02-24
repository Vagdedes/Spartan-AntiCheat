package com.vagdedes.spartan.gui.spartan;

import com.vagdedes.filegui.api.FileGUIAPI;
import com.vagdedes.spartan.abstraction.InventoryMenu;
import com.vagdedes.spartan.compatibility.necessary.FileGUI;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.gui.configuration.ManageConfiguration;
import com.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import com.vagdedes.spartan.gui.helpers.PlayerStateLists;
import com.vagdedes.spartan.handlers.connection.DiscordMemberCount;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.handlers.stability.TestServer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainMenu extends InventoryMenu {

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

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        List<String> lore = new ArrayList<>(20);
        UUID uuid = player.getUniqueId();
        int page = PlayerStateLists.getPage(uuid), previousPageSlot = 18, nextPageSlot = 26;
        setTitle(player, getMenuName() + page);

        // AntiCheat Updates
        add("§a" + AntiCheatUpdates.name,
                AntiCheatUpdates.getInformation(true),
                new ItemStack(Material.CHEST), 46);
        int random = AlgebraUtils.randomInteger(1, 64);
        boolean dividedBy2 = random % 2 == 0;

        // Live Customer Support
        InventoryUtils.prepareDescription(lore, "Available via Discord Server");
        lore.add("§7Download your favorite plugins");
        lore.add("§7from our Discord server and save");
        lore.add("§7time with our Auto Updater.");
        int discordMemberCount = DiscordMemberCount.get();

        if (discordMemberCount > 0) {
            lore.add("");
            lore.add((dividedBy2 ? "§2" : "§6") + discordMemberCount + " online Discord " + (discordMemberCount == 1 ? "member" : "members"));
        }
        ItemStack supportItem = new ItemStack(dividedBy2 ? Material.EMERALD_BLOCK : Material.GOLD_BLOCK, random);
        supportItem.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        add("§aAuto Updater", lore, supportItem, 48);

        // Configuration
        InventoryUtils.prepareDescription(lore, "Plugin Management");
        Runtime runtime = Runtime.getRuntime();
        lore.add("§7Server Type§8: §a" + (TestServer.isIdentified() ? "Testing (" + TestServer.getIdentification() + ")" : "Production"));
        if (!MultiVersion.unknownFork || !MultiVersion.other) {
            lore.add("§7Server Information§8: §a" + (MultiVersion.unknownFork ? "Version" : MultiVersion.fork())
                    + (MultiVersion.other ? "" : " " + MultiVersion.versionString()));
        }
        double tps = TPS.get(player, false);
        lore.add("§7" + (MultiVersion.folia ? "Region " : "") + "TPS (Ticks Per Second)§8: §a" + AlgebraUtils.cut(tps, 2)
                + " - " + (tps >= TPS.excellent ? "Excellent" : tps >= TPS.good ? "Good" : tps >= TPS.minimum ? "Mediocre" : "Unstable"));
        long maxMemory = runtime.maxMemory();
        lore.add("§7Server Memory Usage§8: §a" + AlgebraUtils.cut(((maxMemory - runtime.freeMemory()) / ((double) maxMemory)) * 100.0, 2) + "%");
        lore.add("§7Research Engine§8: §a" + ResearchEngine.getProgress().logs + " Logs" + (ResearchEngine.isFull() ? " §c(Maxed Out)" : ""));

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
        add("§aConfiguration", lore, new ItemStack(MaterialUtils.get("crafting_table")), 50);

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

        if (item.equals("Auto Updater")) {
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
            SpartanMenu.playerInfo.open(player, false, item);
        }
        return true;
    }
}
