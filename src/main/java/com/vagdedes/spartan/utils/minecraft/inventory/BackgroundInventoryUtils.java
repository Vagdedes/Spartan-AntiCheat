package com.vagdedes.spartan.utils.minecraft.inventory;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

public class BackgroundInventoryUtils {

    private static final boolean paperProfile = ReflectionUtils.classExists(
            "com.destroystokyo.paper.profile.PlayerProfile"
    );
    static final boolean bukkitProfile = ReflectionUtils.classExists(
            "org.bukkit.profile.PlayerProfile"
    );

    static ItemStack getSkull_v1_13(OfflinePlayer offlinePlayer, String backupName, boolean create) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

        if (offlinePlayer != null) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            if (create
                    && !offlinePlayer.hasPlayedBefore()
                    && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_18)) {
                String name = offlinePlayer.getName();

                if (name == null) {
                    name = backupName;
                    PlayerProfile profile;

                    if (paperProfile) {
                        if (name == null) {
                            profile = Bukkit.createProfile(offlinePlayer.getUniqueId());
                        } else {
                            profile = Bukkit.createProfileExact(offlinePlayer.getUniqueId(), name);
                        }
                    } else if (name == null) {
                        profile = Bukkit.createPlayerProfile(offlinePlayer.getUniqueId());
                    } else {
                        profile = Bukkit.createPlayerProfile(offlinePlayer.getUniqueId(), name);
                    }
                    meta.setOwnerProfile(profile);
                }
            } else {
                meta.setOwningPlayer(offlinePlayer);
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }

}
