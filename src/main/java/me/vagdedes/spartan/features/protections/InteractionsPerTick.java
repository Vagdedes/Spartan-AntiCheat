package me.vagdedes.spartan.features.protections;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class InteractionsPerTick {

    private static final String str = "interact-spam";
    private static final Material
            snowball = MaterialUtils.get("snowball"),
            fireball = MaterialUtils.get("fireball");

    public static boolean run(SpartanPlayer p, SpartanBlock b, Action action) {
        if (!Permissions.isBypassing(p, null)) {
            ItemStack item;

            if (action == Action.RIGHT_CLICK_BLOCK && b != null && BlockUtils.isChangeable(b.getLocation())

                    || action == Action.RIGHT_CLICK_AIR && (item = p.getItemInHand()) != null
                    && (item.getType() == Material.FISHING_ROD
                    || item.getType() == snowball
                    || item.getType() == fireball)) {
                if (p.getCooldowns().canDo(str)) {
                    int limit = Settings.getInteger("Protections.interactions_per_tick");

                    if (limit > 0 && p.getBuffer().start(str, 1) >= Math.max(5, limit)) {
                        p.getCooldowns().add(str, 20);
                        return true;
                    }
                } else return true;
            }
        }
        return false;
    }
}
