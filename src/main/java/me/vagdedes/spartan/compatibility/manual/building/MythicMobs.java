package me.vagdedes.spartan.compatibility.manual.building;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanInventory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.java.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashSet;

public class MythicMobs {

    private static final HashSet<ItemStack> hm = new HashSet<>();

    public static void reload() {
        hm.clear();
        File[] files = new File("/plugins/MythicMobs/Items/").listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);

                    for (String key : filea.getKeys(false)) {
                        if (filea.contains(key + ".Id") && filea.contains(key + ".Display")) {
                            String id = filea.getString(key + ".Id");
                            Material material = Material.getMaterial(id.toUpperCase());

                            if (material != null) {
                                ItemStack item = new ItemStack(material);
                                ItemMeta meta = item.getItemMeta();
                                meta.setDisplayName(StringUtils.getClearColorString(
                                        StringUtils.getClearColorSyntaxString(
                                                filea.getString(key + ".Display"))));
                                item.setItemMeta(meta);
                                hm.add(item);
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean is(Entity entity) {
        return Compatibility.CompatibilityType.MythicMobs.isFunctional()
                && new BukkitAPIHelper().isMythicMob(entity);
    }

    public static boolean is(SpartanPlayer player) {
        if (Compatibility.CompatibilityType.MythicMobs.isFunctional()) {
            SpartanInventory inventory = player.getInventory();

            for (ItemStack armor : inventory.getArmorContents()) {
                if (armor != null && is(armor)) {
                    return true;
                }
            }
            return is(inventory.getItemInHand()) || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && is(inventory.getItemInOffHand());
        }
        return false;
    }

    private static boolean is(ItemStack item) {
        for (ItemStack cache : hm) {
            if (cache.getType() == item.getType() && item.getItemMeta() != null
                    && StringUtils.getClearColorString(item.getItemMeta().getDisplayName()).equals(cache.getItemMeta().getDisplayName())) {
                return true;
            }
        }
        return false;
    }
}

