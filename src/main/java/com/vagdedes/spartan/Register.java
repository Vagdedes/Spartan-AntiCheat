package com.vagdedes.spartan;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.functionality.command.CommandExecution;
import com.vagdedes.spartan.functionality.command.CommandTab;
import com.vagdedes.spartan.functionality.connection.DiscordMemberCount;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.SuspicionNotifications;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.*;
import com.vagdedes.spartan.listeners.protocol.*;
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
        Config.create();

        // Separator
        if (MultiVersion.other) {
            AwarenessNotifications.forcefullySend(
                    "The server's version or type is not supported. "
                            + "Please contact the plugin's developer if you think this is an error."
            );
            disablePlugin();
            return;
        }

        // Pre Cache
        DiscordMemberCount.get();

        // Base
        Config.settings.runOnLogin();
        PlayerLimitPerIP.cache();
        SuspicionNotifications.run();

        // Listeners
        if (SpartanBukkit.packetsEnabled()) {
            ProtocolManager p = ProtocolLibrary.getProtocolManager();
            p.addPacketListener(new Move());
            p.addPacketListener(new Teleport());
            p.addPacketListener(new Join());
            p.addPacketListener(new EntityAction());
            p.addPacketListener(new Velocity());
        } else {
            enable(new EventHandler_Shared(), EventHandler_Shared.class);
        }
        enable(new EventsHandler1(), EventsHandler1.class);
        enable(new EventsHandler2(), EventsHandler2.class);
        enable(new EventsHandler3(), EventsHandler3.class);
        enable(new EventsHandler4(), EventsHandler4.class);
        enable(new EventsHandler5(), EventsHandler5.class);

        if (!MultiVersion.folia) {
            enable(new EventsHandler_non_folia(), EventsHandler_non_folia.class);
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
        // Always First
        plugin.setNaggable(false);

        // Separator
        ProxyUtils.unregister();
        listeners.clear();
        Cache.disable();
    }

    // Utilities

    public static void enable(Listener l, Class<?> c) {
        if (isPluginEnabled() && listeners.add(c)) {
            manager.registerEvents(l, plugin);
        }
    }

    public static void disablePlugin() {
        if (isPluginEnabled()) {
            manager.disablePlugin(plugin);
        }
    }

    public static boolean isPluginLoaded() {
        return plugin != null;
    }

    public static boolean isPluginEnabled() {
        return isPluginLoaded() && plugin.isEnabled();
    }

    public static Class<?>[] getListeners() {
        return listeners.toArray(new Class[0]);
    }

}
