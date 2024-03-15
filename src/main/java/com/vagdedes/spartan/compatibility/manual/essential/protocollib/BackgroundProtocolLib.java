package com.vagdedes.spartan.compatibility.manual.essential.protocollib;

import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.listeners.PlibHandlers;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import com.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.plugin.Plugin;

public class BackgroundProtocolLib {

    private static int enabled = 0;

    static boolean isInitiated() {
        return enabled == 1;
    }

    static void initiate() {
        try {
            if (enabled == 0) {
                Plugin plugin = PluginUtils.get("ProtocolLib");

                if (plugin != null) {
                    String version = plugin.getDescription().getVersion().replace("-SNAPSHOT", "");
                    Double number = AlgebraUtils.returnValidDecimal(version.substring(0, version.length() - 2));

                    if (number != null
                            && number < 4.5
                            && !ReflectionUtils.classExists("org.apache.commons.lang3.Validate")
                            && !MultiVersion.folia) {
                        enabled = -1;
                    } else {
                        enabled = 1;
                        PlibHandlers.runNoSlowdown();
                    }
                }
            }
        } catch (Exception e) {
            enabled = -1;
        }
    }
}
