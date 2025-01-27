package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.CPlayerRiptideEvent;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class TridentEvent implements Listener {

    private static final double riptideMaxSafeLevel = 3.0;

    @EventHandler
    private void Event(PlayerRiptideEvent e) {
        event(
                new CPlayerRiptideEvent(
                        e.getPlayer(),
                        e.getItem(),
                        e.getPlayer().getVelocity()
                ), false);
    }

    public static void event(CPlayerRiptideEvent e, boolean packets) {
        PlayerProtocol p = PluginBase.getProtocol(e.player, true);

        if (p.packetsEnabled() == packets) {
            PlayerInventory inventory = e.player.getInventory();

            for (ItemStack item : new ItemStack[]{inventory.getItemInHand(), inventory.getItemInOffHand()}) {
                if (item.getType() == Material.TRIDENT) {
                    int level = item.getEnchantmentLevel(Enchantment.RIPTIDE);

                    if (level > 0) {
                        int ticks = AlgebraUtils.integerRound(Math.log(level) * TPS.maximum);

                        if (level > riptideMaxSafeLevel) {
                            p.bukkitExtra.trackers.add(
                                    PlayerTrackers.TrackerType.TRIDENT,
                                    AlgebraUtils.integerCeil(ticks * (level / riptideMaxSafeLevel))
                            );
                        } else {
                            p.bukkitExtra.trackers.add(PlayerTrackers.TrackerType.TRIDENT, ticks);
                        }
                        p.profile().executeRunners(false, e);
                        break;
                    }
                }
            }
        }
    }

}
