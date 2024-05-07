package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class EventsHandler9 implements Listener {

    @EventHandler
    private void PluginEnable(PluginEnableEvent e) {

        // Utils
        PluginUtils.clear();

        // System
        Config.compatibility.fastClear();
    }

    @EventHandler
    private void PluginDisable(PluginDisableEvent e) {

        // Utils
        PluginUtils.clear();

        // System
        Config.compatibility.fastClear();
    }

}
