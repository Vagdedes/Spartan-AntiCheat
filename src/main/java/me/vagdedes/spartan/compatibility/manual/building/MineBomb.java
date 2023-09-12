package me.vagdedes.spartan.compatibility.manual.building;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class MineBomb {

    private static final Set<Material> hs = new HashSet<>();

    public static void reload() {
        hs.clear();
    }

    public static boolean canUse(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.MineBomb.isFunctional()) {
            if (cacheCanUse(p)) {
                return true;
            }
            File file = new File("plugins/MineBomb/config.yml");

            if (file.exists()) {
                boolean result = false;
                YamlConfiguration filea = YamlConfiguration.loadConfiguration(file);

                for (String key : filea.getKeys(true)) {
                    if (key.endsWith(".item")) {
                        Material m = Material.getMaterial(filea.getString(key));

                        if (m != null) {
                            hs.add(m);
                            result = true;
                        }
                    }
                }
                return result;
            }
        }
        return false;
    }

    private static boolean cacheCanUse(SpartanPlayer p) {
        for (Material m : hs) {
            if (p.getItemInHand().getType() == m) {
                return true;
            }
        }
        return false;
    }
}
