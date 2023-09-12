package me.vagdedes.spartan.compatibility.manual.essential;

import com.earth2me.essentials.User;
import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class Essentials {

    private static Plugin essentials = null;
    private static final String compatibility = Compatibility.CompatibilityType.Essentials.toString();

    public static void reload() {
        try {
            essentials = Register.manager.getPlugin(compatibility);
        } catch (Exception ignored) {
        }
    }

    public static void run(SpartanPlayer p, String m) {
        if (Compatibility.CompatibilityType.Essentials.isFunctional()) {
            if (executed(p, m, "break")) {
                UUID uuid = p.getUniqueId();
                Enums.HackType.FastEat.getCheck().addDisabledUser(uuid, Enums.HackType.FastEat.toString(), 10);
                Enums.HackType.GhostHand.getCheck().addDisabledUser(uuid, Enums.HackType.GhostHand.toString(), 10);
                Enums.HackType.ImpossibleActions.getCheck().addDisabledUser(uuid, Enums.HackType.ImpossibleActions.toString(), 10);
            } else if (executed(p, m, "feed")) {
                Enums.HackType.FastEat.getCheck().addDisabledUser(p.getUniqueId(), Enums.HackType.FastEat.toString(), 10);
            }
        }
    }

    public static boolean isAFK(SpartanPlayer p) {
        if (Compatibility.CompatibilityType.Essentials.isFunctional() && essentials != null && essentials.isEnabled()) {
            User user = ((com.earth2me.essentials.Essentials) essentials).getUser(p.getUniqueId());
            return user != null && user.isAfk();
        }
        return false;
    }

    private static boolean executed(SpartanPlayer p, String m, String c) {
        return (p.getPlayer().hasPermission("essentials." + c) || p.isOp())
                && m.toLowerCase().startsWith("/" + c + " ") || m.equalsIgnoreCase("/" + c);
    }
}
