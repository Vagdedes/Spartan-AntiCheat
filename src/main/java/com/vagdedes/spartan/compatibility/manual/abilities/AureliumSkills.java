package com.vagdedes.spartan.compatibility.manual.abilities;

import com.archyx.aureliumskills.api.event.TerraformBlockBreakEvent;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.LinkedHashMap;

public class AureliumSkills implements Listener {

    private static final Cooldowns cooldowns = new Cooldowns(
            new OverflowMap<>(new LinkedHashMap<>(), 512)
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(TerraformBlockBreakEvent e) {
        if (Compatibility.CompatibilityType.AURELIUM_SKILLS.isEnabled()) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());
            cooldowns.add(protocol.getUUID() + "=aureliumskills=compatibility", 20);
        }
    }

    public static boolean isUsing(SpartanProtocol p) {
        return Compatibility.CompatibilityType.AURELIUM_SKILLS.isFunctional()
                && !cooldowns.canDo(p.getUUID() + "=aureliumskills=compatibility");
    }
}
