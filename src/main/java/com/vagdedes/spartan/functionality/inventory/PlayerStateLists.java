package com.vagdedes.spartan.functionality.inventory;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.utils.server.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerStateLists {

    private static final int maxPages = 999;

    public static final String
            hackerPlayers = "Identified Hackers",
            suspectedPlayers = "Suspected Players",
            legitimatePlayers = "Legitimate Players",
            inactiveColour = "§8",

    noDataAvailable = "No data available at this time",
            viewData = "§7Click this item to §eview the player's information§7.";
    public static final String[] menuList = new String[]{
            hackerPlayers,
            suspectedPlayers,
            legitimatePlayers
    };
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

    private static void fill(String title, Inventory inventory, PlayerProfile profile, List<String> description, int slot) {
        ItemStack item = profile.getSkull(false);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c" + profile.getName());

        List<String> lore = new ArrayList<>(description.size() + 10);
        InventoryUtils.prepareDescription(lore, title);

        if (!description.isEmpty()) {
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
                case hackerPlayers:
                    List<PlayerProfile> playerProfiles = subList(ResearchEngine.getHackers(), skip, skip + limit);

                    if ((listSize = playerProfiles.size()) > 0) {
                        for (PlayerProfile playerProfile : playerProfiles) {
                            Collection<Enums.HackType> evidenceDetails = playerProfile.evidence.getKnowledgeList();

                            if (!evidenceDetails.isEmpty()) {
                                lore.clear();
                                lore.add("§7Detected for§8:");

                                for (Enums.HackType hackType : evidenceDetails) {
                                    lore.add("§4" + hackType.getCheck().getName());
                                }
                                fill(title, inventory, playerProfile, lore, freeSlots[slotPosition]);
                                slotPosition++;
                            } else {
                                listSize--;
                            }
                        }
                    }
                    break;
                case suspectedPlayers:
                    playerProfiles = subList(ResearchEngine.getSuspectedPlayers(), skip, skip + limit);

                    if ((listSize = playerProfiles.size()) > 0) {
                        for (PlayerProfile playerProfile : playerProfiles) {
                            Collection<Enums.HackType> evidenceDetails = playerProfile.evidence.getKnowledgeList();

                            if (!evidenceDetails.isEmpty()) {
                                lore.clear();
                                lore.add("§7Detected for§8:");

                                for (Enums.HackType hackType : evidenceDetails) {
                                    lore.add("§4" + hackType.getCheck().getName());
                                }
                                fill(title, inventory, playerProfile, lore, freeSlots[slotPosition]);
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
                            fill(title, inventory, playerProfile, lore, freeSlots[slotPosition]);
                            slotPosition++;
                        }
                    }
                    break;
                default:
                    listSize = 0;
                    break;
            }

            if (listSize != limit) {
                boolean option = Config.settings.getBoolean("Important.inventory_menu_empty_heads");

                if (option) {
                    InventoryUtils.prepareDescription(lore, title);
                    lore.add("§7" + noDataAvailable);
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
