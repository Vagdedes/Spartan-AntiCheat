package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class TridentUse {

    private static final double riptideMaxSafeLevel = 3.0;

    public static void run(SpartanProtocol p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            PlayerInventory inventory = p.bukkit.getInventory();

            for (ItemStack item : new ItemStack[]{inventory.getItemInHand(), inventory.getItemInOffHand()}) {
                if (item.getType() == Material.TRIDENT) {
                    int level = item.getEnchantmentLevel(Enchantment.RIPTIDE);
                    if (level > 0) {
                        int ticks = AlgebraUtils.integerRound(Math.log(level) * TPS.maximum);

                        if (level > riptideMaxSafeLevel) {
                            p.spartan.trackers.add(
                                    PlayerTrackers.TrackerType.TRIDENT,
                                    AlgebraUtils.integerCeil(ticks * (level / riptideMaxSafeLevel))
                            );
                        } else {
                            p.spartan.trackers.add(PlayerTrackers.TrackerType.TRIDENT, ticks);
                        }

                        p.spartan.getExecutor(Enums.HackType.Speed).handle(false, level);
                    }
                    break;
                }
            }
        }
    }

}
