package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.implementation.world.exploits.Exploits;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Elytra {

    public static void judge(SpartanProtocol p) {
        if (((Exploits) p.spartan.getRunner(Enums.HackType.Exploits)).elytra.isEnabled()
                && !Permissions.isBypassing(p.bukkit, Enums.HackType.Exploits)) {
            if (p.spartan.getVehicle() == null) {
                PlayerInventory inventory = p.bukkit.getInventory();

                if (inventory != null) {
                    ItemStack i = inventory.getChestplate();

                    if (i != null) {
                        if (i.getType() == Material.ELYTRA) {
                            if (i.getDurability() < 432) {
                                p.spartan.getRunner(Enums.HackType.Exploits).handle(false, Elytra.class);
                            } else {
                                p.bukkit.setGliding(false);
                            }
                        } else {
                            p.bukkit.setGliding(false);
                        }
                    } else {
                        p.bukkit.setGliding(false);
                    }
                }
            } else {
                p.bukkit.setGliding(false);
            }
        }
    }

}
