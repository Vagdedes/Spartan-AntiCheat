package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.utils.java.OverflowMap;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.concurrent.ConcurrentHashMap;

public class Elytra {

    private static final Cooldowns cooldowns = new Cooldowns(
            new OverflowMap<>(new ConcurrentHashMap<>(), 512)
    );

    public static void judge(SpartanPlayer p, boolean event) {
        boolean gliding = p.movement.isGliding();

        if (p.getInstance().getVehicle() == null) {
            PlayerInventory inventory = p.getInventory();

            if (inventory != null) {
                ItemStack i = inventory.getChestplate();

                if (i != null) {
                    if (i.getType() == Material.ELYTRA) {
                        if (i.getDurability() < 432) {
                            if (event) {
                                p.getExecutor(Enums.HackType.Exploits).handle(false, Elytra.class);
                            } else {
                                cooldowns.add("elytra-wear=" + p.uuid, 2);
                            }
                        } else {
                            stopGliding(p);

                            if (gliding) {
                                p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{2, 2});
                            }
                        }
                    } else {
                        stopGliding(p);
                    }
                } else {
                    stopGliding(p);

                    if (gliding && !cooldowns.canDo("elytra-wear=" + p.uuid)) {
                        p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{3, 2});
                    }
                }
            }
        } else {
            stopGliding(p);

            if (gliding) {
                p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{1, 0});
            }
        }
    }

    private static void stopGliding(SpartanPlayer player) {
        Player p = player.getInstance();

        if (p != null) {
            p.setGliding(false);
        }
    }

}
