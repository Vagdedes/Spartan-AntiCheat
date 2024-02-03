package com.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import com.vagdedes.spartan.checks.exploits.Exploits;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ElytraUse {

    private static final String increasedSpeedKey = "elytra=increased-speed";
    private static final Material firework = MaterialUtils.get("firework");

    public static void judge(SpartanPlayer p, boolean gliding, boolean event) {
        if (p.getVehicle() == null) {
            ItemStack i = p.getInventory().getChestplate();

            if (i != null) {
                if (i.getType() == Material.ELYTRA) {
                    if (i.getDurability() < 432) {
                        if (!event
                                || !p.getExecutor(Enums.HackType.Exploits).handle(Exploits.ELYTRA_GLIDE_SPAM)) {
                            p.setGliding(gliding, false);
                            Handlers handlers = p.getHandlers();
                            handlers.add(Handlers.HandlerType.ElytraWear, 2);

                            if (gliding) {
                                handlers.add(Handlers.HandlerType.ElytraUse, hasIncreasedSpeed(p) ? 20 : 10);
                            }
                        }
                    } else {
                        p.setGliding(false, gliding);

                        if (gliding) {
                            p.getExecutor(Enums.HackType.Exploits).handle(new int[]{2, 2});
                        }
                    }
                } else {
                    p.setGliding(false, gliding);
                }
            } else {
                p.setGliding(false, gliding);

                if (gliding && !p.getHandlers().has(Handlers.HandlerType.ElytraWear)) {
                    p.getExecutor(Enums.HackType.Exploits).handle(new int[]{3, 2});
                }
            }
        } else {
            p.setGliding(false, gliding);

            if (gliding) {
                p.getExecutor(Enums.HackType.Exploits).handle(new int[]{1, 0});
            }
        }
    }

    private static boolean hasIncreasedSpeed(SpartanPlayer p) {
        ItemStack item = p.getItemInHand();

        if (item.getType() == firework
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (item = p.getInventory().getItemInOffHand()) != null && item.getType() == firework
                || p.getHandlers().has(Handlers.HandlerType.Trident)
                || Velocity.hasCooldown(p)) {
            p.getCooldowns().add(increasedSpeedKey, 20);
            return true;
        }
        return !p.getCooldowns().canDo(increasedSpeedKey);
    }
}
