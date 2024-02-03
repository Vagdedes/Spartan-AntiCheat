package com.vagdedes.spartan.compatibility.semi;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.utils.server.PluginUtils;

public class QuickShop {

    public static boolean isEnabled() {
        return Compatibility.CompatibilityType.QuickShop.isFunctional()
                && PluginUtils.exists("quickshop");
    }
}
