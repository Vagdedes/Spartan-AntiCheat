package com.vagdedes.spartan.abstraction.inventory.implementation;

import com.vagdedes.filegui.api.FileGUIAPI;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.functionality.command.CommandExecution;
import com.vagdedes.spartan.functionality.connection.DiscordMemberCount;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.moderation.PlayerStateLists;
import com.vagdedes.spartan.functionality.moderation.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MainMenu extends InventoryMenu {

    private static final String name = Register.pluginName + " AntiCheat (Page ";

    public MainMenu() {
        super(name, 54, new Permission[]{Permission.MANAGE, Permission.INFO});
    }

    public static void refresh() {
        Collection<PlayerProtocol> protocols = PluginBase.getProtocols();

        if (!protocols.isEmpty()) {
            for (PlayerProtocol protocol : protocols) {
                PluginBase.transferTask(protocol, () -> {
                    String title = protocol.bukkit().getOpenInventory().getTitle();

                    if (title.startsWith(name)) {
                        DiscordMemberCount.ignore();
                        PluginBase.mainMenu.open(protocol);
                    }
                });
            }
        }
    }

    @Override
    public boolean internalOpen(PlayerProtocol protocol, boolean permissionMessage, Object object) {
        List<String> lore = new ArrayList<>(20);
        UUID uuid = protocol.getUUID();
        int page = PlayerStateLists.getPage(uuid), previousPageSlot = 18, nextPageSlot = 26;
        setTitle(protocol, name + page + ")");

        // AntiCheat Updates
        int random = AlgebraUtils.randomInteger(1, 64);
        boolean dividedBy2 = random % 2 == 0;

        // Configuration
        InventoryUtils.prepareDescription(lore, "Plugin Management");

        lore.add("§7Packets§8: §a" + (PluginBase.packetsEnabled() ? "Enabled" : "Disabled"));
        lore.add("§7Detections Available§8: "
                + (SpartanEdition.hasDetectionsPurchased(Check.DataType.JAVA) ? "§a" : "§c") + Check.DataType.JAVA
                + " §8/ "
                + (SpartanEdition.hasDetectionsPurchased(Check.DataType.BEDROCK) ? "§a" : "§c") + Check.DataType.BEDROCK);
        Runtime runtime = Runtime.getRuntime();

        lore.add("§7Server Version§8: §a" + MultiVersion.serverVersion.toString());
        long maxMemory = runtime.maxMemory();
        lore.add("§7Server Memory Usage§8: §a" + AlgebraUtils.cut(((maxMemory - runtime.freeMemory()) / ((double) maxMemory)) * 100.0, 2) + "%");
        lore.add("");
        lore.add("§7Click to §amanage checks§7.");
        add("§aManagement", lore, new ItemStack(MaterialUtils.get("crafting_table")), 47);

        // Live Customer Support
        InventoryUtils.prepareDescription(lore, "Discord Community");
        lore.add("§7Get support for your plugins");
        lore.add("§7in our welcoming community.");
        int discordMemberCount = DiscordMemberCount.get();

        if (discordMemberCount > 0) {
            lore.add("");
            lore.add((dividedBy2 ? "§2" : "§6") + discordMemberCount + " online Discord " + (discordMemberCount == 1 ? "member" : "members"));
        }
        add(
                "§aGet Help",
                lore,
                new ItemStack(dividedBy2 ? Material.EMERALD_BLOCK : Material.GOLD_BLOCK, random),
                49
        );

        // Compatibilities
        InventoryUtils.prepareDescription(lore, "Local Functionality");
        List<Compatibility.CompatibilityType> activeCompatibilities = Config.compatibility.getActiveCompatibilities();
        int activeCompatibilitiesSize = activeCompatibilities.size();

        lore.add("§7Identified§8:§a " + activeCompatibilitiesSize);
        lore.add("§7Total§8:§a " + Config.compatibility.getTotalCompatibilities().size());

        if (activeCompatibilitiesSize > 0) {
            lore.add("");
            lore.add("§7Compatibilities§8:");

            for (Compatibility.CompatibilityType compatibility : activeCompatibilities) {
                lore.add("§a" + compatibility.toString());
            }
        }
        add("§aCompatibilities", lore, new ItemStack(MaterialUtils.get("enchanting_table")), 51);

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
    public boolean internalHandle(PlayerProtocol protocol) {
        String name = itemStack.getItemMeta().getDisplayName(),
                item = (name.startsWith("§") ? name.substring(2) : name);

        if (item.equals("Auto Updater")) {
            protocol.bukkitExtra.sendImportantMessage("§6Discord Invite URL§8: §e§n" + DiscordMemberCount.discordURL);

        } else if (item.equals("Compatibilities")) {
            if (!Permissions.has(protocol.bukkit(), Permission.MANAGE)) {
                protocol.bukkit().closeInventory();
                ClickableMessage.sendURL(
                        protocol.bukkit(),
                        Config.messages.getColorfulString("no_permission"),
                        CommandExecution.support,
                        DiscordMemberCount.discordURL
                );
            } else if (Compatibility.CompatibilityType.FILE_GUI.isFunctional()) {
                Player n = protocol.bukkit();

                if (n.hasPermission("filegui.modify")) {
                    FileGUIAPI.openMenu(n, Config.compatibility.getFile().getPath(), 1);
                } else {
                    protocol.bukkit().closeInventory();
                    ClickableMessage.sendURL(
                            protocol.bukkit(),
                            Config.messages.getColorfulString("no_permission"),
                            CommandExecution.support,
                            DiscordMemberCount.discordURL
                    );
                }
            }

        } else if (item.equals("Management")) {
            if (!Permissions.has(protocol.bukkit(), Permission.MANAGE)) {
                protocol.bukkit().closeInventory();
                ClickableMessage.sendURL(
                        protocol.bukkit(),
                        Config.messages.getColorfulString("no_permission"),
                        CommandExecution.support,
                        DiscordMemberCount.discordURL
                );
            } else {
                PluginBase.manageChecks.open(protocol);
            }

        } else if (item.startsWith("Page")) {
            String[] split = item.split(" ");

            if (split.length == 2) {
                String number = split[1];

                if (AlgebraUtils.validInteger(number)) {
                    UUID uuid = protocol.getUUID();
                    int itemPage = Integer.parseInt(number), currentPage = PlayerStateLists.getPage(uuid);

                    if (itemPage < currentPage) {
                        if (PlayerStateLists.previousPage(uuid)) {
                            DiscordMemberCount.ignore();
                            open(protocol);
                        }
                    } else if (itemPage > currentPage) {
                        if (PlayerStateLists.nextPage(uuid)) {
                            DiscordMemberCount.ignore();
                            open(protocol);
                        }
                    }
                }
            }
        } else if (!name.startsWith(PlayerStateLists.inactiveColour)
                && !item.equals("Summary")) {
            PluginBase.playerInfo.open(protocol, false, item);
        }
        return true;
    }

}
