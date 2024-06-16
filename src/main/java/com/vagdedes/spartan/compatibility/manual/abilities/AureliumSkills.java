package com.vagdedes.spartan.compatibility.manual.abilities;

import com.archyx.aureliumskills.api.event.TerraformBlockBreakEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AureliumSkills implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(TerraformBlockBreakEvent e) {
        if (Compatibility.CompatibilityType.AURELIUM_SKILLS.isEnabled()) {
            SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

            if (p == null) {
                return;
            }
            p.cooldowns.add("aureliumskills=compatibility", 20);
        }
    }

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.AURELIUM_SKILLS.isFunctional()
                && !p.cooldowns.canDo("aureliumskills=compatibility");
    }
}
