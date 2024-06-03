package com.vagdedes.spartan.utils.minecraft.server.inventory;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

public class BackgroundInventoryUtils {

    static final boolean paperProfile = ReflectionUtils.classExists(
            "com.destroystokyo.paper.profile.PlayerProfile"
    );

    static ItemStack getSkull(OfflinePlayer offlinePlayer, String backupName, boolean create) {
        ItemStack skull;
        SkullMeta meta;

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            skull = new ItemStack(Material.PLAYER_HEAD);
            meta = (SkullMeta) skull.getItemMeta();

            if (offlinePlayer != null) {
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
                            offlinePlayer = Bukkit.getOfflinePlayer(offlinePlayer.getUniqueId());
                        } else if (name == null) {
                            profile = Bukkit.createPlayerProfile(offlinePlayer.getUniqueId());
                            offlinePlayer = Bukkit.getOfflinePlayer(offlinePlayer.getUniqueId());
                        } else {
                            profile = Bukkit.createPlayerProfile(offlinePlayer.getUniqueId(), name);
                            offlinePlayer = Bukkit.getOfflinePlayer(offlinePlayer.getUniqueId());
                        }
                        meta.setOwnerProfile(profile);
                    }
                } else {
                    meta.setOwningPlayer(offlinePlayer);
                }
            }
        } else {
            skull = new ItemStack(
                    Material.getMaterial("SKULL_ITEM"),
                    1,
                    (short) SkullType.PLAYER.ordinal()
            );
            meta = (SkullMeta) skull.getItemMeta();
            String name = null;

            if (offlinePlayer != null) {
                name = offlinePlayer.getName();
            }
            if (name != null) {
                meta.setOwner(name);
            } else if (backupName != null) {
                meta.setOwner(backupName);
            }
        }
        skull.setItemMeta(meta);
        return skull;
    }

}
