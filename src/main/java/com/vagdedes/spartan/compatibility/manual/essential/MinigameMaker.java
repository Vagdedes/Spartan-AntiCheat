package com.vagdedes.spartan.compatibility.manual.essential;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.features.IncompatibleItem;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import io.signality.api.MinigameExecutionEvent;
import io.signality.utils.system.Events;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class MinigameMaker implements Listener {

    public static final String name = Compatibility.CompatibilityType.MinigameMaker.toString();

    private static final LinkedHashSet<IncompatibleItem> set = new LinkedHashSet<>();
    private static File file = new File(Register.plugin.getDataFolder() + "/incompatibleItems.yml");

    public static void reload() {
        set.clear();
        Register.enable(new MinigameMaker(), MinigameMaker.class);
        file = new File(Register.plugin.getDataFolder() + "/incompatibleItems.yml");

        if (file.exists()) {
            for (String key : YamlConfiguration.loadConfiguration(file).getKeys(false)) {
                String[] split = key.split(" ");

                if (split.length == 4) {
                    addItem(split[0], split[1], split[3], split[2], 0);
                }
            }
        }
    }

    public static IncompatibleItem[] getIncompatibleItems() {
        return set.toArray(new IncompatibleItem[0]);
    }

    public static IncompatibleItem getByID(int ID) {
        for (IncompatibleItem item : set) {
            if (item.hashCode() == ID) {
                return item;
            }
        }
        return null;
    }

    public static boolean exists(IncompatibleItem other) {
        for (IncompatibleItem item : set) {
            if (item.getEventType() == other.getEventType() && item.getMaterial() == other.getMaterial()
                    && Arrays.equals(item.getHackTypes(), other.getHackTypes())
                    && item.getActualName().equalsIgnoreCase(other.getActualName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean addItem(String eventTypeString, String materialString, String hackTypesString, String name, int seconds) {
        eventTypeString = eventTypeString.replace("-", "_");
        Events.EventType eventType = null;

        for (Events.EventType original : Events.EventType.values()) {
            if (original.toString().equalsIgnoreCase(eventTypeString)) {
                eventType = original;
                break;
            }
        }

        if (eventType != null) {
            Material material = Material.getMaterial(materialString.replace("-", "_").toUpperCase());

            if (material != null) {
                String[] hackTypesStringSplit = hackTypesString.replace("-", "_").split("\\|");
                Set<Enums.HackType> hackTypes = new LinkedHashSet<>(hackTypesStringSplit.length);

                for (Enums.HackType original : Enums.HackType.values()) {
                    String originalString = original.toString();

                    for (String hackTypeString : hackTypesStringSplit) {
                        if (originalString.equalsIgnoreCase(hackTypeString)) {
                            hackTypes.add(original);
                            break;
                        }
                    }
                }

                if (hackTypes.size() > 0) {
                    IncompatibleItem incompatibleItem = new IncompatibleItem(
                            eventType,
                            material,
                            StringUtils.getClearColorString(name.replace(" ", "%spc%")),
                            hackTypes.toArray(new Enums.HackType[0]));
                    hackTypes.clear();

                    if (seconds > 0) {
                        if (!exists(incompatibleItem)) {
                            set.add(incompatibleItem);
                            incompatibleItem.setSeconds(seconds);
                            ConfigUtils.set(file, incompatibleItem.getConfigurationKey(), seconds);
                            return true;
                        }
                    } else {
                        seconds = YamlConfiguration.loadConfiguration(file).getInt(incompatibleItem.getConfigurationKey());

                        if (seconds > 0 && !exists(incompatibleItem)) {
                            set.add(incompatibleItem);
                            incompatibleItem.setSeconds(seconds);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean updateSeconds(IncompatibleItem item, int seconds) {
        if (seconds > 0 && seconds <= 60 && exists(item)) {
            int current = item.getSeconds();

            if (current != seconds) {
                item.setSeconds(seconds);
                ConfigUtils.set(file, item.getConfigurationKey(), seconds);
                return true;
            }
        }
        return false;
    }

    public static boolean removeItem(IncompatibleItem item) {
        if (exists(item)) {
            set.remove(item);
            ConfigUtils.set(file, item.getConfigurationKey(), null);
            return true;
        }
        return false;
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(MinigameExecutionEvent e) {
        Compatibility.CompatibilityType compatibilityType = Compatibility.CompatibilityType.MinigameMaker;

        if (compatibilityType.isFunctional()) {
            OfflinePlayer offlinePlayer = e.getPlayer();

            if (offlinePlayer != null && offlinePlayer.isOnline()) {
                Player onlinePlayer = (Player) offlinePlayer;

                PlayerInventory inventory = onlinePlayer.getInventory();
                Events.EventType eventType = e.getEventType();
                boolean exit = false;

                for (IncompatibleItem incompatibleItem : set) {
                    if (eventType == incompatibleItem.getEventType()) {
                        for (ItemStack itemStack : new ItemStack[]{
                                inventory.getItemInHand(),
                                inventory.getHelmet(),
                                inventory.getChestplate(),
                                inventory.getLeggings(),
                                inventory.getBoots()}) {
                            if (itemStack != null && itemStack.getType() == incompatibleItem.getMaterial()) {
                                String name = incompatibleItem.getName();
                                ItemMeta meta;

                                if (name == null || (meta = itemStack.getItemMeta()) != null && StringUtils.getClearColorString(meta.getDisplayName()).equals(name)) {
                                    UUID uuid = onlinePlayer.getUniqueId();
                                    int seconds = incompatibleItem.getSeconds();

                                    for (Enums.HackType hackType : incompatibleItem.getHackTypes()) {
                                        hackType.getCheck().addDisabledUser(uuid,
                                                compatibilityType + "-" + name,
                                                seconds * 20);
                                    }
                                }
                            }
                        }
                    }

                    if (exit) {
                        break;
                    }
                }
            }
        }
    }
}
