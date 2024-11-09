package com.vagdedes.spartan.abstraction.inventory.implementation;

import com.vagdedes.spartan.abstraction.check.CancelCause;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.command.CommandExecution;
import com.vagdedes.spartan.functionality.connection.DiscordMemberCount;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInfo extends InventoryMenu {

    private static final Cooldowns cooldowns = new Cooldowns(
            new OverflowMap<>(new ConcurrentHashMap<>(), 512)
    );
    private static final String
            menu = ("§0Player Info: ").substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0),
            documentationURL = "https://docs.google.com/document/d/e/2PACX-1vRSwc6vazSE5uCv6pcYkaWsP_RaHTmkU70lBLB9f9tudlSbJZr2ZdRQg3ZGXFtz-QWjDzTQqkSzmMt2/pub";
    private static final int[] slots = new int[]{
            20, 21, 22, 23, 24
    };

    public PlayerInfo() {
        super(menu, 45, new Permission[]{Permission.MANAGE, Permission.INFO});
    }

    @Override
    public boolean internalOpen(SpartanProtocol protocol, boolean permissionMessage, Object object) {
        boolean back = !permissionMessage;
        SpartanProtocol target = SpartanBukkit.getAnyCaseProtocol(object.toString());
        boolean isOnline = target != null;
        PlayerProfile profile = isOnline
                ? target.getProfile()
                : ResearchEngine.getAnyCasePlayerProfile(object.toString());

        if (profile == null) {
            protocol.bukkit.closeInventory();
            ClickableMessage.sendURL(
                    protocol.bukkit,
                    Config.messages.getColorfulString("player_not_found_message"),
                    CommandExecution.support,
                    DiscordMemberCount.discordURL
            );
            return false;
        } else {
            setTitle(protocol, menu + (isOnline ? target.bukkit.getName() : profile.name));
            Set<Map.Entry<HackType, Double>> evidenceDetails = profile.getEvidenceEntries(
                    PlayerEvidence.emptyProbability
            );
            List<String> lore = new ArrayList<>();
            lore.add("");

            if (!evidenceDetails.isEmpty()) {
                lore.add("§7Certainty of cheating§8:");
                Map<Double, HackType> map = PlayerEvidence.POSITIVE
                        ? new TreeMap<>(Collections.reverseOrder())
                        : new TreeMap<>();
                boolean missingData = false;

                for (Map.Entry<HackType, Double> entry : evidenceDetails) {
                    map.put(
                            entry.getValue(),
                            entry.getKey()
                    );

                    if (!missingData
                            && !profile.getExecutor(
                            entry.getKey()
                    ).hasSufficientData(
                            profile.getLastDataType()
                    )) {
                        missingData = true;
                    }
                }
                for (Map.Entry<Double, HackType> entry : map.entrySet()) {
                    HackType hackType = entry.getValue();
                    double probability = entry.getKey();

                    if (PlayerEvidence.surpassedProbability(
                            probability,
                            PlayerEvidence.punishmentProbability
                    )) {
                        probability = PlayerEvidence.probabilityToCertainty(probability);
                        lore.add(
                                "§4" + hackType.getCheck().getName()
                                        + "§8: §c" + AlgebraUtils.integerRound(probability * 100.0) + "%"
                        );
                    } else if (PlayerEvidence.surpassedProbability(
                            probability,
                            PlayerEvidence.preventionProbability
                    )) {
                        probability = PlayerEvidence.probabilityToCertainty(probability);
                        lore.add(
                                "§6" + hackType.getCheck().getName()
                                        + "§8: §e" + AlgebraUtils.integerRound(probability * 100.0) + "%"
                        );
                    } else if (PlayerEvidence.surpassedProbability(
                            probability,
                            PlayerEvidence.notificationProbability
                    )) {
                        probability = PlayerEvidence.probabilityToCertainty(probability);
                        lore.add(
                                "§2" + hackType.getCheck().getName()
                                        + "§8: §a" + AlgebraUtils.integerRound(probability * 100.0) + "%"
                        );
                    } else {
                        probability = PlayerEvidence.probabilityToCertainty(probability);
                        int showProbability = AlgebraUtils.integerRound(probability * 100.0);

                        if (showProbability > 0) {
                            lore.add(
                                    "§3" + hackType.getCheck().getName() + "§8: §b" + showProbability + "%"
                            );
                        }
                    }
                }
                if (missingData) {
                    lore.add("");
                    lore.add("§eSome checks are still collecting data");
                    lore.add("§eand will fully enable in the future.");
                }
                lore.add("");
            }

            if (isOnline) {
                lore.add("§7Version§8:§c " + target.version.toString());
                lore.add("§7CPS (Clicks Per Second)§8:§c " + target.spartan.clicks.getCount());
                lore.add("§7Latency§8:§c " + target.getPing() + "ms");
                lore.add("§7Edition§8:§c " + target.spartan.dataType);
                lore.add("");
            }
            lore.add("§cClick to delete the player's stored data.");
            add(
                    "§c" + profile.name,
                    lore,
                    profile.getSkull(),
                    4
            );
            for (Enums.HackCategoryType checkType : Enums.HackCategoryType.values()) {
                addChecks(slots[checkType.ordinal()], isOnline, target, profile, lore, checkType);
            }

            add("§c" + (back ? "Back" : "Close"), null, new ItemStack(Material.ARROW), 40);
            return true;
        }
    }

    private void addChecks(int slot, boolean isOnline,
                           SpartanProtocol protocol,
                           PlayerProfile profile,
                           List<String> lore, Enums.HackCategoryType checkType) {
        lore.clear();
        lore.add("");

        // Separator

        ItemStack item = new ItemStack(checkType.material);

        // Separator

        for (HackType hackType : Enums.HackType.values()) {
            if (hackType.category == checkType) {
                String state = getDetectionState(
                        protocol,
                        hackType,
                        profile.getLastDataType(),
                        isOnline
                );
                lore.add(
                        "§7" + hackType.getCheck().getName() + "§8:§e " + state
                );
            }
        }
        add("§2" + checkType + " Checks", lore, item, slot);
    }

    private String getDetectionState(SpartanProtocol protocol,
                                     HackType hackType,
                                     Check.DataType dataType,
                                     boolean hasPlayer) {
        if (!hasPlayer) {
            return "Offline";
        }
        if (!SpartanEdition.hasDetectionsPurchased(dataType)) {
            return "Detection Missing";
        }
        String worldName = protocol.spartan.getWorld().getName();
        Check check = hackType.getCheck();

        if (!check.isEnabled(dataType, worldName)) { // Do not put player because we calculate it below
            return "Disabled";
        }
        CancelCause disabledCause = protocol.spartan.getExecutor(hackType).getDisableCause();
        return Permissions.isBypassing(protocol.bukkit, hackType) ? "Permission Bypass" :
                disabledCause != null ? "Cancelled (" + disabledCause.getReason() + ")" :
                        (check.isSilent(dataType, worldName) ? "Silent " : "") + "Checking";
    }

    public void refresh(String targetName) {
        List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

        if (!protocols.isEmpty()) {
            for (SpartanProtocol protocol : protocols) {
                InventoryView inventoryView = protocol.bukkit.getOpenInventory();

                if (inventoryView.getTitle().equals(PlayerInfo.menu + targetName)
                        && cooldowns.canDo("player-info=" + protocol.getUUID())) {
                    cooldowns.add("player-info=" + protocol.getUUID(), 1);
                    InteractiveInventory.playerInfo.open(protocol, targetName);
                }
            }
        }
    }

    @Override
    public boolean internalHandle(SpartanProtocol protocol) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        String playerName = title.substring(menu.length());

        if (item.equalsIgnoreCase(playerName)) {
            if (!Permissions.has(protocol.bukkit, Permission.MANAGE)) {
                protocol.bukkit.closeInventory();
                ClickableMessage.sendURL(
                        protocol.bukkit,
                        Config.messages.getColorfulString("no_permission"),
                        CommandExecution.support,
                        DiscordMemberCount.discordURL
                );
            } else {
                String name = Bukkit.getOfflinePlayer(playerName).getName();

                if (name == null) {
                    ClickableMessage.sendURL(
                            protocol.bukkit,
                            Config.messages.getColorfulString("player_not_found_message"),
                            CommandExecution.support,
                            DiscordMemberCount.discordURL
                    );
                } else {
                    ResearchEngine.resetData(name);
                    protocol.bukkit.sendMessage(Config.messages.getColorfulString("player_stored_data_delete_message").replace("{player}", name));
                }
                protocol.bukkit.closeInventory();
            }
        } else if (item.equals("Close")) {
            protocol.bukkit.closeInventory();
        } else if (item.equals("Back")) {
            InteractiveInventory.mainMenu.open(protocol);
        } else {
            protocol.spartan.sendImportantMessage("§7Click to learn more about the detection states§8: \n§a§n" + documentationURL);
        }
        return true;
    }

}
