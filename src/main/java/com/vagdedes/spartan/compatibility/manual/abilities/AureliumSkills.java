package com.vagdedes.spartan.compatibility.manual.abilities;

import com.archyx.aureliumskills.api.event.TerraformBlockBreakEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AureliumSkills implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(TerraformBlockBreakEvent e) {
        if (Compatibility.CompatibilityType.AURELIUM_SKILLS.isEnabled()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            p.getCooldowns().add("aureliumskills=compatibility", 20);
        }
    }

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.AURELIUM_SKILLS.isFunctional()
                && !p.getCooldowns().canDo("aureliumskills=compatibility");
    }
}
