package com.vagdedes.spartan.abstraction.inventory.implementation;

import com.vagdedes.filegui.api.FileGUIAPI;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.DiscordMemberCount;
import com.vagdedes.spartan.functionality.connection.IDs;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.inventory.PlayerStateLists;
import com.vagdedes.spartan.functionality.inventory.Summary;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
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

    private static final String name = "§0Spartan AntiCheat | Page ".substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);

    public MainMenu() {
        super(name, 54, new Permission[]{Permission.MANAGE, Permission.INFO});
    }

    public static void refresh() {
        if (Config.settings.getBoolean("Important.refresh_inventory_menu")) {
            Runnable runnable = () -> {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (!players.isEmpty()) {
                    for (SpartanPlayer p : players) {
                        SpartanBukkit.runTask(p, () -> {
                            Player n = p.getPlayer();

                            if (n != null) {
                                String title = n.getOpenInventory().getTitle();

                                if (title.startsWith(name)) {
                                    DiscordMemberCount.ignore();
                                    InteractiveInventory.mainMenu.open(p);
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

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        List<String> lore = new ArrayList<>(20);
        UUID uuid = player.uuid;
        int page = PlayerStateLists.getPage(uuid), previousPageSlot = 18, nextPageSlot = 26;
        setTitle(player, name + page);

        // AntiCheat Updates
        add("§aSummary", Summary.get(), new ItemStack(Material.CHEST), 46);
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

        if (!MultiVersion.unknownFork || !MultiVersion.other) {
            lore.add("§7Server Information§8: §a" + (MultiVersion.unknownFork ? "Version" : MultiVersion.fork())
                    + (MultiVersion.other ? "" : " " + MultiVersion.versionString()));
        }
        double tps = TPS.get(player, false);
        lore.add("§7" + (MultiVersion.folia ? "Region " : "") + "TPS (Ticks Per Second)§8: §a" + AlgebraUtils.cut(tps, 2)
                + " - " + (tps >= TPS.excellent ? "Excellent" : tps >= TPS.good ? "Good" : tps >= TPS.minimum ? "Mediocre" : "Unstable"));
        long maxMemory = runtime.maxMemory();
        lore.add("§7Server Memory Usage§8: §a" + AlgebraUtils.cut(((maxMemory - runtime.freeMemory()) / ((double) maxMemory)) * 100.0, 2) + "%");
        lore.add("§7Detections Available§8: "
                + (SpartanEdition.hasDetectionsPurchased(Enums.DataType.Java) ? "§a" : "§c") + Enums.DataType.Java
                + " §8/ "
                + (SpartanEdition.hasDetectionsPurchased(Enums.DataType.Bedrock) ? "§a" : "§c") + Enums.DataType.Bedrock);
        lore.add("");
        lore.add("§7Left click to §amanage checks§7.");
        lore.add("§7Right click to §creload the plugin's memory contents§7.");
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
            player.sendMessage("");

        } else if (item.equals("Compatibilities")) {
            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                if (Compatibility.CompatibilityType.FileGUI.isFunctional()) {
                    Player n = player.getPlayer();

                    if (n != null && n.isOnline() && n.hasPermission("filegui.modify")) {
                        FileGUIAPI.openMenu(n, Config.compatibility.getFile().getPath(), 1);
                    } else {
                        player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
                    }
                } else {
                    player.sendInventoryCloseMessage(
                            "§7You need §aFileGUI §7to access this feature§8:\n§2"
                                    + (IDs.isBuiltByBit() ? "https://builtbybit.com/resources/13185"
                                    : IDs.isPolymart() ? "https://polymart.org/resource/984"
                                    : "https://www.spigotmc.org/resources/73893")
                    );
                }
            }

        } else if (item.equals("Configuration")) {
            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                if (clickType == ClickType.LEFT) {
                    InteractiveInventory.manageChecks.open(player);
                } else if (clickType == ClickType.RIGHT) {
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
                    UUID uuid = player.uuid;
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
                && !item.equals("Summary")) {
            InteractiveInventory.playerInfo.open(player, false, item);
        }
        return true;
    }
}
