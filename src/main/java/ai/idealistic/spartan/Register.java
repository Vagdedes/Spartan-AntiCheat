package ai.idealistic.spartan;

import ai.idealistic.spartan.functionality.command.CommandExecution;
import ai.idealistic.spartan.functionality.command.CommandTab;
import ai.idealistic.spartan.functionality.connection.IDs;
import ai.idealistic.spartan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.listeners.NPCManager;
import ai.idealistic.spartan.listeners.bukkit.*;
import ai.idealistic.spartan.listeners.bukkit.standalone.*;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import ai.idealistic.spartan.utils.minecraft.server.ProxyUtils;
import kireiko.dev.millennium.ml.ClientML;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;

public class Register extends JavaPlugin {

    public static Plugin plugin = null;
    public static final String mainPackage = "ai.idealistic.spartan";
    public static final String pluginName = "Spartan";
    public static final String command = "spartan";
    public static final String pluginURL = "https://spartan.top";
    public static final String synCommand = Register.command + " syn";

    private static final Set<Class<?>> listeners = new LinkedHashSet<>(2);

    public static boolean isFreeEdition() {
        return !IDs.builtByBit && !IDs.polymart;
    }

    public void onEnable() {
        plugin = this;
        //new Metrics(this, 25438);

        if (MultiVersion.serverVersion == MultiVersion.MCVersion.OTHER) {
            AwarenessNotifications.forcefullySend(
                    "The server's version or type is not supported. "
                            + "Please contact the plugin's developer if you think this is an error."
            );
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ClientML.run();
        Config.create();

        // Base
        Config.settings.runOnLogin();

        // Listeners (More may be registered elsewhere)
        enable(new JoinEvent());
        enable(new LeaveEvent());
        enable(new ChatEvent());
        enable(new PluginEvent());
        enable(new InventoryEvent());
        enable(new BowEvent());
        enable(new ChunksEvent());
        enable(new VehicleDeathEvent());
        enable(new HealthEvent());
        enable(new CombatEvent());
        enable(new PlaceEvent());
        enable(new MovementEvent());
        enable(new TeleportEvent());
        enable(new DeathEvent());
        enable(new VehicleEvent());
        enable(new VelocityEvent());
        enable(new WorldEvent());

        //enable(new GhostBlockTest());

        if (NPCManager.supported) {
            enable(new NPCManager());
        }
        if (PlayerUtils.elytra) {
            enable(new ElytraEvent());

            if (PlayerUtils.trident) {
                enable(new TridentEvent());
            }
        }
        ProxyUtils.register();

        // Commands
        this.getCommand(command).setExecutor(new CommandExecution());
        this.getCommand(command).setTabCompleter(new CommandTab());
    }

    public void onDisable() {
        plugin = this;

        // Separator
        ProxyUtils.unregister();
        listeners.clear();
        PluginBase.disable();
    }

    // Utilities

    public static void enable(Listener l) {
        if (isPluginEnabled() && listeners.add(l.getClass())) {
            Bukkit.getPluginManager().registerEvents(l, plugin);
        }
    }

    public static boolean isPluginEnabled() {
        return plugin != null && plugin.isEnabled();
    }

}
