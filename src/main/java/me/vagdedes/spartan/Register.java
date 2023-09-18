package me.vagdedes.spartan;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.functionality.protections.PlayerLimitPerIP;
import me.vagdedes.spartan.functionality.protections.ReconnectCooldown;
import me.vagdedes.spartan.handlers.connection.DiscordMemberCount;
import me.vagdedes.spartan.handlers.connection.Piracy;
import me.vagdedes.spartan.handlers.identifiers.simple.VehicleAccess;
import me.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.spartan.interfaces.commands.CommandExecution;
import me.vagdedes.spartan.interfaces.commands.CommandTab;
import me.vagdedes.spartan.interfaces.listeners.*;
import me.vagdedes.spartan.utils.server.NetworkUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;

public class Register extends JavaPlugin {

    public static Plugin plugin = null;
    private static boolean enabled = false;

    public static final PluginManager manager = Bukkit.getPluginManager();
    private static final Set<Class<?>> listeners = new LinkedHashSet<>(40);

    public void onEnable() {
        plugin = this;
        enabled = true;

        /*PluginDescriptionFile description = plugin.getDescription();

        // Separator
        if (ReflectionUtils.classExists("com.google.gson.JsonArray") && ReflectionUtils.classExists("com.google.gson.JsonObject")
                && ReflectionUtils.classExists("com.google.gson.JsonParser") && ReflectionUtils.classExists("com.google.gson.JsonPrimitive")) {
            try {
                Metrics metrics = new Metrics(plugin, ((int) Math.pow(85, 2)) + description.getName().substring(1).length());
            } catch (Exception ex) {
            }
        }*/

        // Separator
        Config.create();

        // Separator
        if (MultiVersion.other) {
            AwarenessNotifications.forcefullySend("The server's version or type is not supported. Please contact the plugin's developer if you think this is an error.");
            disablePlugin();
            return;
        }

        // Pre Cache
        DiscordMemberCount.get();

        // Base
        Config.settings.runOnLogin();
        LagLeniencies.cache();
        PlayerLimitPerIP.cache();
        ReconnectCooldown.loadCooldowns();
        VehicleAccess.run();

        // Listeners
        enable(new EventsHandler1(), EventsHandler1.class);
        enable(new EventsHandler2(), EventsHandler2.class);
        enable(new EventsHandler3(), EventsHandler3.class);
        enable(new EventsHandler4(), EventsHandler4.class);
        enable(new EventsHandler5(), EventsHandler5.class);
        enable(new EventsHandler6(), EventsHandler6.class);
        enable(new EventsHandler7(), EventsHandler7.class);
        enable(new EventsHandler8(), EventsHandler8.class);
        enable(new EventsHandler9(), EventsHandler9.class);
        enable(new EventsHandler10(), EventsHandler10.class);
        enable(new Piracy(), Piracy.class);

        if (!MultiVersion.folia) {
            enable(new EventsHandler_non_folia(), EventsHandler_non_folia.class);
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            enable(new EventsHandler_1_9(), EventsHandler_1_9.class);

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                enable(new EventsHandler_1_13(), EventsHandler_1_13.class);
            }
        }
        if (!MultiVersion.fork().equals("Unknown")) {
            enable(new EventHandler_Incompatible(), EventHandler_Incompatible.class);
        }
        NetworkUtils.register();

        // Commands
        String command = plugin.getName().toLowerCase();
        getCommand(command).setExecutor(new CommandExecution());
        getCommand(command).setTabCompleter(new CommandTab());
    }

    public void onDisable() {
        // Always First
        enabled = false;
        plugin.setNaggable(false);

        // Separator
        NetworkUtils.unregister();
        listeners.clear();
        Cache.disable();
    }

    // Utilities

    public static void enable(Listener l, Class<?> c) {
        if (isPluginEnabled()
                && listeners.add(c)) {
            manager.registerEvents(l, plugin);
        }
    }

    public static void disablePlugin() {
        enabled = false;

        if (plugin != null && plugin.isEnabled()) {
            manager.disablePlugin(plugin);
        }
    }

    public static boolean isPluginEnabled() {
        return isPluginLoaded() && plugin.isEnabled();
    }

    public static boolean isPluginLoaded() {
        return enabled && plugin != null;
    }

    public static Class<?>[] getListeners() {
        return listeners.toArray(new Class[0]);
    }

}
