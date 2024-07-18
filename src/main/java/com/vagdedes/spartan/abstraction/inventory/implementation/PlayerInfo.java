package com.vagdedes.spartan.abstraction.inventory.implementation;

import com.vagdedes.spartan.abstraction.check.CancelCause;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerEvidence;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        boolean back = !permissionMessage;
        SpartanProtocol target = SpartanBukkit.getProtocol(object.toString());
        boolean isOnline = target != null;
        PlayerProfile profile = isOnline
                ? target.getProfile()
                : ResearchEngine.getPlayerProfile(object.toString());

        if (profile == null) {
            player.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
            return false;
        } else {
            setTitle(player, menu + (isOnline ? target.spartanPlayer.name : profile.getName()));
            Set<Map.Entry<HackType, Double>> evidenceDetails = profile.evidence.getKnowledgeEntries(0.0);
            List<String> lore = new ArrayList<>();
            lore.add("");

            if (!evidenceDetails.isEmpty()) {
                lore.add("§7Certainty of cheating§8:");
                Map<Double, HackType> map = new TreeMap<>(Collections.reverseOrder());

                for (Map.Entry<HackType, Double> entry : evidenceDetails) {
                    map.put(entry.getValue(), entry.getKey());
                }
                for (Map.Entry<Double, HackType> entry : map.entrySet()) {
                    double probability = entry.getKey();

                    if (probability < PlayerEvidence.prevention) {
                        lore.add(
                                "§2" + entry.getValue().getCheck().getName()
                                        + "§8: §a" + AlgebraUtils.integerRound(probability * 100.0) + "%"
                        );
                    } else {
                        lore.add(
                                "§4" + entry.getValue().getCheck().getName()
                                        + "§8: §c" + AlgebraUtils.integerRound(probability * 100.0) + "%"
                        );
                    }
                }
                lore.add("");
            }

            if (isOnline) {
                lore.add("§7CPS (Clicks Per Second)§8:§c " + target.spartanPlayer.clicks.getCount());
                lore.add("§7Latency§8:§c " + target.getPing() + "ms");
                lore.add("§7Edition§8:§c " + target.spartanPlayer.dataType);
                lore.add("");
                lore.add("§eLeft click to reset the player's live violations.");
            }
            lore.add("§cRight click to delete the player's stored data.");
            add(
                    "§c" + profile.getName(),
                    lore,
                    profile.getSkull(true),
                    4
            );
            SpartanPlayer sp = isOnline ? target.spartanPlayer : null;

            for (Enums.HackCategoryType checkType : Enums.HackCategoryType.values()) {
                addChecks(slots[checkType.ordinal()], isOnline, sp, profile, lore, checkType);
            }

            add("§c" + (back ? "Back" : "Close"), null, new ItemStack(Material.ARROW), 40);
            return true;
        }
    }

    private void addChecks(int slot, boolean isOnline,
                           SpartanPlayer player, PlayerProfile playerProfile,
                           List<String> lore, Enums.HackCategoryType checkType) {
        lore.clear();
        lore.add("");
        int violations = 0;
        HackType[] hackTypes = Enums.HackType.values();

        if (isOnline) {
            for (HackType hackType : hackTypes) {
                if (hackType.category == checkType) {
                    violations += player.getViolations(hackType).getLevel();
                }
            }
        }

        // Separator

        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);

        // Separator

        Enums.DataType dataType = isOnline ? player.dataType : null;
        boolean notChecked = isOnline && !PlayerDetectionSlots.isChecked(player.uuid);
        String cancellableCompatibility = isOnline ? player.getCancellableCompatibility() : null;

        for (HackType hackType : hackTypes) {
            if (hackType.category == checkType) {
                violations = isOnline ? player.getViolations(hackType).getLevel() : 0;
                String state = getDetectionState(
                        player,
                        hackType,
                        dataType,
                        cancellableCompatibility,
                        isOnline,
                        notChecked
                );
                lore.add(
                        "§6" + hackType.getCheck().getName()
                                + "§8[§e" + state + "§8]§c "
                                + violations + " violations"
                );
            }
        }
        add("§2" + checkType.toString() + " Checks", lore, item, slot);
    }

    private String getDetectionState(SpartanPlayer player,
                                     HackType hackType,
                                     Enums.DataType dataType,
                                     String cancellableCompatibility,
                                     boolean hasPlayer,
                                     boolean notChecked) {
        if (!hasPlayer) {
            return "Offline";
        }
        if (dataType != null && !SpartanEdition.hasDetectionsPurchased(dataType)) {
            return "Detection Missing";
        }
        String worldName = player.getWorld().getName();
        Check check = hackType.getCheck();

        if (!check.isEnabled(dataType, worldName, null)) { // Do not put player because we calculate it below
            return "Disabled";
        }
        CancelCause disabledCause = player.getViolations(hackType).getDisableCause();
        return Permissions.isBypassing(player, hackType) ? "Permission Bypass" :
                cancellableCompatibility != null ? cancellableCompatibility + " Compatibility" :
                        notChecked ? "Temporarily Not Checked" :
                                disabledCause != null ? "Cancelled (" + disabledCause.getReason() + ")" :
                                        (check.isSilent(dataType, worldName) ? "Silent " : "") + "Checking";
    }

    public void refresh(String targetName) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer o : players) {
                Player no = o.getInstance();

                if (no != null) {
                    InventoryView inventoryView = no.getOpenInventory();

                    if (inventoryView.getTitle().equals(PlayerInfo.menu + targetName)
                            && cooldowns.canDo("player-info=" + o.uuid)) {
                        cooldowns.add("player-info=" + o.uuid, 1);
                        InteractiveInventory.playerInfo.open(o, targetName);
                    }
                }
            }
        }
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;
        String playerName = title.substring(menu.length());

        if (item.equals(playerName)) {
            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                SpartanProtocol t = SpartanBukkit.getProtocol(playerName);

                if (t != null && clickType.isLeftClick()) {
                    for (HackType hackType : Enums.HackType.values()) {
                        t.spartanPlayer.getViolations(hackType).reset();
                    }
                    String message = Config.messages.getColorfulString("player_violation_reset_message").replace("{player}", t.spartanPlayer.name);
                    player.sendMessage(message);
                } else {
                    String name = Bukkit.getOfflinePlayer(playerName).getName();

                    if (name == null) {
                        player.sendMessage(Config.messages.getColorfulString("player_not_found_message"));
                    } else {
                        ResearchEngine.resetData(name);
                        player.sendMessage(Config.messages.getColorfulString("player_stored_data_delete_message").replace("{player}", name));
                    }
                }
                player.sendInventoryCloseMessage(null);
            }
        } else if (item.equals("Close")) {
            player.sendInventoryCloseMessage(null);
        } else if (item.equals("Back")) {
            InteractiveInventory.mainMenu.open(player);
        } else {
            player.sendImportantMessage("§7Click to learn more about the detection states§8: \n§a§n" + documentationURL);
        }
        return true;
    }

}
