package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Elytra {

    public static void judge(SpartanPlayer p, boolean event) {
        boolean gliding = p.movement.isGliding();

        if (p.getVehicle() == null) {
            PlayerInventory inventory = p.getInventory();

            if (inventory != null) {
                ItemStack i = inventory.getChestplate();

                if (i != null) {
                    if (i.getType() == Material.ELYTRA) {
                        if (i.getDurability() < 432) {
                            if (event) {
                                p.getExecutor(Enums.HackType.Exploits).handle(false, Elytra.class);
                            } else {
                                Cooldowns cooldowns = p.getCooldowns();
                                cooldowns.add("elytra-wear", 2);

                                if (gliding) {
                                    p.getTrackers().add(Trackers.TrackerType.ELYTRA_USE, (int) (TPS.maximum / 2));
                                }
                            }
                        } else {
                            p.movement.setGliding(false);

                            if (gliding) {
                                p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{2, 2});
                            }
                        }
                    } else {
                        p.movement.setGliding(false);
                    }
                } else {
                    p.movement.setGliding(false);

                    if (gliding && !p.getCooldowns().canDo("elytra-wear")) {
                        p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{3, 2});
                    }
                }
            }
        } else {
            p.movement.setGliding(false);

            if (gliding) {
                p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{1, 0});
            }
        }
    }

}
