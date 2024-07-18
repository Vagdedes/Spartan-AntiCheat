package com.vagdedes.spartan.compatibility.manual.abilities;

import com.archyx.aureliumskills.api.event.TerraformBlockBreakEvent;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.concurrent.ConcurrentHashMap;

public class AureliumSkills implements Listener {

    private static final Cooldowns cooldowns = new Cooldowns(
            new OverflowMap<>(new ConcurrentHashMap<>(), 512)
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(TerraformBlockBreakEvent e) {
        if (Compatibility.CompatibilityType.AURELIUM_SKILLS.isEnabled()) {
            Player n = e.getPlayer();

            if (ProtocolLib.isTemporary(n)) {
                return;
            }
            SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

            if (p == null) {
                return;
            }
            cooldowns.add(p.uuid + "=aureliumskills=compatibility", 20);
        }
    }

    public static boolean isUsing(SpartanPlayer p) {
        return Compatibility.CompatibilityType.AURELIUM_SKILLS.isFunctional()
                && !cooldowns.canDo(p.uuid + "=aureliumskills=compatibility");
    }
}
