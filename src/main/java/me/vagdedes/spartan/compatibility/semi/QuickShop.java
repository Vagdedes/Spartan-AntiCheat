package me.vagdedes.spartan.compatibility.semi;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.utils.server.PluginUtils;

public class QuickShop {

    public static boolean isEnabled() {
        return Compatibility.CompatibilityType.QuickShop.isFunctional() && PluginUtils.exists("quickshop");
    }
}
