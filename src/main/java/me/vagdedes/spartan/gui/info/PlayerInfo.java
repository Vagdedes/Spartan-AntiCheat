package me.vagdedes.spartan.gui.info;

import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.moderation.Spectate;
import me.vagdedes.spartan.features.performance.MaximumCheckedPlayers;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.gui.helpers.AntiCheatUpdates;
import me.vagdedes.spartan.gui.helpers.PlayerStateLists;
import me.vagdedes.spartan.gui.spartan.SpartanMenu;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.MiningHistory;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.CancelCause;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import me.vagdedes.spartan.system.IDs;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.data.CooldownUtils;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerInfo {

    public static final String name = "Player Info",
            menu = ("§0" + name + ": ").substring(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? 2 : 0);
    private static final String documentationURL = "https://docs.google.com/document/d/e/2PACX-1vRSwc6vazSE5uCv6pcYkaWsP_RaHTmkU70lBLB9f9tudlSbJZr2ZdRQg3ZGXFtz-QWjDzTQqkSzmMt2/pub";

    public static void open(SpartanPlayer p, String name, boolean back) {
        if (!Permissions.has(p, Permission.INFO) && !Permissions.has(p, Permission.MANAGE)) {
            p.sendInventoryCloseMessage(Messages.get("no_permission"));
            return;
        }
        SpartanPlayer t = SpartanBukkit.getPlayer(name);
        boolean isOnline = t != null;
        PlayerProfile playerProfile = isOnline ? t.getProfile() : ResearchEngine.getPlayerProfileAdvanced(name, true);

        if (playerProfile == null) {
            p.sendInventoryCloseMessage(Messages.get("player_not_found_message"));
            return;
        }
        Inventory inv = p.createInventory(36, menu + (isOnline ? t.getName() : playerProfile.getName()));
        List<String> lore = new ArrayList<>(20);

        int i = 10;

        for (Enums.CheckType checkType : Enums.CheckType.values()) {
            if (checkType == Enums.CheckType.EXPLOITS) {
                addCheck(HackType.Exploits, 15, isOnline, t, inv, playerProfile, lore);
            } else {
                addChecks(i, isOnline, t, inv, playerProfile, lore, checkType);
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
                String ore = history.getOre().getString();
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
        InventoryUtils.add(inv, "§2Mining History", lore, item, 16);

        if (isOnline) {
            int violations = Check.getViolationCount(t.getUniqueId());
            lore.clear();
            lore.add("");
            lore.add("§7CPS (Clicks Per Second)§8:§a " + t.getClickData().getCount());
            lore.add("§7Player Latency§8:§a " + t.getPing() + "ms");
            lore.add("§7Overall Violations§8:§a " + violations);
            lore.add("§7Player State§8:§a " + playerProfile.getEvidence().getType() + " (" + t.getDataType() + ")");
            InventoryUtils.add(inv, "§2Information", lore, new ItemStack(Material.BOOK, Math.max(1, Math.min(violations, 64))), information);

            lore.clear();
            lore.add("");
            lore.add("§eLeft click to reset the player's live violations.");
            lore.add("§cRight click to delete the player's stored data.");
            InventoryUtils.add(inv, "§4Reset", lore, new ItemStack(Material.REDSTONE), reset);
        } else {
            lore.clear();
            lore.add("");
            lore.add("§7Player State§8:§a " + playerProfile.getEvidence().getType() + " (" + playerProfile.getDataType() + ")");
            InventoryUtils.add(inv, "§2Information", lore, new ItemStack(Material.BOOK), information);

            lore.clear();
            lore.add("");
            lore.add("§cClick to delete the player's stored data.");
            InventoryUtils.add(inv, "§4Reset", lore, new ItemStack(Material.REDSTONE), reset);
        }
        InventoryUtils.add(inv, "§2Debug", null, new ItemStack(Material.IRON_SWORD), 28);
        InventoryUtils.add(inv, "§2Spectate", null, new ItemStack(MaterialUtils.get("watch")), 29);

        InventoryUtils.add(inv, "§c" + (back ? "Back" : "Close"), AntiCheatUpdates.getInformation(false),
                new ItemStack(Material.ARROW), 31);

        p.openInventory(inv);
    }

    private static void addChecks(int slot, boolean isOnline,
                                  SpartanPlayer p, Inventory inv, PlayerProfile playerProfile,
                                  List<String> lore, Enums.CheckType checkType) {
        lore.clear();
        lore.add("");
        int violations = 0;
        HackType[] hackTypes = Enums.HackType.values();

        if (isOnline) {
            for (HackType hackType : hackTypes) {
                Check check = hackType.getCheck();

                if (check.getCheckType() == checkType) {
                    violations += check.getViolations(p).getLevel();
                }
            }
        }

        // Separator

        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);

        // Separator

        double delayNumber = isOnline ? LagLeniencies.getDelaySimplified(LagLeniencies.getDelay(p)) : 0.0; // Base
        ResearchEngine.DataType dataType = isOnline ? p.getDataType() : playerProfile.getDataType();
        boolean tpsDropping = isOnline && LagLeniencies.hasInconsistencies(p, "tps"),
                latencyLag = isOnline && LagLeniencies.hasInconsistencies(p, "ping"),
                notChecked = isOnline && !MaximumCheckedPlayers.isChecked(p.getUniqueId()),
                detectionsNotAvailable = SpartanBukkit.canAdvertise && !SpartanEdition.hasDetectionsPurchased(dataType),
                listedChecks = false;
        String cancellableCompatibility = isOnline ? p.getCancellableCompatibility() : null;

        for (HackType hackType : hackTypes) {
            Check check = hackType.getCheck();

            if (check.getCheckType() == checkType) {
                violations = isOnline ? check.getViolations(p).getLevel() : 0;
                boolean hasViolations = violations > 0,
                        hasData = playerProfile.isSuspectedOrHacker() && playerProfile.getEvidence().has(hackType);

                String state = getDetectionState(p, hackType, dataType, cancellableCompatibility, delayNumber, isOnline,
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
        InventoryUtils.add(inv, "§2" + checkType.toString() + " Checks", lore, item, slot);
    }

    private static void addCheck(HackType hackType, int slot, boolean isOnline,
                                 SpartanPlayer p, Inventory inv, PlayerProfile playerProfile,
                                 List<String> lore) {
        lore.clear();
        lore.add("");
        Check check = hackType.getCheck();
        int violations = !isOnline ? 0 : check.getViolations(p).getLevel();
        ItemStack item = isOnline && violations > 0 ?
                new ItemStack(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? Material.RED_TERRACOTTA : Material.getMaterial("STAINED_CLAY"), Math.min(violations, 64), (short) 14) :
                new ItemStack(Material.QUARTZ_BLOCK);
        boolean hasViolations = violations > 0,
                hasData = playerProfile.isSuspectedOrHacker() && playerProfile.getEvidence().has(hackType);
        ResearchEngine.DataType dataType = isOnline ? p.getDataType() : playerProfile.getDataType();
        String state = getDetectionState(p,
                hackType,
                dataType,
                isOnline ? p.getCancellableCompatibility() : null,
                isOnline ? LagLeniencies.getDelaySimplified(LagLeniencies.getDelay(p)) : 0.0,
                isOnline,
                isOnline && LagLeniencies.hasInconsistencies(p, "tps"),
                isOnline && LagLeniencies.hasInconsistencies(p, "ping"),
                isOnline && !MaximumCheckedPlayers.isChecked(p.getUniqueId()),
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
        InventoryUtils.add(inv, "§2" + hackType.getCheck().getName() + " Check", lore, item, slot);
    }

    private static String getDetectionState(SpartanPlayer p, HackType hackType, ResearchEngine.DataType dataType,
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
        String worldName = p.getWorld().getName();
        Check check = hackType.getCheck();

        if (!check.isEnabled(dataType, worldName, null)) { // Do not put player because we calculate it below
            return returnNull ? null : "Disabled"; //todo
        }
        UUID uuid = p.getUniqueId();
        String delay = delayNumber == 0.0 ? "" : " (" + (Math.floor(delayNumber) == delayNumber ? String.valueOf((int) delayNumber) : String.valueOf(delayNumber)) + ")";
        CancelCause disabledCause = check.getDisabledCause(uuid);
        return Permissions.isBypassing(p, hackType) ? "Permission Bypass" :
                cancellableCompatibility != null ? cancellableCompatibility + " Compatibility" :
                        notChecked ? "Temporarily Not Checked" :
                                tpsDropping ? "Server Lag" + delay :
                                        latencyIncreasing ? "Latency Lag" + delay :
                                                disabledCause != null ? "Cancelled (" + disabledCause.getReason() + ")" :
                                                        (returnNull ? null : (check.isSilent(worldName, uuid) ? "Silent " : "") + "Checking" + delay);
    }

    public static void refresh(UUID uuid) {
        SpartanPlayer player = SpartanBukkit.getPlayer(uuid);

        if (player != null) {
            refresh(player.getName());
        }
    }

    public static void refresh(String targetName) {
        String key = "player-info=inventory-menu";

        if (Settings.getBoolean("Important.refresh_inventory_menu")
                && CooldownUtils.store.canDo(SpartanBukkit.uuid + "=" + key)) {
            CooldownUtils.store.add(SpartanBukkit.uuid + "=" + key, 1);
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (players.size() > 0) {
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
                            open(o, targetName, back);
                        }
                    }
                }
            }
        }
    }

    public static boolean run(SpartanPlayer p, ItemStack i, String title, ClickType clickType) {
        if (!title.contains(menu)) {
            return false;
        }
        String item = i.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Debug")) {
            if (!Permissions.has(p, Permission.INFO) && !Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            } else {
                String playerName = title.substring(menu.length());
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t == null) {
                    p.sendInventoryCloseMessage(Messages.get("player_not_found_message"));
                } else {
                    DebugMenu.open(p, t);
                }
            }
        } else if (item.equals("Spectate")) {
            if (!Permissions.has(p, Permission.INFO) && !Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            } else {
                String playerName = title.substring(menu.length());
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t == null) {
                    p.sendInventoryCloseMessage(Messages.get("player_not_found_message"));
                } else {
                    Spectate.run(p, t);
                }
            }
        } else if (item.equals("Reset")) {
            String playerName = title.substring(menu.length());

            if (!Permissions.has(p, Permission.MANAGE)) {
                p.sendInventoryCloseMessage(Messages.get("no_permission"));
            } else {
                SpartanPlayer t = SpartanBukkit.getPlayer(playerName);

                if (t != null && clickType.isLeftClick()) {
                    for (HackType hackType : Enums.HackType.values()) {
                        hackType.getCheck().getViolations(t).reset();
                    }
                    String message = Messages.get("player_violation_reset_message").replace("{player}", t.getName());
                    p.sendMessage(message);
                } else {
                    String name = Bukkit.getOfflinePlayer(playerName).getName();

                    if (name == null) {
                        p.sendMessage(Messages.get("player_not_found_message"));
                    } else {
                        ResearchEngine.resetData(name);
                        p.sendMessage(Messages.get("player_stored_data_delete_message").replace("{player}", name));
                    }
                }
                p.sendInventoryCloseMessage(null);
            }
        } else if (item.equals("Close")) {
            p.sendInventoryCloseMessage(null);
        } else if (item.equals("Back")) {
            SpartanMenu.open(p);
        } else {
            p.sendImportantMessage("§7Click to learn more about the detection states§8: \n§a§n" + documentationURL);
        }
        return true;
    }
}
