package me.vagdedes.spartan.compatibility.manual.abilities;

import com.snowgears.grapplinghook.api.HookAPI;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class GrapplingHook implements Listener {

    private static boolean isItem(ItemStack i) {
        if (Compatibility.CompatibilityType.GrapplingHook.isFunctional()) {
            try {
                return PluginUtils.exists("grapplinghook") ? HookAPI.isGrapplingHook(i) : i.getType() == Material.FISHING_ROD;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(PlayerFishEvent e) {
        if (Compatibility.CompatibilityType.GrapplingHook.isFunctional() && e.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity caught = e.getCaught();

            if (caught instanceof Player) {
                SpartanPlayer p = SpartanBukkit.getPlayer((Player) caught),
                        t = SpartanBukkit.getPlayer(e.getPlayer().getUniqueId());

                if (p != null && t != null && !p.equals(t) && isItem(t.getItemInHand())) {
                    if (PluginUtils.exists("grapplinghook")) {
                        CheckProtection.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.GrapplingHook, 40);
                    } else {
                        CheckProtection.evadeStandardCombatFPs(p, Compatibility.CompatibilityType.GrapplingHook, 10);
                    }
                }
            }
        }
    }
}
