package me.vagdedes.spartan.handlers.bug;

import me.vagdedes.spartan.functionality.important.Permissions;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FalseFallDamage {

    public static boolean runDamage(SpartanPlayer p, EntityDamageEvent.DamageCause dmg) {
        if (!Permissions.isBypassing(p, Enums.HackType.NoFall)) {
            UUID uuid = p.getUniqueId();
            Check check = Enums.HackType.NoFall.getCheck();

            if (check.getDisabledCause(uuid) == null
                    && check.getSilentCause(uuid) == null
                    && dmg == EntityDamageEvent.DamageCause.FALL

                    && !Moderation.wasDetected(p)
                    && p.getLastViolation().getLastViolationTime(false) > 500L
                    && p.hasViolations()

                    && !p.getHandlers().has(Handlers.HandlerType.Explosion)) {
                ItemStack itemStack = p.getItemInHand();

                if (itemStack.getType() != Material.WATER_BUCKET
                        && itemStack.getType() != Material.BUCKET
                        && (!p.isOnGround() || !p.isOnGroundCustom())) {
                    SpartanLocation to = p.getLocation();
                    return !PlayerData.isOnGround(p, to, 0, -1, 0)
                            && !PlayerData.isOnGround(p, to, 0, -1.5, 0);
                }
            }
        }
        return false;
    }
}
