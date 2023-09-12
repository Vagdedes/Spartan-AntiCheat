package me.vagdedes.spartan.gui.helpers;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.moderation.PlayerReports;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.profiling.PlayerReport;
import me.vagdedes.spartan.objects.profiling.PunishmentHistory;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.utils.server.InventoryUtils;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerStateLists {

    private static final int maxPages = 999;

    public static final String playerStates = "Player State",
            hackerFinder = "Identified Hackers",
            suspectedPlayers = "Suspected Players",
            legitimatePlayers = "Legitimate Players",
            punishedPlayers = "Punished Players",
            inactiveColour = "§8",

    noDataAvailable = "No data available at this time",
            calculatingData = "Calculating available data...",
            viewData = "§7Click this item to §eview the player's information§7.";
    public static final String[] menuList = new String[]{playerStates, legitimatePlayers, punishedPlayers};
    private static final int[] ignoredSlots = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    /*private static final int[] ignoredSlots = new int[]{
            0, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            45, 53
    };*/

    private static final Map<UUID, Integer> cache = Cache.store(new LinkedHashMap<>());

    private static void fill(String title, Inventory inventory, String name, List<String> description, int slot) {
        ItemStack item = ResearchEngine.getSkull(name);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c" + name);

        List<String> lore = new ArrayList<>(description.size() + 10);
        InventoryUtils.prepareDescription(lore, title);

        if (description.size() > 0) {
            lore.addAll(description);
            lore.add("");
        }
        lore.add(viewData);

        meta.setLore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    public static int getPage(UUID uuid) {
        return cache.getOrDefault(uuid, 1);
    }

    public static boolean nextPage(UUID uuid) {
        int page = getPage(uuid);

        if (page < maxPages) {
            cache.put(uuid, page + 1);
            return true;
        }
        return false;
    }

    public static boolean previousPage(UUID uuid) {
        int page = getPage(uuid);

        if (page > 1) {
            cache.put(uuid, page - 1);
            return true;
        }
        return false;
    }

    public static void fill(UUID uuid, Inventory inventory) {
        List<String> lore = new ArrayList<>(20);
        int slotPosition = 0, limit = 7;
        Integer[] freeSlots = getFreeSlots(inventory);

        for (String title : menuList) {
            int listSize, page = getPage(uuid), skip = ((page - 1) * limit);

            switch (title) {
                case playerStates:
                    List<PlayerProfile> playerProfiles = ResearchEngine.getHackers();
                    playerProfiles.addAll(ResearchEngine.getSuspectedPlayers());
                    playerProfiles = subList(playerProfiles, skip, skip + limit);

                    if ((listSize = playerProfiles.size()) > 0) {
                        for (PlayerProfile playerProfile : playerProfiles) {
                            Collection<Enums.HackType> evidenceDetails = playerProfile.getEvidence().getKnowledgeList();

                            if (evidenceDetails.size() > 0) {
                                lore.clear();
                                lore.add("§7Detected for§8:");

                                for (Enums.HackType hackType : evidenceDetails) {
                                    lore.add("§4" + hackType.getCheck().getName());
                                }
                                fill(playerProfile.isHacker() ? hackerFinder : suspectedPlayers, inventory, playerProfile.getName(), lore, freeSlots[slotPosition]);
                                slotPosition++;
                            } else {
                                listSize--;
                            }
                        }
                    }
                    break;
                case legitimatePlayers:
                    playerProfiles = subList(ResearchEngine.getLegitimatePlayers(), skip, skip + limit);

                    if ((listSize = playerProfiles.size()) > 0) {
                        for (PlayerProfile playerProfile : playerProfiles) {
                            lore.clear();
                            fill(title, inventory, playerProfile.getName(), lore, freeSlots[slotPosition]);
                            slotPosition++;
                        }
                    }
                    break;
                case punishedPlayers:
                    List<PlayerReport> playerReports = subList(PlayerReports.getList(), skip, skip + limit);

                    if ((listSize = playerReports.size()) > 0) {
                        for (PlayerReport playerReport : playerReports) {
                            lore.clear();
                            lore.add("§7Reported for§8:");
                            StringUtils.constructDescription("§7" + playerReport.getReason(), lore, true);
                            fill(title, inventory, playerReport.getReported(), lore, freeSlots[slotPosition]);
                            slotPosition++;
                        }
                    }
                    if (listSize < limit) {
                        int oldListSize = listSize,
                                newLimit = limit - listSize;
                        playerProfiles = subList(ResearchEngine.getPunishedProfiles(false), skip, skip + newLimit);

                        if ((listSize = playerProfiles.size()) > 0) {
                            for (PlayerProfile playerProfile : playerProfiles) {
                                PunishmentHistory punishmentHistory = playerProfile.getPunishmentHistory();
                                lore.clear();
                                lore.add("§7Warnings§8:§c " + punishmentHistory.getWarnings());
                                lore.add("§7Kicks§8:§c " + punishmentHistory.getKicks());
                                lore.add("§7Bans§8:§c " + punishmentHistory.getBans());
                                lore.add("§7Reports§8:§c " + punishmentHistory.getReports().size());
                                fill(title, inventory, playerProfile.getName(), lore, freeSlots[slotPosition]);
                                slotPosition++;
                            }
                        }
                        listSize += oldListSize;
                    }
                    break;
                default:
                    listSize = 0;
                    break;
            }

            if (listSize != limit) {
                boolean option = Settings.getBoolean("Important.inventory_menu_empty_heads");

                if (option) {
                    InventoryUtils.prepareDescription(lore, title);

                    if (ResearchEngine.isCaching()) {
                        lore.add("§7" + calculatingData);
                    } else {
                        lore.add("§7" + noDataAvailable);
                    }
                    // do not translate or make it modifiable
                    lore.add("");
                    lore.add("§cEmpty items like this will be filled with");
                    lore.add("§cuseful information about your players");
                    lore.add("§cas Spartan learns more about your server.");
                }

                for (int i = listSize; i < limit; i++) {
                    if (option) {
                        InventoryUtils.add(inventory, inactiveColour + "Empty", lore, InventoryUtils.getHead(), freeSlots[slotPosition]);
                    }
                    slotPosition++;
                }
            }
        }
    }

    private static Integer[] getFreeSlots(Inventory inventory) {
        int slot = 0, maxSize = 45;
        List<Integer> array = new ArrayList<>(Math.min(inventory.getSize(), maxSize));

        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null) {
                slot++;
            } else {
                boolean add = true;

                for (int ignoredSlot : ignoredSlots) {
                    if (ignoredSlot == slot) {
                        add = false;
                        break;
                    }
                }

                if (add) {
                    array.add(slot);
                }
                if (slot == maxSize) {
                    break;
                }
                slot++;
            }
        }
        return array.toArray(new Integer[0]);
    }

    private static <E> List<E> subList(List<E> list, int startIndex, int toIndex) {
        int size = list.size();
        return startIndex >= size ? new ArrayList<>(0) :
                list.subList(startIndex, Math.min(toIndex, size - 1));
    }
}
