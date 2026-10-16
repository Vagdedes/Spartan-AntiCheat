package ai.idealistic.spartan.abstraction.inventory.implementation;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.check.CheckCancellation;
import ai.idealistic.spartan.abstraction.check.CheckEnums;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.data.Cooldowns;
import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.profiling.PlayerProfile;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.Permission;
import ai.idealistic.spartan.compatibility.Compatibility;
import ai.idealistic.spartan.functionality.command.CommandExecution;
import ai.idealistic.spartan.functionality.connection.DiscordServer;
import ai.idealistic.spartan.functionality.moderation.clickable.ClickableMessage;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.Permissions;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.tracking.LimitedEdition;
import ai.idealistic.spartan.functionality.tracking.ResearchEngine;
import ai.idealistic.spartan.utils.java.OverflowMap;
import ai.idealistic.spartan.utils.java.TimeUtils;
import ai.idealistic.spartan.utils.minecraft.inventory.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInfo extends InventoryMenu {

    public static final Permission[] permissions = new Permission[]{Permission.MANAGE, Permission.INFO};

    private static final Cooldowns cooldowns = new Cooldowns(
            new OverflowMap<>(new ConcurrentHashMap<>(), 512)
    );
    private static final String menu = "Player Info: ";
    private static final int[] slots = new int[]{
            20, 21, 23, 24
    };

    public PlayerInfo() {
        super(menu, 45, permissions);
    }

    @Override
    public boolean internalOpen(PlayerProtocol protocol, boolean permissionMessage, Object object) {
        PlayerProtocol target = PluginBase.getAnyCaseProtocol(object.toString());
        boolean isOnline = target != null;
        PlayerProfile profile = isOnline
                ? target.profile()
                : ResearchEngine.getAnyCasePlayerProfile(object.toString());

        if (profile == null) {
            protocol.bukkit().closeInventory();
            ClickableMessage.sendURL(
                    protocol.bukkit(),
                    Config.messages.getColorfulString("player_not_found_message"),
                    CommandExecution.support,
                    DiscordServer.url
            );
            return false;
        } else {
            setTitle(protocol, menu + (isOnline ? target.bukkit().getName() : profile.name));
            List<String> lore = new ArrayList<>();
            lore.add("");

            if (isOnline) {
                lore.add("§7Version§8:§c " + target.version.toString());
                lore.add("§7Latency§8:§c " + target.getPing() + "ms");
                lore.add("§7Edition§8:§c " + target.getDataType());
            } else {
                lore.add("§7Last Known Edition§8:§c " + profile.getLastDataType());
            }
            long time = profile.getContinuity().getOnlineTime();

            if (time > 0L) {
                lore.add("§7Total Active Time§8:§c " + TimeUtils.convertMilliseconds(time));
            }
            lore.add("");
            lore.add("§cClick to delete the player's stored data.");
            add(
                    "§c" + profile.name,
                    lore,
                    profile.getSkull(),
                    4
            );
            for (CheckEnums.HackCategoryType checkType : CheckEnums.HackCategoryType.values()) {
                addChecks(slots[checkType.ordinal()], isOnline, target, lore, checkType);
            }

            // Separator

            lore.clear();
            lore.add("");
            lore.add("§7Click to join our Discord community!");
            add("§aDiscord", lore, new ItemStack(MaterialUtils.get("crafting_table")), 39);

            // Separator

            add("§cClose", null, new ItemStack(Material.ARROW), 40);

            // Separator

            List<Compatibility.CompatibilityType> activeCompatibilities = Config.compatibility.getActiveCompatibilities();
            int activeCompatibilitiesSize = activeCompatibilities.size();

            lore.clear();
            lore.add("");
            lore.add("§7Identified§8:§a " + activeCompatibilitiesSize);
            lore.add("§7Total§8:§a " + Config.compatibility.getTotalCompatibilities().size());

            if (activeCompatibilitiesSize > 0) {
                lore.add("");
                lore.add("§7Compatibilities§8:");

                for (Compatibility.CompatibilityType compatibility : activeCompatibilities) {
                    lore.add("§a" + compatibility.toString());
                }
            }
            add("§aCompatibilities", lore, new ItemStack(MaterialUtils.get("enchanting_table")), 41);
            return true;
        }
    }

    private void addChecks(int slot,
                           boolean isOnline,
                           PlayerProtocol protocol,
                           List<String> lore,
                           CheckEnums.HackCategoryType checkType) {
        lore.clear();
        ItemStack item = new ItemStack(checkType.material);

        // Separator

        boolean added = false;

        for (HackType hackType : HackType.values()) {
            if (hackType.category == checkType) {
                String state = getDetectionNotification(
                        protocol,
                        hackType,
                        isOnline
                );

                if (state != null) {
                    if (!added) {
                        added = true;
                        lore.add("");
                    }
                    lore.add("§7" + hackType.getCheck().getName() + "§8:§f " + state);
                } else {
                    List<String> evidence = protocol.getRunner(hackType).getEvidence();

                    if (!evidence.isEmpty()) {
                        if (!added) {
                            added = true;
                            lore.add("");
                        }
                        lore.add(
                                "§4" + hackType.getCheck().getName()
                                        + "§8: §c" + evidence.size()
                                        + (evidence.size() == 1
                                        ? " suspicion"
                                        : " suspicions")
                        );
                    }
                }
            }
        }

        if (!added) {
            lore.add("");
            lore.add("§7No useful information currently available.");
        }

        // Separator

        add("§2" + checkType + " checks", lore, item, slot);
    }

    private String getDetectionNotification(PlayerProtocol protocol,
                                            HackType hackType,
                                            boolean hasPlayer) {
        if (!hasPlayer) {
            return "Player is offline";
        }
        CheckCancellation disabledCause = protocol.getRunner(hackType).getDisableCause(true);
        return Permissions.isBypassing(protocol.bukkit(), hackType)
                ? "Player has permission bypass"
                : disabledCause != null
                ? "Custom: " + disabledCause.getReason()
                : !LimitedEdition.canRun()
                ? LimitedEdition.message
                : null;
    }

    public void refresh(String targetName) {
        Collection<PlayerProtocol> protocols = PluginBase.getProtocols();

        if (!protocols.isEmpty()) {
            String expectedTitle = PlayerInfo.menu + targetName;

            for (PlayerProtocol protocol : protocols) {
                if (cooldowns.canDo("player-info=" + protocol.getUUID())) {
                    PluginBase.runTask(protocol, () -> {
                        if (protocol.getOpenInventoryTitle("").equals(expectedTitle)) {
                            cooldowns.add(
                                    "player-info=" + protocol.getUUID(),
                                    10
                            );
                            PluginBase.playerInfo.open(protocol, targetName);
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean internalHandle(PlayerProtocol protocol) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        String playerName = title.substring(menu.length());

        if (item.equalsIgnoreCase(playerName)) {
            if (!Permissions.has(protocol.bukkit(), Permission.MANAGE)) {
                protocol.bukkit().closeInventory();
                ClickableMessage.sendURL(
                        protocol.bukkit(),
                        Config.messages.getColorfulString("no_permission"),
                        CommandExecution.support,
                        DiscordServer.url
                );
            } else {
                String name = Bukkit.getOfflinePlayer(playerName).getName();

                if (name == null) {
                    ClickableMessage.sendURL(
                            protocol.bukkit(),
                            Config.messages.getColorfulString("player_not_found_message"),
                            CommandExecution.support,
                            DiscordServer.url
                    );
                } else {
                    ResearchEngine.resetData(name);
                    protocol.bukkit().sendMessage(Config.messages.getColorfulString("player_stored_data_delete_message").replace("{player}", name));
                }
                protocol.bukkit().closeInventory();
            }
        } else if (item.equals("Close")) {
            protocol.bukkit().closeInventory();
        } else if (item.equals("Discord")) {
            protocol.sendImportantMessage("§2§l" + DiscordServer.url);
            protocol.bukkit().closeInventory();
        } else if (Register.isFreeEdition()) {
            protocol.sendImportantMessage("§2§l" + Register.pluginURL);
            protocol.bukkit().closeInventory();
        }
        return true;
    }

}
