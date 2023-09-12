package me.vagdedes.spartan.handlers.identifiers.complex.unpredictable;

import me.vagdedes.spartan.checks.exploits.ElytraGlideSpam;
import me.vagdedes.spartan.checks.exploits.IllegalElytraPacket;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.server.MaterialUtils;
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
                        if (!event || !ElytraGlideSpam.run(p)) {
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
                            IllegalElytraPacket.run(p, 2, 2);
                        }
                    }
                } else {
                    p.setGliding(false, gliding);
                }
            } else {
                p.setGliding(false, gliding);

                if (gliding && !p.getHandlers().has(Handlers.HandlerType.ElytraWear)) {
                    IllegalElytraPacket.run(p, 3, 2);
                }
            }
        } else {
            p.setGliding(false, gliding);

            if (gliding) {
                IllegalElytraPacket.run(p, 1, 0);
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
