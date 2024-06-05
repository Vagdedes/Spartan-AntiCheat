package com.vagdedes.spartan;

import com.vagdedes.spartan.functionality.command.CommandExecution;
import com.vagdedes.spartan.functionality.command.CommandTab;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.*;
import com.vagdedes.spartan.utils.minecraft.server.ProxyUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;

public class Register extends JavaPlugin {

    public static Plugin plugin = null;

    public static final PluginManager manager = Bukkit.getPluginManager();
    private static final Set<Class<?>> listeners = new LinkedHashSet<>(2);

    public void onEnable() {
        plugin = this;
        plugin.setNaggable(false);
        Config.create();

        // Version
        if (MultiVersion.other) {
            AwarenessNotifications.forcefullySend(
                    "The server's version or type is not supported. "
                            + "Please contact the plugin's developer if you think this is an error."
            );
            disablePlugin();
            return;
        }

        // Base
        Config.settings.runOnLogin();
        PlayerLimitPerIP.cache();

        // Listeners (More may be registered elsewhere)
        enable(new Event_Status(), Event_Status.class);
        enable(new Event_Vehicle(), Event_Vehicle.class);
        enable(new Event_Chat(), Event_Chat.class);
        enable(new Event_Plugin(), Event_Plugin.class);
        enable(new Event_World(), Event_Plugin.class);
        enable(new Event_Inventory(), Event_Inventory.class);
        enable(new Event_Combat(), Event_Combat.class);
        enable(new Events_Player(), Events_Player.class);
        enable(new Event_Shared(), Event_Shared.class);

        if (NPCManager.supported) {
            enable(new NPCManager(), NPCManager.class);
        }
        if (!MultiVersion.folia) {
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            enable(new EventsHandler_1_9(), EventsHandler_1_9.class);

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                enable(new EventsHandler_1_13(), EventsHandler_1_13.class);
            }
        }
        ProxyUtils.register();

        // Commands
        String command = plugin.getName().toLowerCase();
        getCommand(command).setExecutor(new CommandExecution());
        getCommand(command).setTabCompleter(new CommandTab());
    }

    public void onDisable() {
        plugin = this;
        plugin.setNaggable(false);

        // Separator
        ProxyUtils.unregister();
        listeners.clear();
        SpartanBukkit.disable();
    }

    // Utilities

    public static void enable(Listener l, Class<?> c) {
        if (isPluginEnabled() && listeners.add(c)) {
            manager.registerEvents(l, plugin);
        }
    }

    public static void disablePlugin() {
        if (isPluginEnabled()) {
            plugin.setNaggable(false);
            manager.disablePlugin(plugin);
        }
    }

    public static boolean isPluginEnabled() {
        return plugin != null && plugin.isEnabled();
    }

    public static Class<?>[] getListeners() {
        return listeners.toArray(new Class[0]);
    }

}
