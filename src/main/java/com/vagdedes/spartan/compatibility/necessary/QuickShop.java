package com.vagdedes.spartan.compatibility.necessary;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.utils.server.PluginUtils;

public class QuickShop {

    public static boolean isEnabled() {
        return Compatibility.CompatibilityType.QuickShop.isFunctional()
                && PluginUtils.exists("quickshop");
    }
}
