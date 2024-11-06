package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.implementation.world.exploits.Exploits;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Elytra {

    public static void judge(SpartanPlayer p) {
        if (((Exploits) p.getExecutor(Enums.HackType.Exploits)).elytra.isEnabled()
                && !Permissions.isBypassing(p.getInstance(), Enums.HackType.Exploits)) {
            if (p.getInstance().getVehicle() == null) {
                PlayerInventory inventory = p.getInstance().getInventory();

                if (inventory != null) {
                    ItemStack i = inventory.getChestplate();

                    if (i != null) {
                        if (i.getType() == Material.ELYTRA) {
                            if (i.getDurability() < 432) {
                                p.getExecutor(Enums.HackType.Exploits).handle(false, Elytra.class);
                            } else {
                                p.getInstance().setGliding(false);
                            }
                        } else {
                            p.getInstance().setGliding(false);
                        }
                    } else {
                        p.getInstance().setGliding(false);
                    }
                }
            } else {
                p.getInstance().setGliding(false);
            }
        }
    }

}
