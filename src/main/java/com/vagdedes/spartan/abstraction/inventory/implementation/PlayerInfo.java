package com.vagdedes.spartan.abstraction.inventory.implementation;

import com.vagdedes.spartan.abstraction.check.CancelCause;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.inventory.InventoryMenu;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PunishmentHistory;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.inventory.PlayerStateLists;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerInfo extends InventoryMenu {

    private static final String menu = ("§0Player Info: ").substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);
    private static final String documentationURL = "https://docs.google.com/document/d/e/2PACX-1vRSwc6vazSE5uCv6pcYkaWsP_RaHTmkU70lBLB9f9tudlSbJZr2ZdRQg3ZGXFtz-QWjDzTQqkSzmMt2/pub";
    private static final Cooldowns cooldowns = new Cooldowns(null);

    public PlayerInfo() {
        super(menu, 36, new Permission[]{Permission.MANAGE, Permission.INFO});
    }

    @Override
    public boolean internalOpen(SpartanPlayer player, boolean permissionMessage, Object object) {
        boolean back = !permissionMessage;
        SpartanPlayer target = SpartanBukkit.getPlayer(object.toString());
        boolean isOnline = target != null;
        PlayerProfile playerProfile = isOnline ? target.getProfile() : ResearchEngine.getPlayerProfileAdvanced(object.toString(), true);

        if (playerProfile == null) {
            player.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
            return false;
        } else {
            setTitle(player, menu + (isOnline ? target.name : playerProfile.getName()));
            List<String> lore = new ArrayList<>(20);

            int i = 10;

            for (Enums.HackCategoryType checkType : Enums.HackCategoryType.values()) {
                if (checkType == Enums.HackCategoryType.EXPLOITS) {
                    addCheck(HackType.Exploits, 15, isOnline, target, playerProfile, lore);
                } else {
                    addChecks(i, isOnline, target, playerProfile, lore, checkType);
                    i++;
                }
            }

            lore.clear();
            lore.add("");
            boolean added = false;

            for (MiningHistory history : playerProfile.getMiningHistory()) {
                int mines = history.getMines();

                if (mines > 0) {
                    added = true;
                    String ore = history.ore.toString();
                    int days = history.getDays();
                    lore.add("§c" + mines + " §7" + ore + (mines == 1 ? "" : (ore.endsWith("s") ? "es" : "s"))
                            + " in §c" + days + " §7" + (days == 1 ? "day" : "days"));
                }
            }
            if (!added) {
                lore.add("§c" + PlayerStateLists.noDataAvailable);
            }
            int miningHistoryMines = playerProfile.getOverallMiningHistory().getMines(),
                    information = 29,
                    reset = 33;
            ItemStack item = miningHistoryMines > 0 ?
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(miningHistoryMines, 64), (short) 14) :
                    new ItemStack(Material.QUARTZ_BLOCK);
            add("§2Mining History", lore, item, 16);

            PunishmentHistory punishmentHistory = playerProfile.punishmentHistory;
            lore.clear();
            lore.add("");
            lore.add("§7Warnings§8:§c " + punishmentHistory.getWarnings());
            lore.add("§7Kicks§8:§c " + punishmentHistory.getKicks());

            if (isOnline) {
                lore.add("§7CPS (Clicks Per Second)§8:§a " + target.getClicks().getCount());
                lore.add("§7Player Latency§8:§a " + target.getPing() + "ms");
                lore.add("§7Player State§8:§a " + playerProfile.evidence.getType().name + " (" + target.dataType.name + ")");
                add("§2Information", lore, new ItemStack(Material.BOOK, 1), information);

                lore.clear();
                lore.add("");
                lore.add("§eLeft click to reset the player's live violations.");
                lore.add("§cRight click to delete the player's stored data.");
                add("§4Reset", lore, new ItemStack(Material.REDSTONE), reset);
            } else {
                lore.add("§7Player State§8:§a " + playerProfile.evidence.getType().name + " (" + playerProfile.getDataType().name + ")");
                add("§2Information", lore, new ItemStack(Material.BOOK), information);

                lore.clear();
                lore.add("");
                lore.add("§cClick to delete the player's stored data.");
                add("§4Reset", lore, new ItemStack(Material.REDSTONE), reset);
            }
            add("§c" + (back ? "Back" : "Close"), null, new ItemStack(Material.ARROW), 31);
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
                    violations += player.getViolations(hackType).getTotalLevel();
                }
            }
        }

        // Separator

        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);

        // Separator

        Enums.DataType dataType = isOnline ? player.dataType : playerProfile.getDataType();
        boolean serverLag = isOnline && TPS.areLow(player),
                notChecked = isOnline && !MaximumCheckedPlayers.isChecked(player.uuid),
                detectionsNotAvailable = !SpartanEdition.hasDetectionsPurchased(dataType),
                listedChecks = false;
        String cancellableCompatibility = isOnline ? player.getCancellableCompatibility() : null;

        for (HackType hackType : hackTypes) {
            if (hackType.category == checkType) {
                violations = isOnline ? player.getViolations(hackType).getTotalLevel() : 0;
                boolean hasViolations = violations > 0,
                        hasData = playerProfile.evidence.has(hackType, true);
                String state = getDetectionState(
                        player,
                        hackType,
                        dataType,
                        cancellableCompatibility,
                        isOnline,
                        serverLag,
                        notChecked,
                        detectionsNotAvailable,
                        !hasViolations && !hasData
                );

                if (hasViolations || hasData || state != null) {
                    String color = "§c";
                    String data = playerProfile.evidence.getKnowledge(hackType, color, true);
                    listedChecks = true;
                    lore.add("§6" + hackType.getCheck().getName() + "§8[§e" + state + "§8] " + color
                            + (hasData ? data : hasViolations ? violations + " " + (violations == 1 ? "violation" : "violations") : "")
                    );
                }
            }
        }
        if (!listedChecks) {
            lore.add("§c" + PlayerStateLists.noDataAvailable);
        }
        add("§2" + checkType.toString() + " Checks", lore, item, slot);
    }

    private void addCheck(HackType hackType, int slot, boolean isOnline,
                          SpartanPlayer player, PlayerProfile playerProfile,
                          List<String> lore) {
        lore.clear();
        lore.add("");
        int violations = !isOnline ? 0 : player.getViolations(hackType).getTotalLevel();
        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);
        boolean hasViolations = violations > 0,
                hasData = playerProfile.evidence.has(hackType, true);
        Enums.DataType dataType = isOnline ? player.dataType : playerProfile.getDataType();
        String state = getDetectionState(player,
                hackType,
                dataType,
                isOnline ? player.getCancellableCompatibility() : null,
                isOnline,
                isOnline && TPS.areLow(player),
                isOnline && !MaximumCheckedPlayers.isChecked(player.uuid),
                !SpartanEdition.hasDetectionsPurchased(dataType),
                !hasViolations && !hasData);

        if (hasViolations || hasData || state != null) {
            lore.add("§a" + violations + " §7violations");
            lore.add("§e" + state);

            if (hasData) {
                String color = "§c";
                lore.add(color + playerProfile.evidence.getKnowledge(hackType, color, true));
            }
        } else {
            lore.add("§c" + PlayerStateLists.noDataAvailable);
        }
        add("§2" + hackType.getCheck().getName() + " Check", lore, item, slot);
    }

    private String getDetectionState(SpartanPlayer player, HackType hackType, Enums.DataType dataType,
                                     String cancellableCompatibility,
                                     boolean hasPlayer,
                                     boolean serverLag,
                                     boolean notChecked,
                                     boolean detectionMissing,
                                     boolean returnNull) {
        if (!hasPlayer) {
            return returnNull ? null : "Offline";
        }
        if (detectionMissing) {
            return "Detection Missing";
        }
        String worldName = player.getWorld().getName();
        Check check = hackType.getCheck();

        if (!check.isEnabled(dataType, worldName, null)) { // Do not put player because we calculate it below
            return returnNull ? null : "Disabled";
        }
        CancelCause disabledCause = player.getViolations(hackType).getDisableCause();
        return Permissions.isBypassing(player, hackType) ? "Permission Bypass" :
                cancellableCompatibility != null ? cancellableCompatibility + " Compatibility" :
                        notChecked ? "Temporarily Not Checked" :
                                serverLag ? "Server Lag" :
                                        disabledCause != null ? "Cancelled (" + disabledCause.getReason() + ")" :
                                                (returnNull ? null : (check.isSilent(worldName) ? "Silent " : "") + "Checking");
    }

    public void refresh(String targetName) {
        if (Config.settings.getBoolean("Important.refresh_inventory_menu")
                && cooldowns.canDo("")) {
            cooldowns.add("", 1);
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                for (SpartanPlayer o : players) {
                    Player no = o.getPlayer();

                    if (no != null) {
                        InventoryView inventoryView = no.getOpenInventory();

                        if (inventoryView.getTitle().equals(PlayerInfo.menu + targetName)) {
                            InteractiveInventory.playerInfo.open(o, targetName);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean internalHandle(SpartanPlayer player) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Reset")) {
            String playerName = title.substring(menu.length());

            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t != null && clickType.isLeftClick()) {
                    for (HackType hackType : Enums.HackType.values()) {
                        t.getViolations(hackType).reset();
                    }
                    String message = Config.messages.getColorfulString("player_violation_reset_message").replace("{player}", t.name);
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
