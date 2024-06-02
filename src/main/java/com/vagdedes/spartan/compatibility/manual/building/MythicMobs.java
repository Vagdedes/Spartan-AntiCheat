package com.vagdedes.spartan.compatibility.manual.building;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.java.StringUtils;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
        return Compatibility.CompatibilityType.MYTHIC_MOBS.isFunctional()
                && new BukkitAPIHelper().isMythicMob(entity);
    }

    public static boolean is(SpartanPlayer player) {
        if (Compatibility.CompatibilityType.MYTHIC_MOBS.isFunctional()) {
            PlayerInventory inventory = player.getInventory();

            for (ItemStack armor : inventory.getArmorContents()) {
                if (armor != null && is(armor)) {
                    return true;
                }
            }
            return is(inventory.getItemInHand())
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                    && is(inventory.getItemInOffHand());
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

