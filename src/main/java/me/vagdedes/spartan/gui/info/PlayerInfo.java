package me.vagdedes.spartan.gui.info;

import me.vagdedes.spartan.abstraction.InventoryMenu;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.functionality.moderation.Spectate;
import me.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.functionality.synchronicity.SpartanEdition;
import me.vagdedes.spartan.gui.SpartanMenu;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.gui.helpers.PlayerStateLists;
import me.vagdedes.spartan.handlers.connection.IDs;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.MiningHistory;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.profiling.PunishmentHistory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.CancelCause;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerInfo extends InventoryMenu {

    public static final String menu = ("§0Player Info: ").substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);
    private static final String documentationURL = "https://docs.google.com/document/d/e/2PACX-1vRSwc6vazSE5uCv6pcYkaWsP_RaHTmkU70lBLB9f9tudlSbJZr2ZdRQg3ZGXFtz-QWjDzTQqkSzmMt2/pub";

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
            setTitle(player, menu + (isOnline ? target.getName() : playerProfile.getName()));
            List<String> lore = new ArrayList<>(20);

            int i = 10;

            for (Enums.CheckType checkType : Enums.CheckType.values()) {
                if (checkType == Enums.CheckType.EXPLOITS) {
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
                    String ore = history.getOre().toString();
                    int days = history.getDays();
                    lore.add("§c" + mines + " §7" + ore + (mines == 1 ? "" : (ore.endsWith("s") ? "es" : "s"))
                            + " in §c" + days + " §7" + (days == 1 ? "day" : "days"));
                }
            }
            if (!added) {
                lore.add("§c" + PlayerStateLists.noDataAvailable);
            }
            int miningHistoryMines = playerProfile.getOverallMiningHistory().getMines(),
                    information = 33,
                    reset = 34;
            ItemStack item = miningHistoryMines > 0 ?
                    new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(miningHistoryMines, 64), (short) 14) :
                    new ItemStack(Material.QUARTZ_BLOCK);
            add("§2Mining History", lore, item, 16);

            PunishmentHistory punishmentHistory = playerProfile.getPunishmentHistory();
            lore.clear();
            lore.add("");
            lore.add("§7Warnings§8:§c " + punishmentHistory.getWarnings());
            lore.add("§7Kicks§8:§c " + punishmentHistory.getKicks());
            lore.add("§7Bans§8:§c " + punishmentHistory.getBans());
            lore.add("§7Reports§8:§c " + punishmentHistory.getReports().size());

            if (isOnline) {
                int violations = target.getViolationCount();
                lore.add("§7CPS (Clicks Per Second)§8:§a " + target.getClicks().getCount());
                lore.add("§7Player Latency§8:§a " + target.getPing() + "ms");
                lore.add("§7Overall Violations§8:§a " + violations);
                lore.add("§7Player State§8:§a " + playerProfile.getEvidence().getType() + " (" + target.getDataType() + ")");
                add("§2Information", lore, new ItemStack(Material.BOOK, Math.max(1, Math.min(violations, 64))), information);

                lore.clear();
                lore.add("");
                lore.add("§eLeft click to reset the player's live violations.");
                lore.add("§cRight click to delete the player's stored data.");
                add("§4Reset", lore, new ItemStack(Material.REDSTONE), reset);
            } else {
                lore.add("§7Player State§8:§a " + playerProfile.getEvidence().getType() + " (" + playerProfile.getDataType() + ")");
                add("§2Information", lore, new ItemStack(Material.BOOK), information);

                lore.clear();
                lore.add("");
                lore.add("§cClick to delete the player's stored data.");
                add("§4Reset", lore, new ItemStack(Material.REDSTONE), reset);
            }
            add("§2Debug", null, new ItemStack(Material.IRON_SWORD), 28);
            add("§2Spectate", null, new ItemStack(MaterialUtils.get("watch")), 29);

            add("§c" + (back ? "Back" : "Close"), AntiCheatUpdates.getInformation(false),
                    new ItemStack(Material.ARROW), 31);
            return true;
        }
    }

    private void addChecks(int slot, boolean isOnline,
                           SpartanPlayer player, PlayerProfile playerProfile,
                           List<String> lore, Enums.CheckType checkType) {
        lore.clear();
        lore.add("");
        int violations = 0;
        HackType[] hackTypes = Enums.HackType.values();

        if (isOnline) {
            for (HackType hackType : hackTypes) {
                if (hackType.getCheck().getCheckType() == checkType) {
                    violations += player.getViolations(hackType).getLevel();
                }
            }
        }

        // Separator

        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);

        // Separator

        double delayNumber = isOnline ? LagLeniencies.getDelaySimplified(LagLeniencies.getDelay(player)) : 0.0; // Base
        ResearchEngine.DataType dataType = isOnline ? player.getDataType() : playerProfile.getDataType();
        boolean tpsDropping = isOnline && LagLeniencies.hasInconsistencies(player, "tps"),
                latencyLag = isOnline && LagLeniencies.hasInconsistencies(player, "ping"),
                notChecked = isOnline && !MaximumCheckedPlayers.isChecked(player.getUniqueId()),
                detectionsNotAvailable = SpartanBukkit.canAdvertise && !SpartanEdition.hasDetectionsPurchased(dataType),
                listedChecks = false;
        String cancellableCompatibility = isOnline ? player.getCancellableCompatibility() : null;

        for (HackType hackType : hackTypes) {
            if (hackType.getCheck().getCheckType() == checkType) {
                violations = isOnline ? player.getViolations(hackType).getLevel() : 0;
                boolean hasViolations = violations > 0,
                        hasData = playerProfile.isSuspectedOrHacker() && playerProfile.getEvidence().has(hackType);

                String state = getDetectionState(player, hackType, dataType, cancellableCompatibility, delayNumber, isOnline,
                        tpsDropping, latencyLag, notChecked, detectionsNotAvailable, !SpartanEdition.supportsCheck(dataType, hackType),
                        !hasViolations && !hasData);

                if (hasViolations || hasData || state != null) {
                    String color = "§c";
                    String data = playerProfile.getEvidence().getKnowledge(hackType, color);
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
        int violations = !isOnline ? 0 : player.getViolations(hackType).getLevel();
        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);
        boolean hasViolations = violations > 0,
                hasData = playerProfile.isSuspectedOrHacker() && playerProfile.getEvidence().has(hackType);
        ResearchEngine.DataType dataType = isOnline ? player.getDataType() : playerProfile.getDataType();
        String state = getDetectionState(player,
                hackType,
                dataType,
                isOnline ? player.getCancellableCompatibility() : null,
                isOnline ? LagLeniencies.getDelaySimplified(LagLeniencies.getDelay(player)) : 0.0,
                isOnline,
                isOnline && LagLeniencies.hasInconsistencies(player, "tps"),
                isOnline && LagLeniencies.hasInconsistencies(player, "ping"),
                isOnline && !MaximumCheckedPlayers.isChecked(player.getUniqueId()),
                SpartanBukkit.canAdvertise && !SpartanEdition.hasDetectionsPurchased(dataType),
                !SpartanEdition.supportsCheck(dataType, hackType),
                !hasViolations && !hasData);

        if (hasViolations || hasData || state != null) {
            lore.add("§a" + violations + " §7violations");
            lore.add("§e" + state);

            if (hasData) {
                String color = "§c";
                lore.add(color + playerProfile.getEvidence().getKnowledge(hackType, color));
            }
        } else {
            lore.add("§c" + PlayerStateLists.noDataAvailable);
        }
        add("§2" + hackType.getCheck().getName() + " Check", lore, item, slot);
    }

    private String getDetectionState(SpartanPlayer player, HackType hackType, ResearchEngine.DataType dataType,
                                     String cancellableCompatibility,
                                     double delayNumber,
                                     boolean hasPlayer,
                                     boolean tpsDropping,
                                     boolean latencyIncreasing,
                                     boolean notChecked,
                                     boolean detectionMissing,
                                     boolean detectionUnsupported,
                                     boolean returnNull) {
        if (IDs.isPreview()) {
            return "Preview Mode";
        }
        if (!hasPlayer) {
            return returnNull ? null : "Offline";
        }
        if (detectionUnsupported) {
            return "Detection Unsupported";
        }
        if (detectionMissing) {
            return "Detection Missing";
        }
        String worldName = player.getWorld().getName();
        Check check = hackType.getCheck();

        if (!check.isEnabled(dataType, worldName, null)) { // Do not put player because we calculate it below
            return returnNull ? null : "Disabled";
        }
        UUID uuid = player.getUniqueId();
        String delay = delayNumber == 0.0 ? "" : " (" + (Math.floor(delayNumber) == delayNumber ? String.valueOf((int) delayNumber) : String.valueOf(delayNumber)) + ")";
        CancelCause disabledCause = check.getDisabledCause(uuid);
        return Permissions.isBypassing(player, hackType) ? "Permission Bypass" :
                cancellableCompatibility != null ? cancellableCompatibility + " Compatibility" :
                        notChecked ? "Temporarily Not Checked" :
                                tpsDropping ? "Server Lag" + delay :
                                        latencyIncreasing ? "Latency Lag" + delay :
                                                disabledCause != null ? "Cancelled (" + disabledCause.getReason() + ")" :
                                                        (returnNull ? null : (check.isSilent(worldName, uuid) ? "Silent " : "") + "Checking" + delay);
    }

    public void refresh(UUID uuid) {
        SpartanPlayer player = SpartanBukkit.getPlayer(uuid);

        if (player != null) {
            refresh(player.getName());
        }
    }

    public void refresh(String targetName) {
        String key = "player-info=inventory-menu";

        if (Config.settings.getBoolean("Important.refresh_inventory_menu")
                && SpartanBukkit.cooldowns.canDo(SpartanBukkit.uuid + "=" + key)) {
            SpartanBukkit.cooldowns.add(SpartanBukkit.uuid + "=" + key, 1);
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                for (SpartanPlayer o : players) {
                    Player no = o.getPlayer();

                    if (no != null) {
                        InventoryView inventoryView = no.getOpenInventory();

                        if (inventoryView.getTitle().equals(PlayerInfo.menu + targetName)) {
                            boolean back = false;

                            for (ItemStack itemStack : inventoryView.getTopInventory().getContents()) {
                                if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                                        && StringUtils.getClearColorString(itemStack.getItemMeta().getDisplayName()).equals("Back")) {
                                    back = true;
                                    break;
                                }
                            }
                            SpartanMenu.playerInfo.open(o, targetName);
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

        if (item.equals("Debug")) {
            if (!Permissions.has(player, Permission.INFO) && !Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                String playerName = title.substring(menu.length());
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t == null) {
                    player.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
                } else {
                    SpartanMenu.debugMenu.open(player, t);
                }
            }
        } else if (item.equals("Spectate")) {
            if (!Permissions.has(player, Permission.INFO) && !Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                String playerName = title.substring(menu.length());
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t == null) {
                    player.sendInventoryCloseMessage(Config.messages.getColorfulString("player_not_found_message"));
                } else {
                    Spectate.run(player, t);
                }
            }
        } else if (item.equals("Reset")) {
            String playerName = title.substring(menu.length());

            if (!Permissions.has(player, Permission.MANAGE)) {
                player.sendInventoryCloseMessage(Config.messages.getColorfulString("no_permission"));
            } else {
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t != null && clickType.isLeftClick()) {
                    for (HackType hackType : Enums.HackType.values()) {
                        t.getViolations(hackType).reset();
                    }
                    String message = Config.messages.getColorfulString("player_violation_reset_message").replace("{player}", t.getName());
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
            SpartanMenu.mainMenu.open(player);
        } else {
            player.sendImportantMessage("§7Click to learn more about the detection states§8: \n§a§n" + documentationURL);
        }
        return true;
    }
}
