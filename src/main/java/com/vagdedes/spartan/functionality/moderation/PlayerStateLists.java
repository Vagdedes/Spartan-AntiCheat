package com.vagdedes.spartan.functionality.moderation;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.minecraft.inventory.InventoryUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerStateLists {

    public static final String inactiveColour = "§8";
    private static final int[] ignoredSlots = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 16, 17,
            18, 19, 25, 26,
            27, 28, 34, 35,
            36, 37, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private static final Map<UUID, Integer> cache = new LinkedHashMap<>();

    private static void fill(Inventory inventory, PlayerProfile profile,
                             List<String> description, int slot) {
        ItemStack item = InventoryUtils.getHead();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c" + profile.name);

        List<String> lore = new ArrayList<>(description.size() + 4);
        lore.add("");
        lore.addAll(description);
        lore.add("");
        lore.add("§7Click this item to §eview the player's information§7.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);

        PluginBase.headThread.executeWithPriority(() -> {
            ItemStack itemNew = profile.getSkull();
            ItemMeta metaNew = itemNew.getItemMeta();
            metaNew.setDisplayName(meta.getDisplayName());
            metaNew.setLore(lore);
            itemNew.setItemMeta(metaNew);
            inventory.setItem(slot, itemNew);
        });
    }

    public static int getPage(UUID uuid) {
        return cache.getOrDefault(uuid, 1);
    }

    public static boolean nextPage(UUID uuid) {
        int page = getPage(uuid);

        if (page < 999) {
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

    private static Map<PlayerProfile, Collection<Enums.HackType>> getProfiles() {
        Collection<PlayerProfile> profiles = ResearchEngine.getPlayerProfiles();

        if (!profiles.isEmpty()) {
            Map<PlayerProfile, Collection<Enums.HackType>> map = new LinkedHashMap<>();

            for (PlayerProfile playerProfile : profiles) {
                Collection<Enums.HackType> evidenceDetails = playerProfile.getEvidenceList(
                        PlayerEvidence.slightestProbability
                );

                if (!evidenceDetails.isEmpty()) {
                    map.put(playerProfile, evidenceDetails);
                }
            }
            return map;
        } else {
            return new HashMap<>(0);
        }
    }

    public static void fill(UUID uuid, Inventory inventory) {
        Map<PlayerProfile, Collection<Enums.HackType>> selectedProfiles = getProfiles();
        int listSize;
        int slotPosition = 0,
                limit = 15;
        List<String> lore = new ArrayList<>();
        Integer[] freeSlots = getFreeSlots(inventory);

        if (!selectedProfiles.isEmpty()) {
            int page = getPage(uuid),
                    skip = ((page - 1) * limit);
            List<PlayerProfile> selectedProfilesToReview = subList(
                    new ArrayList<>(selectedProfiles.keySet()),
                    skip,
                    skip + limit
            );
            listSize = selectedProfilesToReview.size();

            if (listSize > 0) {
                for (PlayerProfile profile : selectedProfilesToReview) {
                    Collection<Enums.HackType> evidenceDetails = selectedProfiles.get(profile);

                    if (!evidenceDetails.isEmpty()) {
                        lore.clear();
                        lore.add("§7Has data for§8:");

                        for (Enums.HackType hackType : evidenceDetails) {
                            String description = "§4" + hackType.getCheck().getName();

                            if (!profile.getRunner(hackType).hasSufficientData(
                                    profile.getLastDataType(),
                                    profile.getLastDetectionType(),
                                    PlayerEvidence.dataRatio
                            )) {
                                description += " §8(§7Data incomplete§8)";
                            }
                            lore.add(description);
                        }
                        fill(inventory, profile, lore, freeSlots[slotPosition]);
                        slotPosition++;
                    } else {
                        listSize--;
                    }
                }
            }
        } else {
            listSize = 0;
        }

        if (listSize != limit) {
            lore.clear();
            lore.add("");
            lore.add("§cEmpty items like this will be filled with");
            lore.add("§csuspected players as they are found.");

            for (int i = listSize; i < limit; i++) {
                InventoryUtils.add(inventory, inactiveColour + "Empty", lore, InventoryUtils.getHead(), freeSlots[slotPosition]);
                slotPosition++;
            }
        }
    }

    private static Integer[] getFreeSlots(Inventory inventory) {
        List<Integer> freeSlots = new ArrayList<>();
        int maxSize = Math.min(inventory.getSize(), 45);

        for (int slot = 0; slot < maxSize; slot++) {
            int finalSlot = slot;
            if (inventory.getItem(slot) == null
                    && Arrays.stream(ignoredSlots)
                    .noneMatch(s -> s == finalSlot)) {
                freeSlots.add(slot);
            }
        }
        return freeSlots.toArray(new Integer[0]);
    }

    private static <E> List<E> subList(List<E> list, int startIndex, int toIndex) {
        int size = list.size();
        return startIndex > size
                ? new ArrayList<>(0)
                : list.subList(startIndex, Math.min(toIndex, size));
    }
}
