package com.vagdedes.spartan.compatibility.manual.essential;

import com.earth2me.essentials.User;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.plugin.Plugin;

public class Essentials {

    private static Plugin essentials = null;
    private static final String compatibility = Compatibility.CompatibilityType.Essentials.toString();
    private static final Enums.HackType[] hackTypes = new Enums.HackType[]{
            Enums.HackType.FastEat,
            Enums.HackType.GhostHand,
            Enums.HackType.ImpossibleActions
    };

    public static void reload() {
        try {
            essentials = Register.manager.getPlugin(compatibility);
        } catch (Exception ignored) {
        }
    }

    public static void run(SpartanPlayer p, String m) {
        if (Compatibility.CompatibilityType.Essentials.isFunctional()) {
            if (executed(p, m, "break")) {
                for (Enums.HackType hackType : hackTypes) {
                    p.getViolations(hackType).addDisableCause(hackType.toString(), null, 10);
                }
            } else if (executed(p, m, "feed")) {
                p.getViolations(Enums.HackType.FastEat).addDisableCause(Enums.HackType.FastEat.toString(), null, 10);
            }
        }
    }

    public static boolean isAFK(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.Essentials.isFunctional() && essentials != null && essentials.isEnabled()) {
            User user = ((com.earth2me.essentials.Essentials) essentials).getUser(p.uuid);
            return user != null && user.isAfk();
        }
        return false;
    }

    private static boolean executed(SpartanPlayer p, String m, String c) {
        return (p.getPlayer().hasPermission("essentials." + c) || p.isOp())
                && m.toLowerCase().startsWith("/" + c + " ") || m.equalsIgnoreCase("/" + c);
    }
}
