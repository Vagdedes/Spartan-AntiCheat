package me.vagdedes.spartan.compatibility.manual.abilities;

import com.archyx.aureliumskills.api.event.TerraformBlockBreakEvent;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AureliumSkills implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(TerraformBlockBreakEvent e) {
        if (Compatibility.CompatibilityType.AureliumSkills.isEnabled()) {
            SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

            if (p == null) {
                return;
            }
            p.getCooldowns().add("aureliumskills=compatibility", 20);
        }
    }

    public static boolean canCancel(SpartanPlayer p) {
        return Compatibility.CompatibilityType.AureliumSkills.isFunctional()
                && !p.getCooldowns().canDo("aureliumskills=compatibility");
    }
}
